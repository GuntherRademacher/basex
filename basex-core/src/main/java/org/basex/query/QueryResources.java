package org.basex.query;

import static org.basex.query.QueryError.*;

import java.io.*;
import java.util.*;

import org.basex.build.*;
import org.basex.build.xml.SAXHandler.*;
import org.basex.core.*;
import org.basex.core.cmd.*;
import org.basex.core.users.*;
import org.basex.data.*;
import org.basex.io.*;
import org.basex.query.func.fn.*;
import org.basex.query.util.list.*;
import org.basex.query.util.pkg.*;
import org.basex.query.value.*;
import org.basex.query.value.node.*;
import org.basex.query.value.seq.*;
import org.basex.query.value.type.*;
import org.basex.util.*;
import org.basex.util.list.*;

/**
 * This class provides access to all kinds of resources (databases, documents, database connections,
 * sessions) used by an XQuery expression.
 *
 * @author BaseX Team, BSD License
 * @author Christian Gruen
 */
public final class QueryResources {
  /** Default options. */
  public static final DocOptions DOC_OPTIONS = new DocOptions();
  /** Default options for creating new documents. */
  private static final MainOptions MAIN_OPTIONS = new MainOptions(DOC_OPTIONS);

  /** Database context. */
  private final Context context;

  /** Module loader. */
  private ModuleLoader modules;
  /** Collections: single nodes and sequences. */
  private final ArrayList<Value> colls = new ArrayList<>(1);
  /** Names of collections. */
  private final ArrayList<String> collNames = new ArrayList<>(1);
  /** Indicates if the first database in the context is globally opened. */
  private boolean globalData;

  /** Opened databases (both temporary and persistent ones). */
  private final ArrayList<Data> datas = new ArrayList<>(1);
  /** External resources. */
  private final Map<Class<? extends QueryResource>, QueryResource> external = new HashMap<>();
  /** Function items. */
  private final Map<String, Value> functions = new HashMap<>();
  /** Input references. */
  private final ArrayList<InputStream> inputs = new ArrayList<>(1);

  /**
   * Constructor.
   * @param qc query context
   */
  QueryResources(final QueryContext qc) {
    context = qc.context;
  }

  /**
   * Compiles the resources.
   * @param nodes input node set
   * @return context value
   */
  Value compile(final DBNodes nodes) {
    // add globally opened database
    final Data data = addData(nodes.data());
    synchronized(context.datas) { context.datas.pin(data); }
    globalData = true;

    // create context value
    final boolean all = nodes.all();
    final Value value = DBNodeSeq.get(new IntList(nodes.pres()), data, all, all);

    // add default collection. use initial node set if it contains all documents of the database.
    // otherwise, create new node set
    final Value coll = all ? value : DBNodeSeq.get(data.resources.docs(), data, true, true);
    addCollection(coll, data.meta.name);

    return value;
  }

  /**
   * Finalizes data instances.
   */
  void close() {
    for(final Data data : datas) Close.close(data, context);
    datas.clear();
    // close dynamically loaded JAR files
    if(modules != null) modules.close();
    modules = null;
    // close external resources
    for(final QueryResource c : external.values()) c.close();
    external.clear();
    // close input resources
    for(final InputStream is : inputs) {
      try {
        is.close();
      } catch(final IOException ex) {
        Util.debug(ex);
      }
    }
    inputs.clear();
  }

  /**
   * Returns the globally opened database.
   * @return database or {@code null} if no database is globally opened
   */
  Data globalData() {
    return globalData ? datas.get(0) : null;
  }

  /**
   * Returns or creates an external resource of the specified class.
   * @param <R> resource
   * @param resource external resource
   * @return resource
   */
  @SuppressWarnings("unchecked")
  public synchronized <R extends QueryResource> R index(final Class<? extends R> resource) {
    QueryResource value = external.get(resource);
    if(value == null) {
      try {
        value = resource.getDeclaredConstructor().newInstance();
        external.put(resource, value);
      } catch(final Throwable ex) {
        throw Util.notExpected(ex);
      }
    }
    return (R) value;
  }

  /**
   * Adds an input stream reference.
   * @param input input stream
   */
  public synchronized void add(final InputStream input) {
    inputs.add(input);
  }

  /**
   * Closes and removes an input stream reference.
   * @param input input stream
   * @throws IOException I/O exception
   */
  public synchronized void remove(final InputStream input) throws IOException {
    inputs.remove(input);
    input.close();
  }

  /**
   * Returns inspected functions.
   * @param path path to module
   * @return inspected functions
   */
  public synchronized Value functions(final String path) {
    return functions.get(path);
  }

  /**
   * Adds inspected functions.
   * @param path path to module
   * @param funcs functions
   */
  public synchronized void addFunctions(final String path, final Value funcs) {
    functions.put(path, funcs);
  }

  /**
   * Opens a new database or returns a reference to an already opened database.
   * @param name name of database
   * @param user current user
   * @param updating updating access
   * @param info input info (can be {@code null})
   * @return database instance
   * @throws QueryException query exception
   */
  public synchronized Data database(final String name, final User user, final boolean updating,
      final InputInfo info) throws QueryException {

    final boolean mainmem = context.options.get(MainOptions.MAINMEM);

    // check if a database with the same name has already been opened
    for(final Data data : datas) {
      // default mode: skip main-memory database instances (which may result from fn:doc calls)
      if(data.inMemory() && !mainmem) continue;
      if(IO.equals(data.meta.name, name)) return data;
    }

    // open and register database
    final Perm perm = updating ? Perm.WRITE : Perm.READ;
    if(!user.has(perm, name)) throw BASEX_PERMISSION_X_X.get(info, perm, name);
    try {
      return addData(Open.open(name, context, context.options, true, false));
    } catch(final IOException ex) {
      throw DB_GET2_X.get(info, ex);
    }
  }

  /**
   * Evaluates {@code fn:doc()}: opens an existing database document, or creates a new
   * database and node.
   * @param qi query input
   * @param docOpts options used by fn:doc or fn:collection
   * @param user current user
   * @param info input info (can be {@code null})
   * @return document
   * @throws QueryException query exception
   */
  public synchronized DBNode doc(final QueryInput qi, final DocOptions docOpts, final User user,
      final InputInfo info) throws QueryException {
    final MainOptions options = context.options;
    // favor default database
    if(options.get(MainOptions.WITHDB) && options.get(MainOptions.DEFAULTDB)) {
      final Data data = globalData();
      if(data != null) {
        final int pre = data.resources.doc(qi.original);
        if(pre != -1) {
          docOpts.checkDbAccess(info);
          return new DBNode(data, pre, Data.DOC);
        }
      }
    }

    // access open database or create new one
    final Data data = data(true, qi, docOpts, user, info);
    // ensure that database contains a single document
    final IntList docs = data.resources.docs(qi.dbPath);
    if(docs.size() == 1) return new DBNode(data, docs.get(0), Data.DOC);
    throw (docs.isEmpty() ? BASEX_DBPATH1_X : BASEX_DBPATH2_X).get(info, qi.original);
  }

  /**
   * Evaluates {@code fn:collection()}: opens an existing collection,
   * or creates a new data reference.
   * @param qi query input (set to {@code null} if default collection is requested)
   * @param user current user
   * @param info input info (can be {@code null})
   * @return collection
   * @throws QueryException query exception
   */
  public synchronized Value collection(final QueryInput qi, final User user, final InputInfo info)
      throws QueryException {

    final boolean withdb = context.options.get(MainOptions.WITHDB);

    // return default collection
    if(qi == null) {
      if(!withdb || colls.isEmpty()) throw NODEFCOLL.get(info);
      return colls.get(0);
    }

    final MainOptions options = context.options;
    if(withdb) {
      // favor default database
      if(options.get(MainOptions.DEFAULTDB)) {
        final Data data = globalData();
        if(data != null) {
          final IntList pres = data.resources.docs(qi.original);
          return DBNodeSeq.get(pres, data, true, qi.original.isEmpty());
        }
      }

      // check currently opened collections (required for tests)
      final String path = qi.io.path();
      final int cs = colls.size();
      for(int c = 0; c < cs; c++) {
        if(IO.equals(collNames.get(c), path)) return colls.get(c);
      }
    }

    // access open database or create new one
    final Data data = data(false, qi, DOC_OPTIONS, user, info);
    final IntList docs = data.resources.docs(qi.dbPath);
    return DBNodeSeq.get(docs, data, true, qi.dbPath.isEmpty());
  }

  /**
   * Returns the module loader. Called during parsing.
   * @return module loader
   */
  public ModuleLoader modules() {
    if(modules == null) modules = new ModuleLoader(context);
    return modules;
  }

  /**
   * Removes and closes the specified database. Called during updates.
   * @param name name of database to be removed
   */
  public void remove(final String name) {
    final boolean mainmem = context.options.get(MainOptions.MAINMEM);
    final int ds = datas.size();
    for(int d = globalData ? 1 : 0; d < ds; d++) {
      final Data data = datas.get(d);
      // default mode: skip main-memory database instances (which may result from fn:doc calls)
      if(data.meta.name.equals(name) && !(data.inMemory() || mainmem)) {
        Close.close(data, context);
        datas.remove(d);
        break;
      }
    }
  }

  // TEST APIS ====================================================================================

  /** Textual resources. Required for test APIs. */
  private Map<String, String[]> texts;
  /** Cached stop word files. Required for test APIs. */
  private Map<String, IO> stop;
  /** Cached thesaurus files. Required for test APIs. */
  private Map<String, IO> thes;

  /**
   * Returns the document path of a textual resource and its encoding. Only required for test APIs.
   * @param path resource path
   * @param sc static context
   * @return path and encoding or {@code null}
   */
  public String[] text(final String path, final StaticContext sc) {
    if(texts == null) return null;
    final IO io = sc.resolve(path);
    final String[] resource = texts.get(io.path());
    return resource != null ? resource : texts.get(io.name());
  }

  /**
   * Returns stop words. Called during parsing, and only required for test APIs.
   * @param path resource path
   * @param sc static context
   * @return file reference
   */
  IO stopWords(final String path, final StaticContext sc) {
    return stop != null ? stop.get(path) : sc.resolve(path);
  }

  /**
   * Returns a thesaurus file. Called during parsing, and only required for Test APIs.
   * @param path resource path
   * @param sc static context
   * @return file reference
   */
  IO thesaurus(final String path, final StaticContext sc) {
    return thes != null ? thes.get(path) : sc.resolve(path);
  }

  /**
   * Adds a document with the specified path. Only called from the test APIs.
   * @param name document identifier (can be {@code null})
   * @param path document path
   * @param sc static context (can be {@code null})
   * @throws QueryException query exception
   */
  public void addDoc(final String name, final String path, final StaticContext sc)
      throws QueryException {
    final QueryInput qi = new QueryInput(path, sc);
    final Data data = create(qi, DOC_OPTIONS, context.user(), null, true);
    if(name != null) data.meta.original = name;
  }

  /**
   * Adds a resource with the specified path. Only called from the test APIs.
   * @param uri resource URI
   * @param strings resource strings (path, encoding)
   */
  public void addText(final String uri, final String... strings) {
    if(texts == null) texts = new HashMap<>();
    texts.put(uri, strings);
  }

  /**
   * Adds a collection with the specified paths. Only called from the test APIs.
   * @param name name of collection (can be empty string)
   * @param paths documents paths
   * @param sc static context (can be {@code null})
   * @throws QueryException query exception
   */
  public void addCollection(final String name, final String[] paths, final StaticContext sc)
      throws QueryException {

    final ItemList items = new ItemList(paths.length);
    for(final String path : paths) {
      final QueryInput qi = new QueryInput(path, sc);
      items.add(new DBNode(create(qi, DOC_OPTIONS, context.user(), null, false), 0, Data.DOC));
    }
    addCollection(items.value(NodeType.DOCUMENT_NODE), name);
  }

  /**
   * Attaches full-text maps. Only called from the test APIs.
   * @param sw stop words
   * @param th thesaurus
   */
  public void ftmaps(final HashMap<String, IO> sw, final HashMap<String, IO> th) {
    stop = sw;
    thes = th;
  }

  // PRIVATE METHODS ==============================================================================

  /**
   * Returns an already open database for the specified input or creates a new one.
   * @param single single document
   * @param qi query input
   * @param docOpts options used by fn:doc or fn:collection
   * @param user current user
   * @param info input info (can be {@code null})
   * @return document
   * @throws QueryException query exception
   */
  private Data data(final boolean single, final QueryInput qi, final DocOptions docOpts,
      final User user, final InputInfo info) throws QueryException {

    final boolean withdb = context.options.get(MainOptions.WITHDB);
    final String name = qi.dbName;

    // check opened databases
    for(final Data data : datas) {
      final boolean mainmem = data.inMemory();
      if(withdb || mainmem) {
        // compare input path
        final String original = data.meta.original;
        if(!original.isEmpty() && IO.get(original).eq(qi.io) &&
            docOpts.toString().equals(data.meta.docOpts)) {
          // reset database path: indicates that database includes all files of the original path
          qi.dbPath = "";
          return data;
        }
        // compare database name; favor existing database instances
        if(IO.equals(data.meta.name, name) && (!mainmem || !context.soptions.dbExists(name))) {
          docOpts.checkDbAccess(info);
          return data;
        }
      }
    }

    // try to open existing database
    if(withdb && name != null) {
      if(!user.has(Perm.READ, name)) throw BASEX_PERMISSION_X_X.get(info, Perm.READ, name);
      try {
        final Data data = Open.open(name, context, context.options, false, false);
        if(data != null) {
          docOpts.checkDbAccess(info);
          return addData(data);
        }
      } catch(final IOException ex) {
        throw IOERR_X.get(info, ex);
      }
    }

    // otherwise, create new instance
    final Data data = create(qi, docOpts, user, info, single);
    // reset database path: indicates that all documents were parsed
    qi.dbPath = "";
    return data;
  }

  /**
   * Creates a new database instance.
   * @param input query input
   * @param docOpts options used by fn:doc or fn:collection
   * @param user current user
   * @param info input info (can be {@code null})
   * @param single expect single document
   * @return data reference
   * @throws QueryException query exception
   */
  private Data create(final QueryInput input, final DocOptions docOpts, final User user,
      final InputInfo info, final boolean single) throws QueryException {

    // check user permissions
    if(!user.has(Perm.READ)) throw XQUERY_PERMREQUIRED_X.get(info, Perm.READ);

    // check if input points to a single file
    final IO io = input.io;
    if(!io.exists()) throw WHICHRES_X.get(info, io.path());
    if(single && io instanceof IOFile && io.isDir()) throw RESDIR_X.get(info, io.path());

    // create parsing options with custom values
    final boolean mainmem = !context.options.get(MainOptions.FORCECREATE);
    final MainOptions options;
    if(mainmem) {
      final String catalog = context.options.get(MainOptions.CATALOG);
      if(docOpts == DOC_OPTIONS && catalog.isEmpty()) {
        options = MAIN_OPTIONS;
      } else {
        options = new MainOptions(docOpts);
        options.set(MainOptions.CATALOG, catalog);
      }
    } else {
      docOpts.checkDbAccess(info);
      options = context.options;
    }

    try {
      final DirParser parser = new DirParser(io, options);
      final Data data = CreateDB.create(io.dbName(), parser, context, options, mainmem);
      data.meta.docOpts = docOpts.toString();
      if(!docOpts.get(DocOptions.STABLE)) return data;
      return addData(data);
    } catch(final IOException ex) {
      final Throwable th = ex.getCause();
      throw !(th instanceof ValidationException) ? IOERR_X.get(info, ex) :
        options.get(MainOptions.DTDVALIDATION) ? DTDVALIDATIONERR_X.get(info, ex) :
          XSDVALIDATIONERR_X.get(info, ex);
    }
  }

  /**
   * Adds a data reference.
   * @param data data reference to be added
   * @return argument
   */
  private Data addData(final Data data) {
    datas.add(data);
    return data;
  }

  /**
   * Adds a collection to the global collection list.
   * @param coll documents of collection
   * @param name collection name (can be empty string)
   */
  private void addCollection(final Value coll, final String name) {
    colls.add(coll);
    collNames.add(name);
  }
}

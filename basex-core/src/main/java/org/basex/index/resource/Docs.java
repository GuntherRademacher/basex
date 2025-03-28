package org.basex.index.resource;

import static org.basex.data.DataText.*;
import static org.basex.util.Token.*;

import java.io.*;

import org.basex.data.*;
import org.basex.io.in.DataInput;
import org.basex.io.out.DataOutput;
import org.basex.util.*;
import org.basex.util.hash.*;
import org.basex.util.list.*;

/**
 * <p>This data structure contains references to all document nodes in a database.
 * The document nodes are incrementally updated.</p>
 *
 * <p>If updates are performed, the path order is discarded, as the update would be more expensive
 * in some cases (e.g. when bulk insertions of new documents are performed). A tree structure could
 * be introduced to offer better general performance.</p>
 *
 * @author BaseX Team, BSD License
 * @author Christian Gruen
 * @author Lukas Kircher
 */
final class Docs {
  /** Data reference. */
  private final Data data;
  /** Pre values of document nodes (can be {@code null}).
   * This variable should always be requested via {@link #docs()}. */
  private IntList docList;
  /** Document paths (can be {@code null}).
   * This variable should always be requested via {@link #paths()}. */
  private TokenList pathList;
  /** Mapping for path order (can be {@code null}).
   * This variable should always be requested via {@link #order()}. */
  private int[] pathOrder;
  /** Dirty flag. */
  private boolean dirty;
  /** Indicates if a path index is available. */
  private boolean pathIndex;

  /**
   * Constructor.
   * @param data data reference
   */
  Docs(final Data data) {
    this.data = data;
  }

  /**
   * Reads the document index.
   * @param in input stream
   * @throws IOException I/O exception
   */
  synchronized void read(final DataInput in) throws IOException {
    docList = in.readDiffs();
    pathIndex = data.meta.dbFile(DATAPTH).exists();
  }

  /**
   * Writes the document index.
   * @param out output stream
   * @throws IOException I/O exception
   */
  void write(final DataOutput out) throws IOException {
    out.writeDiffs(docs());
    if(dirty && pathIndex) {
      // retrieve paths (must be called before file is opened for writing!)
      final TokenList paths = paths();
      // write paths
      try(DataOutput doc = new DataOutput(data.meta.dbFile(DATAPTH))) {
        doc.writeNum(paths.size());
        for(final byte[] path : paths) doc.writeToken(path);
      }
      dirty = false;
    }
  }

  /**
   * Returns a list with the {@code pre} values of all document nodes.
   * @return PRE values
   */
  synchronized IntList docs() {
    if(docList == null) {
      final IntList pres = new IntList();
      final int is = data.meta.size;
      for(int pre = 0; pre < is;) {
        final int k = data.kind(pre);
        if(k == Data.DOC) pres.add(pre);
        pre += data.size(pre, k);
      }
      update();
      docList = pres;
    }
    return docList;
  }

  /**
   * Returns a list with the document paths.
   * @return document paths
   */
  private synchronized TokenList paths() {
    if(pathList == null && pathIndex) {
      // try to read paths from disk
      try(DataInput in = new DataInput(data.meta.dbFile(DATAPTH))) {
        pathList = new TokenList(in.readTokens());
      } catch(final IOException ex) {
        Util.debug(ex);
      }
    }

    // generate paths
    if(pathList == null) {
      // paths have not been stored to disk yet; scan table
      final IntList docs = docs();
      final int ds = docs.size();
      final TokenList paths = new TokenList(ds);
      for(int d = 0; d < ds; d++) {
        paths.add(normalize(data.text(docs.get(d), true)));
      }
      pathIndex = true;
      pathList = paths;
      update();
    }
    return pathList;
  }

  /**
   * Returns an array with offsets to the sorted document paths.
   * @return path order
   */
  private synchronized int[] order() {
    if(pathOrder == null) pathOrder = Array.createOrder(paths().toArray(), false, true);
    return pathOrder;
  }

  /**
   * Adds entries to the index and updates subsequent nodes.
   * @param pre insertion position
   * @param clip data clip
   */
  void insert(final int pre, final DataClip clip) {
    // find all document nodes in the given data instance
    final IntList il = new IntList();
    final Data src = clip.data;
    for(int dpre = clip.start; dpre < clip.end;) {
      final int k = src.kind(dpre);
      if(k == Data.DOC) il.add(pre + dpre);
      dpre += src.size(dpre, k);
    }
    final int[] pres = il.finish();
    final int ps = pres.length;

    // find insertion offset
    final IntList docs = docs();
    int i = docs.sortedIndexOf(pre);
    if(i < 0) i = -i - 1;

    // insert paths from given data instance
    if(pathIndex) {
      final TokenList paths = paths();
      final byte[][] tmp = new byte[ps][];
      for(int t = 0; t < ps; t++) tmp[t] = normalize(clip.data.text(pres[t] - pre, true));
      paths.insert(i, tmp);
    }

    // insert PRE values
    docs.insert(i, pres);
    // adjust PRE values of following document nodes
    docs.incFrom(clip.size(), i + ps);

    update();
  }

  /**
   * Deletes the specified entry and updates subsequent nodes.
   * @param pre PRE value
   * @param size number of deleted nodes
   */
  void delete(final int pre, final int size) {
    // find insertion offset
    final IntList docs = docs();
    final int doc = docs.sortedIndexOf(pre);

    // PRE value points to a document node...
    if(doc >= 0) {
      if(pathIndex) paths().remove(doc);
      docs.remove(doc);
    }

    // adjust PRE values of following document nodes
    docs.incFrom(-size, doc < 0 ? -doc - 1 : doc);
    update();
  }

  /**
   * Updates the index after a document has been renamed.
   * @param pre PRE value of updated document
   * @param value new name
   */
  void rename(final int pre, final byte[] value) {
    if(pathIndex) paths().set(docs().sortedIndexOf(pre), normalize(value));
    update();
  }

  /**
   * Notifies the meta structures of an update and invalidates the indexes.
   */
  private synchronized void update() {
    pathOrder = null;
    data.meta.dirty = true;
    dirty = true;
  }

  /**
   * Returns the PRE values of all document nodes matching the specified path.
   * @param path input path
   * @param dir directory view
   * @return PRE values (can be internal representation!)
   */
  synchronized IntList docs(final String path, final boolean dir) {
    // invalid path, or no documents: return empty list
    final String pth = MetaData.normPath(path);
    if(pth == null) return new IntList(0);

    // empty path: return all documents
    final IntList docs = docs();
    if(!dir && pth.isEmpty()) return docs;

    // normalize paths, check for explicit directory indicator
    byte[] exact = EMPTY, prefix = normalize(token(pth));
    if(!(pth.isEmpty() || Strings.endsWith(pth, '/'))) {
      exact = prefix;
      prefix = concat(exact, cpToken('/'));
    }

    // relevant paths: exact hits and prefixes
    final TokenSet set = new TokenSet();
    final IntList il = new IntList();
    final TokenList paths = paths();
    final int ps = paths.size();
    for(int p = 0; p < ps; p++) {
      final byte[] pt = paths.get(p);
      boolean add = eq(pt, exact);
      if(!add) {
        add = startsWith(pt, prefix);
        if(add && dir) {
          final int i = indexOf(pt, cpToken('/'), prefix.length + 1);
          if(i != -1) add = set.add(substring(pt, prefix.length, i));
        }
      }
      if(add) il.add(docs.get(p));
    }
    return il.sort();
  }

  /**
   * Returns the PRE value of a document node that matches the specified path.
   * @param path input path
   * @return PRE value, or {@code -1} if the document does not exist or if the path is invalid
   */
  synchronized int doc(final String path) {
    final String pth = MetaData.normPath(path);
    if(pth != null && !pth.isEmpty()) {
      final byte[] npth = normalize(token(pth));
      final TokenList paths = paths();
      final int[] order = order();
      int l = 0, h = order.length - 1;
      while(l <= h) {
        final int m = l + h >>> 1, o = order[m], c = compare(paths.get(o), npth);
        if(c == 0) return docs().get(o);
        if(c < 0) l = m + 1;
        else h = m - 1;
      }
    }
    return -1;
  }

  /**
   * Determines whether the given path is the path to a document directory.
   * @param path given path (will be normalized by adding a trailing slash)
   * @return path to a directory or not
   */
  synchronized boolean isDir(final String path) {
    final byte[] prefix = concat(path, cpToken('/'));
    for(final byte[] pth : paths()) {
      if(startsWith(pth, prefix)) return true;
    }
    return false;
  }

  /**
   * Adds the database paths for the child documents of the given path to the given map.
   * @param path path
   * @param dir returns directories instead of files
   * @param map map with resource types
   */
  synchronized void children(final String path, final boolean dir,
      final TokenObjectMap<ResourceType> map) {

    final String pth = MetaData.normPath(path);
    if(pth == null) return;

    // normalize root path
    byte[] root = token(pth);
    if(root.length != 0) root = concat(root, cpToken('/'));

    final IntList docs = docs();
    final int ds = docs.size();
    for(int d = 0; d < ds; d++) {
      byte[] np = data.text(docs.get(d), true);
      if(startsWith(np, root)) {
        np = substring(np, root.length, np.length);
        final int i = indexOf(np, cpToken('/'));
        // no more slashes means this must be a leaf
        if(!dir && i == -1) map.put(np, ResourceType.XML);
        else if(dir && i >= 0) map.put(substring(np, 0, i), ResourceType.XML);
      }
    }
  }

  /**
   * Returns the normalized index path representation for the specified path.
   * The returned path begins with a slash and uses lower case on non-Unix machines.
   * @param path input path (without leading slash)
   * @return canonical path
   */
  private static byte[] normalize(final byte[] path) {
    return concat(cpToken('/'), Prop.CASE ? path : lc(path));
  }

  @Override
  public String toString() {
    final Table table = new Table();
    table.header.add(TABLEPRE);
    table.header.add(TABLECON);

    final TokenList tl = new TokenList();
    final int ds = paths().size();
    for(int d = 0; d < ds; d++) {
      final int doc = docList != null ? docList.get(d) : 0;
      final byte[] path = pathList != null ? pathList.get(d) : EMPTY;
      table.contents.add(tl.add(doc).add(path));
    }
    return table.toString();
  }
}

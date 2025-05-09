package org.basex.core;

import static org.basex.core.Text.*;

import java.io.*;
import java.util.*;

import org.basex.core.cmd.*;
import org.basex.core.jobs.*;
import org.basex.core.locks.*;
import org.basex.core.parse.*;
import org.basex.core.users.*;
import org.basex.data.*;
import org.basex.io.out.*;
import org.basex.query.value.*;
import org.basex.util.*;
import org.xml.sax.*;

/**
 * This class provides the architecture for all internal command
 * implementations. It evaluates queries that are sent by the GUI, the client or
 * the standalone version.
 *
 * @author BaseX Team, BSD License
 * @author Christian Gruen
 */
public abstract class Command extends Job {
  /** Command arguments. */
  public final String[] args;
  /** Indicates if the command requires an opened database. */
  public final boolean openDB;

  /** Exception during command execution. */
  public Exception exception;
  /** Cached result of a command execution. */
  public Value result;

  /** Database context. */
  protected Context context;
  /** Convenience access to database options. */
  protected MainOptions options;
  /** Convenience access to static options. */
  protected StaticOptions soptions;

  /** Output stream. */
  protected PrintOutput out;
  /** Optional input source. */
  protected InputSource in;
  /** Base URI. */
  protected String uri = "";

  /** Info on command execution. */
  private final TokenBuilder info = new TokenBuilder();
  /** Permission required to execute this command. */
  private final Perm perm;

  /**
   * Constructor for commands requiring no opened database.
   * @param perm required permission
   * @param args arguments
   */
  protected Command(final Perm perm, final String... args) {
    this(perm, false, args);
  }

  /**
   * Constructor.
   * @param perm required permission
   * @param openDB requires an opened database
   * @param args arguments
   */
  protected Command(final Perm perm, final boolean openDB, final String... args) {
    this.perm = perm;
    this.openDB = openDB;
    this.args = args;
  }

  /**
   * Executes the command and prints the result to the specified output
   * stream. If an exception occurs, a {@link BaseXException} is thrown.
   * @param ctx database context
   * @param os output stream reference
   * @throws BaseXException command exception
   */
  public final void execute(final Context ctx, final OutputStream os) throws BaseXException {
    // checks if the command performs updates
    updating = updating(ctx);

    // register job
    register(ctx);
    try {
      // run command and return success flag
      if(!run(ctx, os)) {
        final BaseXException ex = new BaseXException(info());
        ex.initCause(exception);
        throw ex;
      }
    } catch(final RuntimeException th) {
      Util.stack(th);
      throw th;
    } finally {
      // ensure that job will be unregistered
      unregister(ctx);
    }
  }

  /**
   * Executes the command and returns the result as string.
   * If an exception occurs, a {@link BaseXException} is thrown.
   * @param ctx database context
   * @return string result
   * @throws BaseXException command exception
   */
  public final String execute(final Context ctx) throws BaseXException {
    final ArrayOutput ao = new ArrayOutput();
    execute(ctx, ao);
    return ao.toString();
  }

  /**
   * Attaches an input stream.
   * @param input input stream
   */
  public final void setInput(final InputStream input) {
    in = new InputSource(input);
  }

  /**
   * Attaches an input source.
   * @param input input source
   */
  public final void setInput(final InputSource input) {
    in = input;
  }

  /**
   * Runs the command without permission, data and concurrency checks.
   * Should only be called by other database commands.
   * @param ctx database context
   * @return result of check
   */
  public final boolean run(final Context ctx) {
    return run(ctx, new NullOutput());
  }

  /**
   * Returns command information.
   * @return info string
   */
  public final String info() {
    return info.toString();
  }

  /**
   * Checks if the command performs updates/write operations.
   * @param ctx database context
   * @return result of check
   */
  @SuppressWarnings("unused")
  public boolean updating(final Context ctx) {
    return perm == Perm.CREATE || perm == Perm.WRITE;
  }

  /**
   * Checks if the command has updated any data.
   * @param ctx database context
   * @return result of check
   */
  public boolean updated(final Context ctx) {
    return updating(ctx);
  }

  /**
   * Closes an open data reference and returns {@code true} if this command will change the
   * {@link Context#data()} reference. This method is only required by the GUI.
   * @param ctx database context
   * @return result of check
   */
  @SuppressWarnings("unused")
  public boolean newData(final Context ctx) {
    return false;
  }

  /**
   * Returns true if this command returns a progress value.
   * This method is only required by the GUI.
   * @return result of check
   */
  public boolean supportsProg() {
    return false;
  }

  /**
   * Returns true if this command can be stopped.
   * This method is only required by the GUI.
   * @return result of check
   */
  public boolean stoppable() {
    return false;
  }

  /**
   * Initializes the command execution.
   * @param ctx database context
   * @param os output stream
   */
  public final void init(final Context ctx, final OutputStream os) {
    context = ctx;
    options = ctx.options;
    soptions = ctx.soptions;
    out = PrintOutput.get(os);
  }

  /**
   * Runs the command without permission, data and concurrency checks.
   * @param ctx database context
   * @param os output stream
   * @return result of check
   */
  public final boolean run(final Context ctx, final OutputStream os) {
    // check if database is opened
    final Data data = ctx.data();
    if(data == null && openDB) return error(NO_DB_OPENED);

    // check permissions
    if(!ctx.user().has(perm, data != null && !data.inMemory() ? data.meta.name : null))
      return error(PERM_REQUIRED_X, perm);

    init(ctx, os);
    try {
      // check if job was stopped before it was started
      checkStop();
      return run();
    } catch(final JobException ex) {
      // job was interrupted by the user or server
      return error(ex.getMessage());
    } catch(final OutOfMemoryError ex) {
      // out of memory
      Performance.gc(2);
      Util.debug(ex);
      return error(OUT_OF_MEM + (perm == Perm.CREATE ? H_OUT_OF_MEM : ""));
    } catch(final Throwable ex) {
      // any other unexpected error
      return error(Util.bug(ex) + Prop.NL + info);
    } finally {
      // flushes the output
      try {
        if(out != null) out.flush();
      } catch(final IOException ex) {
        Util.debug(ex);
      }
    }
  }

  /**
   * Returns a string representation of the command.
   * @param password password flag
   * @return result of check
   */
  public final String toString(final boolean password) {
    final CmdBuilder cb = new CmdBuilder(this, password);
    build(cb);
    return cb.toString();
  }

  /**
   * Sets a base URI.
   * @param base base URI
   * @return self reference
   */
  public Command baseURI(final String base) {
    uri = base;
    return this;
  }

  /**
   * Returns the base URI.
   * @return base URI
   */
  public String baseURI() {
    return uri;
  }

  @Override
  public final String toString() {
    return toString(false);
  }

  // PROTECTED METHODS ============================================================================

  /**
   * Executes the command and serializes the result (internal call).
   * @return success of operation
   * @throws IOException I/O exception
   */
  protected abstract boolean run() throws IOException;

  /**
   * Builds a string representation from the command. This string must be
   * correctly built, as commands are sent to the server as strings.
   * @param cb command builder
   */
  protected void build(final CmdBuilder cb) {
    cb.init().args();
  }

  /**
   * Adds the error message to the message buffer {@link #info}.
   * @param msg error message
   * @param ext error extension
   * @return {@code false}
   */
  protected final boolean error(final String msg, final Object... ext) {
    info.reset();
    info.addExt(msg == null ? "" : msg, ext);
    return false;
  }

  /**
   * Adds information on command execution.
   * @param str information to be added
   * @param ext extended info
   * @return {@code true}
   */
  protected final boolean info(final String str, final Object... ext) {
    if(!str.isEmpty()) info.addExt(str, ext).add(Prop.NL);
    return true;
  }

  /**
   * Returns the specified command option.
   * @param typ options enumeration
   * @param <E> token type
   * @return option
   */
  protected final <E extends Enum<E>> E getOption(final Class<E> typ) {
    final E e = getOption(args[0], typ);
    if(e == null) error(UNKNOWN_TRY_X, args[0]);
    return e;
  }

  /**
   * Adds the name of the database that has been addressed by the argument index.
   * No databases will be added if the argument uses glob syntax.
   * @param list lock list
   * @param index argument index
   */
  protected final void addLocks(final LockList list, final int index) {
    if(index < args.length && args[index] != null) {
      final String db = args[index];
      if(db.isEmpty() || db.matches(".*[?*,].*")) {
        list.addGlobal();
      } else {
        list.add(args[index]);
      }
    }
  }

  /**
   * Returns the specified command option.
   * @param string string to be found
   * @param type options enumeration
   * @param <E> token type
   * @return option, or {@code null} if the option is not found
   */
  protected static <E extends Enum<E>> E getOption(final String string, final Class<E> type) {
    try {
      return Enum.valueOf(type, string.toUpperCase(Locale.ENGLISH));
    } catch(final Exception ex) {
      Util.debug(ex);
      return null;
    }
  }

  /**
   * Closes the specified database if it is currently opened and only pinned once.
   * @param ctx database context
   * @param db database to be closed
   * @return {@code true} if opened database was closed
   */
  protected static boolean close(final Context ctx, final String db) {
    final Data data = ctx.data();
    return data != null && db.equals(data.meta.name) && ctx.datas.pins(db) == 1 && Close.close(ctx);
  }
}

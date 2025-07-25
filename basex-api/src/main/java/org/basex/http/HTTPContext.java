package org.basex.http;

import java.io.*;
import java.util.*;

import jakarta.servlet.*;

import org.basex.*;
import org.basex.core.*;
import org.basex.io.*;
import org.basex.util.*;

/**
 * Global HTTP context information.
 *
 * @author BaseX Team, BSD License
 * @author Christian Gruen
 */
public final class HTTPContext {
  /** Static options. */
  private StaticOptions soptions;
  /** Database context. */
  private Context context;
  /** Initialized failed. */
  private IOException exception;
  /** Server instance. */
  private BaseXServer server;

  /** Singleton instance. */
  private static volatile HTTPContext instance;

  /** Private constructor. */
  private HTTPContext() { }

  /**
   * Returns the singleton instance.
   * @return instance
   */
  public static HTTPContext get() {
    if(instance == null) instance = new HTTPContext();
    return instance;
  }

  /**
   * Returns the database context. Creates a new instance if not done so before.
   * @return database context
   */
  public Context context() {
    return context;
  }

  /**
   * Initializes the HTTP context with static options.
   * @param sopts static options
   */
  public void init(final StaticOptions sopts) {
    soptions = sopts;
  }

  /**
   * Initializes the HTTP context, based on the initial servlet context.
   * Parses all context parameters and passes them on to the database context.
   * @param sc servlet context
   * @throws IOException I/O exception
   */
  public synchronized void init(final ServletContext sc) throws IOException {
    // check if servlet context has already been initialized
    if(context != null) return;

    final String webapp = sc.getRealPath("/");
    // system property (requested in Prop#homePath)
    System.setProperty(Prop.PATH, webapp);
    // global option (will later be assigned to StaticOptions#WEBPATH)
    Prop.put(StaticOptions.WEBPATH, webapp);

    // set all parameters that start with "org.basex." as global options
    for(final String name : Collections.list(sc.getInitParameterNames())) {
      String value = sc.getInitParameter(name);
      if(name.startsWith(Prop.DBPREFIX) && name.endsWith("path") && !new File(value).isAbsolute()) {
        // prefix relative path with absolute servlet path
        Util.debugln(name.toUpperCase(Locale.ENGLISH) + ": " + value);
        value = new IOFile(webapp, value).path();
      }
      Prop.put(name, value);
    }

    // create context
    if(soptions == null) {
      soptions = new StaticOptions(false);
    } else {
      soptions.setSystem();
    }
    context = new Context(soptions);

    // start server instance
    if(!soptions.get(StaticOptions.HTTPLOCAL)) {
      try {
        server = new BaseXServer(context, "-D");
      } catch(final IOException ex) {
        exception = ex;
        throw ex;
      }
    }
    context.initServer();
  }

  /**
   * Returns an exception that was caught during the initialization of the database server.
   * @return exception (can be {@code null})
   */
  public synchronized IOException exception() {
    return exception;
  }

  /**
   * Closes the database context.
   */
  public synchronized void close() {
    if(server != null) {
      try {
        server.stop();
      } catch(final IOException ex) {
        Util.stack(ex);
      }
      server = null;
    }
    if(context != null) {
      context.close();
      context = null;
    }
  }
}

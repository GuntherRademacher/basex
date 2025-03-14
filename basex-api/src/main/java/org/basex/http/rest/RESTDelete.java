package org.basex.http.rest;

import java.io.*;

import org.basex.core.cmd.*;
import org.basex.http.*;
import org.basex.query.*;

/**
 * REST-based evaluation of DELETE operations.
 *
 * @author BaseX Team, BSD License
 * @author Christian Gruen
 */
final class RESTDelete {
  /** Private constructor. */
  private RESTDelete() { }

  /**
   * Creates and returns a REST command.
   * @param session REST session
   * @return command
   * @throws QueryException query exception
   * @throws IOException I/O exception
   */
  static RESTExec get(final RESTSession session) throws QueryException, IOException {
    RESTCmd.assignOptions(session);

    final HTTPConnection conn = session.conn;
    final String db = conn.db();
    if(db.isEmpty()) throw HTTPStatus.NO_DATABASE_SPECIFIED.get();

    // open database to ensure it exists
    final String path = conn.dbpath();
    session.add(path.isEmpty() ? new DropDB(db) : new Delete(path));
    return new RESTExec(session, false);
  }
}

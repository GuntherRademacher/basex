package org.basex.query.func.prof;

import org.basex.query.*;
import org.basex.query.expr.*;
import org.basex.query.func.*;
import org.basex.query.value.*;
import org.basex.util.*;

/**
 * Function implementation.
 *
 * @author BaseX Team, BSD License
 * @author Christian Gruen
 */
public final class ProfType extends StandardFunc {
  @Override
  public Value value(final QueryContext qc) throws QueryException {
    // implementation for dynamic function lookup
    type(qc);
    return arg(0).value(qc);
  }

  @Override
  protected Expr opt(final CompileContext cc) {
    if(cc.dynamic) {
      type(cc.qc);
      return arg(0);
    }
    return this;
  }

  /**
   * Dumps the specified info to standard error or the info view of the GUI.
   * @param qc query context
   */
  private void type(final QueryContext qc) {
    final Expr expr = arg(0);
    qc.trace(Util.info("%, size: %, exprSize: %", expr.seqType(), expr.size(), expr.exprSize()),
        expr + ": ");
  }
}

package org.basex.query.func.util;

import org.basex.query.*;
import org.basex.query.expr.*;
import org.basex.query.func.*;
import org.basex.query.value.*;
import org.basex.query.value.seq.*;

/**
 * Function implementation.
 *
 * @author BaseX Team, BSD License
 * @author Christian Gruen
 */
public final class UtilIf extends StandardFunc {
  @Override
  public Value value(final QueryContext qc) throws QueryException {
    // implementation for dynamic function lookup
    return create().value(qc);
  }

  @Override
  protected Expr opt(final CompileContext cc) throws QueryException {
    return create().optimize(cc);
  }

  /**
   * Creates a new if expression.
   * @return new if expression
   */
  private If create() {
    return new If(info, arg(0), arg(1), defined(2) ? arg(2) : Empty.VALUE);
  }
}

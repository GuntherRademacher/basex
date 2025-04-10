package org.basex.query.func.fn;

import org.basex.query.*;
import org.basex.query.expr.*;
import org.basex.query.func.*;

/**
 * Function based on dynamic context properties (date, etc.).
 *
 * @author BaseX Team, BSD License
 * @author Christian Gruen
 */
public abstract class DynamicFn extends StandardFunc {
  @Override
  protected Expr opt(final CompileContext cc) throws QueryException {
    return cc.dynamic && values(true, cc) ? value(cc.qc) : this;
  }
}

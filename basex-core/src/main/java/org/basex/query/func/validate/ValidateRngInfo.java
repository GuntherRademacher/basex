package org.basex.query.func.validate;

import org.basex.query.*;
import org.basex.query.value.*;

/**
 * Validates a document against a RelaxNG document.
 *
 * @author BaseX Team, BSD License
 * @author Christian Gruen
 */
public final class ValidateRngInfo extends ValidateRng {
  @Override
  public Value value(final QueryContext qc) throws QueryException {
    return info(qc);
  }
}

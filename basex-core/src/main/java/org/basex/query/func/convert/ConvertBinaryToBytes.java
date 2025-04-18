package org.basex.query.func.convert;

import org.basex.query.*;
import org.basex.query.func.*;
import org.basex.query.value.*;
import org.basex.query.value.seq.*;

/**
 * Function implementation.
 *
 * @author BaseX Team, BSD License
 * @author Christian Gruen
 */
public final class ConvertBinaryToBytes extends StandardFunc {
  @Override
  public Value value(final QueryContext qc) throws QueryException {
    return BytSeq.get(toBin(arg(0), qc).binary(info));
  }
}

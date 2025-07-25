package org.basex.query.func.prof;

import org.basex.query.*;
import org.basex.query.func.*;
import org.basex.query.value.item.*;
import org.basex.util.*;

/**
 * Function implementation.
 *
 * @author BaseX Team, BSD License
 * @author Christian Gruen
 */
public final class ProfCurrentMs extends StandardFunc {
  @Override
  public Item item(final QueryContext qc, final InputInfo ii) {
    return Itr.get(System.currentTimeMillis());
  }
}

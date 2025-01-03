package org.basex.query.iter;

import org.basex.query.*;
import org.basex.query.value.node.*;

/**
 * Node iterator interface.
 *
 * @author BaseX Team, BSD License
 * @author Sebastian Gath
 */
public abstract class FTIter extends Iter {
  @Override
  public abstract FTNode next() throws QueryException;
}

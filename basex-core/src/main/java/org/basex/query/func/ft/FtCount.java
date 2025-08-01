package org.basex.query.func.ft;

import org.basex.query.*;
import org.basex.query.iter.*;
import org.basex.query.util.ft.*;
import org.basex.query.value.item.*;
import org.basex.util.*;

/**
 * Function implementation.
 *
 * @author BaseX Team, BSD License
 * @author Christian Gruen
 */
public final class FtCount extends FtAccessFn {
  @Override
  public Item item(final QueryContext qc, final InputInfo ii) throws QueryException {
    final FTPosData tmp = qc.ftPosData;
    qc.ftPosData = new FTPosData();
    final Iter nodes = arg(0).iter(qc);
    for(Item item; (item = qc.next(nodes)) != null;) {
      toDBNode(item, true);
    }
    final int size = qc.ftPosData.size();
    qc.ftPosData = tmp;
    return Itr.get(size);
  }
}

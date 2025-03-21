package org.basex.query.func.sql;

import static org.basex.query.QueryError.*;

import org.basex.query.*;
import org.basex.query.func.*;
import org.basex.query.value.item.*;
import org.basex.query.value.seq.*;
import org.basex.util.*;

/**
 * Functions on relational databases.
 *
 * @author BaseX Team, BSD License
 * @author Rositsa Shadura
 */
public final class SqlInit extends StandardFunc {
  @Override
  public Item item(final QueryContext qc, final InputInfo ii) throws QueryException {
    final String driver = toString(arg(0), qc);
    if(Reflect.find(driver) == null) throw SQL_INIT_X.get(info, driver);
    return Empty.VALUE;
  }
}

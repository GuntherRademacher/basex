package org.basex.query.up.expr;

import static org.basex.query.QueryError.*;
import static org.basex.query.QueryText.*;

import org.basex.core.users.*;
import org.basex.query.*;
import org.basex.query.expr.*;
import org.basex.query.iter.*;
import org.basex.query.up.*;
import org.basex.query.up.primitives.node.*;
import org.basex.query.value.item.*;
import org.basex.query.value.node.*;
import org.basex.query.value.seq.*;
import org.basex.query.var.*;
import org.basex.util.*;
import org.basex.util.hash.*;

/**
 * Delete expression.
 *
 * @author BaseX Team, BSD License
 * @author Lukas Kircher
 */
public final class Delete extends Update {
  /**
   * Constructor.
   * @param info input info (can be {@code null})
   * @param trg target expression
   */
  public Delete(final InputInfo info, final Expr trg) {
    super(info, trg);
  }

  @Override
  public Item item(final QueryContext qc, final InputInfo ii) throws QueryException {
    final Iter iter = arg(0).iter(qc);
    for(Item item; (item = qc.next(iter)) != null;) {
      if(!(item instanceof final ANode node)) throw UPTRGDELEMPT_X.get(info, item);
      // nodes without parents are ignored
      if(node.parent() == null) continue;
      final Updates updates = qc.updates();
      final DBNode dbnode = updates.determineDataRef(node, qc);
      checkPerm(qc, Perm.WRITE, dbnode.data().meta.name);
      updates.add(new DeleteNode(dbnode.pre(), dbnode.data(), info), qc);
    }
    return Empty.VALUE;
  }

  @Override
  public Expr copy(final CompileContext cc, final IntObjectMap<Var> vm) {
    return copyType(new Delete(info, arg(0).copy(cc, vm)));
  }

  @Override
  public boolean equals(final Object obj) {
    return this == obj || obj instanceof Delete && super.equals(obj);
  }

  @Override
  public void toString(final QueryString qs) {
    qs.token(DELETE).token(NODES).token(arg(0));
  }
}

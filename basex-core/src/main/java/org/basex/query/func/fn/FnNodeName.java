package org.basex.query.func.fn;

import static org.basex.query.value.type.NodeType.*;

import org.basex.query.*;
import org.basex.query.expr.*;
import org.basex.query.value.item.*;
import org.basex.query.value.node.*;
import org.basex.query.value.seq.*;
import org.basex.query.value.type.*;
import org.basex.util.*;

/**
 * Function implementation.
 *
 * @author BaseX Team, BSD License
 * @author Christian Gruen
 */
public class FnNodeName extends ContextFn {
  @Override
  public Item item(final QueryContext qc, final InputInfo ii) throws QueryException {
    final ANode node = toNodeOrNull(context(qc), qc);
    if(node == null) return Empty.VALUE;

    final Type type = node.type;
    return empty(type) || type == NAMESPACE_NODE && node.name().length == 0 ? Empty.VALUE :
      node.qname();
  }

  @Override
  protected Expr opt(final CompileContext cc) {
    final Expr expr = optFirst(false, false, cc.qc.focus.value);
    return expr != this ? expr : empty(cc, true) ? Empty.VALUE : this;
  }

  /**
   * Checks if the result will always be empty.
   * @param cc compilation context
   * @param occ update occurrence indicator
   * @return result of check
   */
  final boolean empty(final CompileContext cc, final boolean occ) {
    final Expr node = contextAccess() ? cc.qc.focus.value : arg(0);
    if(node == null) return false;

    final SeqType st = node.seqType();
    final Type type = st.type;
    if(occ && st.oneOrMore() && type.oneOf(ELEMENT, ATTRIBUTE, PROCESSING_INSTRUCTION)) {
      exprType.assign(Occ.EXACTLY_ONE);
    }
    return type instanceof NodeType && type != NODE && empty(type);
  }

  /**
   * Checks if a node with the specified type returns no result.
   * @param type type
   * @return result of check
   */
  static boolean empty(final Type type) {
    return !type.oneOf(ELEMENT, ATTRIBUTE, PROCESSING_INSTRUCTION, NAMESPACE_NODE);
  }
}

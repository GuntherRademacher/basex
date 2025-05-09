package org.basex.query.expr;

import static org.basex.query.QueryText.*;

import org.basex.query.*;
import org.basex.query.util.*;
import org.basex.query.value.item.*;
import org.basex.util.*;

/**
 * Abstract pragma expression.
 *
 * @author BaseX Team, BSD License
 * @author Leo Woerteler
 */
public abstract class Pragma extends ExprInfo {
  /** QName. */
  final QNm name;
  /** Pragma value. */
  final byte[] value;

  /**
   * Constructor.
   * @param name name of pragma
   * @param value optional value
   */
  Pragma(final QNm name, final byte[] value) {
    this.name = name;
    this.value = value;
  }

  /**
   * Initializes the pragma expression.
   * @param qc query context
   * @param info input info (can be {@code null})
   * @return state before pragmas was set
   * @throws QueryException query exception
   */
  abstract Object init(QueryContext qc, InputInfo info) throws QueryException;

  /**
   * Finalizes the pragma expression.
   * @param qc query context
   * @param state state before pragma was set
   */
  abstract void finish(QueryContext qc, Object state);

  /**
   * Traverses this expression, notifying the visitor of declared and used variables,
   * and checking the tree for other recursive properties.
   * @param visitor visitor
   */
  abstract void accept(ASTVisitor visitor);

  /**
   * Indicates if an expression has one of the specified compiler properties.
   * @param flags flag to be checked
   * @return result of check
   * @see Expr#has(Flag...)
   */
  public abstract boolean has(Flag... flags);

  /**
   * Creates a copy of this pragma.
   * @return copy
   */
  public abstract Pragma copy();

  /**
   * Indicates if the pragma can be simplified.
   * @return result of check
   */
  public abstract boolean simplify();

  /**
   * {@inheritDoc}
   * Must be overwritten by implementing class.
   */
  @Override
  public boolean equals(final Object obj) {
    return obj instanceof final Pragma prgm && name.eq(prgm.name) && Token.eq(value, prgm.value);
  }

  @Override
  public final void toXml(final QueryPlan plan) {
    plan.add(plan.create(this, VALUEE, value), name);
  }

  @Override
  public final void toString(final QueryString qs) {
    qs.token("(#").token(name).token(value).token("#)");
  }
}

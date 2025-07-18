package org.basex.query;

import java.util.*;
import java.util.function.*;

import org.basex.core.*;
import org.basex.query.expr.*;
import org.basex.query.expr.List;
import org.basex.query.expr.constr.*;
import org.basex.query.expr.gflwor.*;
import org.basex.query.expr.path.*;
import org.basex.query.func.*;
import org.basex.query.func.Function;
import org.basex.query.func.fn.*;
import org.basex.query.scope.*;
import org.basex.query.util.*;
import org.basex.query.util.list.*;
import org.basex.query.value.*;
import org.basex.query.value.item.*;
import org.basex.query.value.seq.*;
import org.basex.query.var.*;
import org.basex.util.*;
import org.basex.util.hash.*;

/**
 * Compilation context.
 *
 * @author BaseX Team, BSD License
 * @author Christian Gruen
 */
public final class CompileContext {
  /**
   * Compile-time simplifications.
   * @see Expr#simplifyFor(Simplify, CompileContext)
   */
  public enum Simplify {
    /**
     * EBV checks.
     * Requested by {@link If}, {@link Logical}, {@link Preds}, {@link Condition}, {@link Where},
     * {@link FnBoolean}, {@link FnNot}.
     * Evaluated by {@link Expr} {@link Filter}, {@link List}, {@link SimpleMap}, {@link Path}
     * and others.
     */
    EBV,
    /**
     * Atomizations.
     * Requested by {@link FnData}, {@link FnDistinctValues}, {@link GroupSpec}, {@link OrderKey},
     * {@link Lookup}, {@link TypeCheck}.
     * Evaluated by {@link Cast}, {@link TypeCheck}, {@link CNode}, {@link FnData}.
     */
    DATA,
    /**
     * String arguments.
     * Requested by {@link Cast}, {@link CmpG}, {@link StandardFunc} and others.
     * Evaluated by {@link Cast}, {@link TypeCheck}, {@link CNode}, {@link FnData},
     * {@link FnString}, {@link FnReplicate}.
     */
    STRING,
    /**
     * Numeric arguments.
     * Requested by {@link Arith}, {@link CmpIR}, {@link Range}, {@link StandardFunc} and others.
     * Evaluated by {@link Cast}, {@link TypeCheck}, {@link CNode}, {@link FnData},
     * {@link FnNumber}, {@link FnReplicate}.
     */
    NUMBER,
    /**
     * Predicate checks.
     * Requested by {@link Preds}.
     * Evaluated by {@link Expr}, {@link FnData}, {@link Cast}, {@link TypeCheck},
     * {@link SimpleMap}, {@link FnNumber}, {@link FnReplicate}.
     */
    PREDICATE,
    /**
     * Distinct values.
     * Requested by {@link CmpG} and {@link FnDistinctValues}.
     * Evaluated by {@link Filter}, {@link List}, {@link SimpleMap} and others.
     */
    DISTINCT,
    /**
     * Count and existence checks.
     * Requested by {@link FnCount}, {@link FnEmpty} and {@link FnExists}.
     * Evaluated by {@link Filter}, {@link GFLWOR}, {@link FnReverse} and others.
     */
    COUNT;

    /**
     * Checks if this is one of the specified candidates.
     * @param candidates candidates
     * @return result of check
     */
    public boolean oneOf(final Simplify... candidates) {
      return Enums.oneOf(this, candidates);
    }
  }

  /** Limit for the size of sequences that are pre-evaluated. */
  private static final int MAX_PREEVAL = 1 << 18;

  /** Query context. */
  public final QueryContext qc;
  /** Dynamic compilation. */
  public final boolean dynamic;
  /** Currently inlined function expressions. */
  public final ArrayDeque<XQFunctionExpr> inlined = new ArrayDeque<>();

  /** Variable scope list. */
  private final ArrayDeque<VarScope> scopes = new ArrayDeque<>();
  /** Query focus list. */
  private final ArrayDeque<QueryFocus> focuses = new ArrayDeque<>();

  /**
   * Constructor.
   * @param qc query context
   * @param dynamic dynamic compilation
   */
  public CompileContext(final QueryContext qc, final boolean dynamic) {
    this.qc = qc;
    this.dynamic = dynamic;
  }

  /**
   * Adds some compilation info.
   * @param string evaluation info
   * @param ext text text extensions
   */
  public void info(final String string, final Object... ext) {
    if(qc.parent == null) qc.info.compInfo(dynamic, string, ext);
  }

  /**
   * Pushes a new variable scope to the stack.
   * @param vs variable scope
   */
  public void pushScope(final VarScope vs) {
    scopes.add(vs);
  }

  /**
   * Removes and returns the current variable scope.
   * @return the removed element
   */
  public VarScope removeScope() {
    return scopes.removeLast();
  }

  /**
   * Prepares the variable scope for being compiled.
   * This method should be run after compiling a scope.
   * @param scope scope
   */
  public void removeScope(final Scope scope) {
    removeScope().cleanUp(scope);
  }

  /**
   * Pushes the current query focus onto the stack and, if possible, assigns a new dummy item.
   * @param expr focus expression (can be {@code null})
   * @param item focus single item
   */
  public void pushFocus(final Expr expr, final boolean item) {
    focuses.add(qc.focus);
    final QueryFocus qf = new QueryFocus();
    if(expr != null) qf.value = new Dummy(expr, item);
    qc.focus = qf;
  }

  /**
   * Assigns a new dummy item to the query focus.
   * @param expr focus expression
   * @param item focus single item
   */
  public void updateFocus(final Expr expr, final boolean item) {
    qc.focus.value = new Dummy(expr, item);
  }

  /**
   * Evaluates a function within the focus of the supplied expression.
   * @param expr focus expression (can be {@code null})
   * @param item focus single item
   * @param func function to evaluate
   * @return resulting expression
   * @throws QueryException query exception
   */
  public Expr get(final Expr expr, final boolean item, final QuerySupplier<Expr> func)
      throws QueryException {
    pushFocus(expr, item);
    try {
      return func.get();
    } finally {
      removeFocus();
    }
  }

  /**
   * Evaluates a function within the focus of the supplied expression.
   * @param expr focus expression (can be {@code null})
   * @param item focus single item
   * @param func function to evaluate
   * @return result of check
   * @throws QueryException query exception
   */
  public boolean ok(final Expr expr, final boolean item, final QuerySupplier<Boolean> func)
      throws QueryException {
    pushFocus(expr, item);
    try {
      return func.get();
    } finally {
      removeFocus();
    }
  }

  /**
   * Removes the current query focus from the stack.
   */
  public void removeFocus() {
    qc.focus = focuses.pollLast();
  }

  /**
   * Indicates if the query focus is nested.
   * @return result of check
   */
  public boolean nestedFocus() {
    return !focuses.isEmpty();
  }

  /**
   * Returns the current variable scope.
   * @return variable scope
   */
  public VarScope vs() {
    return scopes.getLast();
  }

  /**
   * Creates a new copy of the given variable in this scope.
   * @param var variable to copy (can be {@code null})
   * @param vm variable mapping (can be {@code null})
   * @return new variable, or {@code null} if the supplied variable is {@code null}
   */
  public Var copy(final Var var, final IntObjectMap<Var> vm) {
    if(var == null) return null;
    final VarScope vs = vs();
    final Var vr = vs.add(new Var(var, qc));
    if(vm != null) vm.put(var.id, vr);
    return vr;
  }

  /**
   * Pre-evaluates the specified expression.
   * @param expr expression
   * @return optimized expression
   * @throws QueryException query exception
   */
  public Value preEval(final Expr expr) throws QueryException {
    return (Value) replaceWith(expr, expr.value(qc));
  }

  /**
   * Replaces the specified expression with an empty sequence.
   * @param expr original expression
   * @return optimized expression
   */
  public Expr emptySeq(final Expr expr) {
    return replaceWith(expr, Empty.VALUE, false);
  }

  /**
   * Replaces an expression with a simplified one and simplifies the result expression.
   * As the simplified expression may have a different type, no type refinement is performed.
   * @param expr original expression
   * @param result resulting expression
   * @param mode mode of simplification
   * @return simplified or original expression
   * @throws QueryException query exception
   */
  public Expr simplify(final Expr expr, final Expr result, final Simplify mode)
      throws QueryException {
    return result != expr ? replaceWith(expr, result, false).simplifyFor(mode, this) :
      result.simplify(mode, this);
  }

  /**
   * Replaces an expression with the specified one.
   * @param expr original expression
   * @param result resulting expression
   * @return optimized expression
   */
  public Expr replaceWith(final Expr expr, final Expr result) {
    return replaceWith(expr, result, true);
  }

  /**
   * Replaces an expression with the specified one.
   * @param expr original expression
   * @param result resulting expression
   * @param refine refine type
   * @return optimized expression
   */
  private Expr replaceWith(final Expr expr, final Expr result, final boolean refine) {
    if(result != expr) {
      info("%", (Supplier<String>) () -> {
        final String exprDesc = expr.description(), resDesc = result.description();
        final byte[] exprString = QueryError.normalize(expr, null);
        final byte[] resString = QueryError.normalize(result, null);
        final boolean eqDesc = exprDesc.equals(resDesc), eqString = Token.eq(exprString, resString);

        final TokenBuilder tb = new TokenBuilder();
        tb.add(QueryText.OPTREWRITE).add(' ').add(exprDesc);
        if(eqString && !eqDesc) tb.add(" to ").add(resDesc);
        tb.add(": ").add(exprString);
        if(!eqString) tb.add(" -> ").add(resString);
        return tb.toString();
      });
      if(refine) result.refineType(expr);
    }
    return result;
  }

  /**
   * Compiles an expression or creates an error.
   * @param expr expression
   * @param error return error instead of {@code fn:error} instance
   * @return compiled expression or error.
   * @throws QueryException query exception
   */
  public Expr compileOrError(final Expr expr, final boolean error) throws QueryException {
    try {
      return expr.compile(this);
    } catch(final QueryException ex) {
      // replace original expression with error
      if(error) throw ex;
      return FnError.get(ex, expr);
    }
  }

  /**
   * Creates and returns an optimized instance of the specified function.
   * @param function function
   * @param info input info (can be {@code null})
   * @param exprs expressions
   * @return function
   * @throws QueryException query exception
   */
  public Expr function(final AFunction function, final InputInfo info, final Expr... exprs)
      throws QueryException {
    return function.get(info, exprs).optimize(this);
  }

  /**
   * Creates a list with an expression to be absorbed and the actual result expression.
   * @param expr expression to be absorbed
   * @param result result expression
   * @param info input info (can be {@code null})
   * @return function
   * @throws QueryException query exception
   */
  public Expr voidAndReturn(final Expr expr, final Expr result, final InputInfo info)
      throws QueryException {
    // an expression (for example, fn:doc) may be nondeterministic during compilation and
    // deterministic during optimization:
    // let $db := db:get('db') return 1  ->  void(db:get('db'), true()), 1  ->  1
    return List.get(this, info, function(Function.VOID, info, expr, Bln.TRUE), result);
  }

  /**
   * Replicates an expression.
   * @param expr expression
   * @param count count expression
   * @param info input info (can be {@code null})
   * @return function
   * @throws QueryException query exception
   */
  public Expr replicate(final Expr expr, final Expr count, final InputInfo info)
      throws QueryException {
    final ExprList args = new ExprList().add(expr).add(count);
    if(expr.has(Flag.NDT, Flag.CNS)) args.add(Bln.TRUE);
    return function(Function.REPLICATE, info, args.finish());
  }

  /**
   * Returns an unrolled expression list.
   * @param expr expression to analyze
   * @param single unroll expressions and medium-size lists that yield single items
   * @return expression list or {@code null}
   */
  public ExprList unroll(final Expr expr, final boolean single) {
    final Consumer<ExprList> add;
    final long size = expr.size(), limit = qc.context.options.get(MainOptions.UNROLLLIMIT);
    if(single && size == 1) {
      // expression yielding single item: <a/>
      add = exprs -> exprs.add(expr);
    } else if(expr instanceof final Seq seq && size <= limit) {
      // sequence: 'a', 'b', 'c'
      add = exprs -> { for(final Item item : seq) exprs.add(item); };
    } else if(expr instanceof List && (single ?
        size <= limit && ((Checks<Expr>) ex -> ex.seqType().one()).all(expr.args()) :
        expr.args().length <= limit)) {
      // list: <a/>, <b/>
      add = exprs -> { for(final Expr ex : expr.args()) exprs.add(ex); };
    } else {
      return null;
    }

    info(QueryText.OPTUNROLL_X, expr);
    final ExprList exprs = new ExprList();
    add.accept(exprs);
    return exprs;
  }

  /**
   * Checks if all specified expressions are values (possibly of small size).
   * @param limit check if result size of any expression exceeds {@link #MAX_PREEVAL}
   * @param exprs expressions
   * @return result of check
   */
  public boolean values(final boolean limit, final Expr... exprs) {
    for(final Expr expr : exprs) {
      if(!(expr instanceof Value) || limit && largeResult(expr)) return false;
    }
    return true;
  }

  /**
   * Checks if an expression yields a large result.
   * @param expr expression
   * @return result of check
   */
  public boolean largeResult(final Expr expr) {
    return expr.size() > MAX_PREEVAL;
  }
}

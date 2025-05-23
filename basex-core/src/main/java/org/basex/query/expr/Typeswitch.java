package org.basex.query.expr;

import static org.basex.query.QueryText.*;

import java.util.*;

import org.basex.query.*;
import org.basex.query.CompileContext.*;
import org.basex.query.iter.*;
import org.basex.query.util.*;
import org.basex.query.value.*;
import org.basex.query.value.item.*;
import org.basex.query.value.type.*;
import org.basex.query.var.*;
import org.basex.util.*;
import org.basex.util.hash.*;

/**
 * Typeswitch expression.
 *
 * @author BaseX Team, BSD License
 * @author Christian Gruen
 */
public final class Typeswitch extends ParseExpr {
  /** Cases. */
  private TypeswitchGroup[] groups;
  /** Condition. */
  private Expr cond;

  /**
   * Constructor.
   * @param info input info (can be {@code null})
   * @param cond condition
   * @param groups case groups (last one is default)
   */
  public Typeswitch(final InputInfo info, final Expr cond, final TypeswitchGroup[] groups) {
    super(info, SeqType.ITEM_ZM);
    this.cond = cond;
    this.groups = groups;
  }

  @Override
  public void checkUp() throws QueryException {
    checkNoUp(cond);
    final int gl = groups.length;
    final Expr[] tmp = new Expr[gl];
    for(int g = 0; g < gl; ++g) tmp[g] = groups[g].expr;
    checkAllUp(tmp);
  }

  @Override
  public Expr compile(final CompileContext cc) throws QueryException {
    cond = cond.compile(cc);
    for(final TypeswitchGroup group : groups) group.compile(cc);
    return optimize(cc);
  }

  @Override
  public Expr optimize(final CompileContext cc) throws QueryException {
    // pre-evaluate expression
    if(cond instanceof final Value value) {
      for(final TypeswitchGroup group : groups) {
        if(group.match(value, null)) {
          group.inline(value, cc);
          return cc.replaceWith(this, group.expr);
        }
      }
    }

    // remove checks that will never match
    final SeqType ct = cond.seqType();
    final ArrayList<SeqType> types = new ArrayList<>();
    final ArrayList<TypeswitchGroup> newGroups = new ArrayList<>(groups.length);
    for(final TypeswitchGroup group : groups) {
      if(group.removeTypes(ct, types, cc)) newGroups.add(group);
    }
    groups = newGroups.toArray(TypeswitchGroup[]::new);

    // determine common type
    exprType.assign(SeqType.union(groups, true)).data(groups);

    // check if always the same branch will be evaluated
    Expr expr = this;
    TypeswitchGroup tg = null;
    final int gl = groups.length;
    for(int g = 0; g < gl - 1; g++) {
      final ArrayList<SeqType> matching = groups[g].matchingTypes(ct);
      if(!matching.isEmpty()) {
        // make sure that preceding groups do not branch on subtypes
        boolean branch = false;
        for(int h = 0; !branch && h < g; h++) {
          for(final SeqType st : groups[h].seqTypes) {
            if(((Checks<SeqType>) st::instanceOf).any(matching)) {
              branch = true;
              break;
            }
          }
        }
        if(!branch) tg = groups[g];
        break;
      }
    }
    // choose default branch if none of the branches will be chosen
    if(tg == null) {
      boolean opt = true;
      for(int g = 0; opt && g < gl - 1; g++) opt = groups[g].noMatches(ct);
      if(opt) tg = groups[gl - 1];
    }
    // choose first branch if all return expressions are equal
    if(tg == null) {
      boolean opt = true;
      for(int g = 1; opt && g < gl; g++) opt = groups[0].expr.equals(groups[g].expr);
      if(opt) tg = groups[0];
    }

    if(tg != null) {
      // rewrite chosen branch
      expr = tg.rewrite(cond, cc);
    } else if(gl < 3 && groups[0].seqTypes.length == 1 && !cond.has(Flag.NDT)) {
      // otherwise, rewrite to 'if' expression if one or two branches are left
      final Expr iff = new Instance(info, cond, groups[0].seqTypes[0]).optimize(cc);
      final Expr thn = groups[0].rewrite(cond, cc), els = groups[1].rewrite(cond, cc);
      expr = new If(info, iff, thn, els).optimize(cc);
    }
    return cc.replaceWith(this, expr);
  }

  @Override
  public Expr simplifyFor(final Simplify mode, final CompileContext cc) throws QueryException {
    boolean changed = false;
    for(final TypeswitchGroup group : groups) {
      changed |= group.simplifyFor(mode, cc) == null;
    }
    return changed ? optimize(cc) : super.simplifyFor(mode, cc);
  }

  @Override
  public Iter iter(final QueryContext qc) throws QueryException {
    return group(qc).iter(qc);
  }

  @Override
  public Value value(final QueryContext qc) throws QueryException {
    return group(qc).value(qc);
  }

  @Override
  public Item item(final QueryContext qc, final InputInfo ii) throws QueryException {
    return group(qc).item(qc, info);
  }

  /**
   * Returns the typeswitch group with a matching expression.
   * @param qc query context
   * @return group
   * @throws QueryException query exception
   */
  private TypeswitchGroup group(final QueryContext qc) throws QueryException {
    final Value value = cond.value(qc);
    for(final TypeswitchGroup group : groups) {
      if(group.match(value, qc)) return group;
    }
    throw Util.notExpected();
  }

  @Override
  public boolean vacuous() {
    return ((Checks<TypeswitchGroup>) group -> group.expr.vacuous()).all(groups);
  }

  @Override
  public boolean ddo() {
    return ((Checks<TypeswitchGroup>) group -> group.expr.ddo()).all(groups);
  }

  @Override
  public boolean has(final Flag... flags) {
    for(final TypeswitchGroup group : groups) {
      if(group.has(flags)) return true;
    }
    return cond.has(flags);
  }

  @Override
  public boolean inlineable(final InlineContext ic) {
    for(final TypeswitchGroup group : groups) {
      if(!group.inlineable(ic)) return false;
    }
    return cond.inlineable(ic);
  }

  @Override
  public VarUsage count(final Var var) {
    return cond.count(var).plus(VarUsage.maximum(var, groups));
  }

  @Override
  public Expr inline(final InlineContext ic) throws QueryException {
    boolean changed = ic.inline(groups, true);
    final Expr inlined = cond.inline(ic);
    if(inlined != null) {
      changed = true;
      cond = inlined;
    }
    return changed ? optimize(ic.cc) : null;
  }

  @Override
  public Expr typeCheck(final TypeCheck tc, final CompileContext cc) throws QueryException {
    boolean changed = false;
    for(final TypeswitchGroup group : groups) {
      changed = group.typeCheck(tc, cc) != null;
    }
    return changed ? optimize(cc) : this;
  }

  @Override
  public Expr copy(final CompileContext cc, final IntObjectMap<Var> vm) {
    return copyType(new Typeswitch(info, cond.copy(cc, vm), Arr.copyAll(cc, vm, groups)));
  }

  @Override
  public void markTailCalls(final CompileContext cc) {
    for(final TypeswitchGroup group : groups) group.markTailCalls(cc);
  }

  @Override
  public boolean accept(final ASTVisitor visitor) {
    return cond.accept(visitor) && visitAll(visitor, groups);
  }

  @Override
  public int exprSize() {
    int size = 1;
    for(final Expr group : groups) size += group.exprSize();
    return size + cond.exprSize();
  }

  @Override
  public boolean equals(final Object obj) {
    return this == obj || obj instanceof final Typeswitch ts && cond.equals(ts.cond) &&
        Array.equals(groups, ts.groups);
  }

  @Override
  public void toXml(final QueryPlan plan) {
    plan.add(plan.create(this), cond, groups);
  }

  @Override
  public void toString(final QueryString qs) {
    qs.token(TYPESWITCH).paren(cond).tokens(groups);
  }
}

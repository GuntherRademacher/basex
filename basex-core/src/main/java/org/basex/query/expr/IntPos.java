package org.basex.query.expr;

import static java.lang.Long.*;
import static org.basex.query.QueryText.*;

import org.basex.query.*;
import org.basex.query.expr.CmpG.*;
import org.basex.query.func.*;
import org.basex.query.util.*;
import org.basex.query.value.*;
import org.basex.query.value.item.*;
import org.basex.query.value.seq.*;
import org.basex.query.value.type.*;
import org.basex.query.var.*;
import org.basex.util.*;
import org.basex.util.hash.*;

/**
 * Integer position range check.
 *
 * @author BaseX Team, BSD License
 * @author Christian Gruen
 */
public final class IntPos extends Simple implements CmpPos {
  /** Minimum position (1 or larger). */
  final long min;
  /** Maximum position (inclusive, 1 or larger, never smaller than {@link #min}). */
  final long max;

  /**
   * Constructor.
   * @param min minimum value (1 or larger)
   * @param max minimum value (inclusive, 1 or larger)
   * @param info input info (can be {@code null})
   */
  private IntPos(final long min, final long max, final InputInfo info) {
    super(info, SeqType.BOOLEAN_O);
    this.min = min;
    this.max = max;
  }

  /**
   * Returns a position expression for the specified range, or an optimized boolean item.
   * @param min minimum position
   * @param max minimum position (inclusive)
   * @param info input info (can be {@code null})
   * @return expression
   */
  public static Expr get(final long min, final long max, final InputInfo info) {
    // assumption: positions do not exceed bounds of long values
    return min > max || max < 1 ? Bln.FALSE : min <= 1 && max == MAX_VALUE ? Bln.TRUE :
      new IntPos(Math.max(1, min), Math.max(1, max), info);
  }

  @Override
  public Bln item(final QueryContext qc, final InputInfo ii) throws QueryException {
    return Bln.get(test(qc, ii, 0));
  }

  @Override
  public boolean test(final QueryContext qc, final InputInfo ii, final long pos)
      throws QueryException {
    ctxValue(qc);
    final long p = qc.focus.pos;
    return p >= min && p <= max;
  }

  @Override
  public Value positions(final QueryContext qc) {
    return RangeSeq.get(min, max - min + 1, true);
  }

  @Override
  public boolean exact() {
    return min == max;
  }

  @Override
  public Expr inline(final InlineContext ic) {
    return null;
  }

  @Override
  public IntPos copy(final CompileContext cc, final IntObjectMap<Var> vm) {
    return copyType(new IntPos(min, max, info));
  }

  @Override
  public Expr invert(final CompileContext cc) throws QueryException {
    if(exact()) {
      final Expr pos = cc.function(Function.POSITION, info);
      return new CmpG(info, pos, Itr.get(min), OpG.NE).optimize(cc);
    }
    return min == 1 ? get(max + 1, MAX_VALUE, info) :
      max == MAX_VALUE ? get(1, min - 1, info) : null;
  }

  @Override
  public Expr mergeEbv(final Expr ex, final boolean or, final CompileContext cc) {
    if(ex instanceof final IntPos pos) {
      // find range with smaller minimum
      final boolean smaller = min < pos.min;
      final IntPos pos1 = smaller ? this : pos, pos2 = smaller ? pos : this;
      // create intersection: pos: 1, 2 and pos: 2, 3  ->  pos: 2
      if(!or) return get(pos2.min, Math.min(pos1.max, pos2.max), info);
      // create union: pos: 1, 2 or pos: 2, 3  ->  pos: 1, 3
      if(pos1.max + 1 >= pos2.min) return get(pos1.min, Math.max(pos1.max, pos2.max), info);
      // disjoint ranges, no rewrite: pos: 1 or pos: 3
    }
    return null;
  }

  @Override
  public boolean has(final Flag... flags) {
    return Flag.POS.oneOf(flags) || Flag.CTX.oneOf(flags) || super.has(flags);
  }

  @Override
  public boolean equals(final Object obj) {
    return this == obj || obj instanceof final IntPos pos && min == pos.min && max == pos.max;
  }

  @Override
  public String description() {
    return "positional access";
  }

  @Override
  public void toXml(final QueryPlan plan) {
    plan.add(plan.create(this, MIN, min, MAX, max == MAX_VALUE ? INF : max));
  }

  @Override
  public void toString(final QueryString qs) {
    qs.function(Function.POSITION);
    if(exact()) {
      qs.token("=").token(min);
    } else if(max == MAX_VALUE) {
      qs.token(">=").token(min);
    } else if(min == 1) {
      qs.token("<=").token(max);
    } else {
      qs.token("=").token(min).token(TO).token(max);
    }
  }
}

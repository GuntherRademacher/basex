package org.basex.query.func.fn;

import static org.basex.query.func.Function.*;

import java.util.*;

import org.basex.index.stats.*;
import org.basex.query.*;
import org.basex.query.CompileContext.*;
import org.basex.query.expr.*;
import org.basex.query.expr.CmpV.*;
import org.basex.query.expr.path.*;
import org.basex.query.func.*;
import org.basex.query.iter.*;
import org.basex.query.util.collation.*;
import org.basex.query.util.hash.*;
import org.basex.query.util.list.*;
import org.basex.query.value.*;
import org.basex.query.value.item.*;
import org.basex.query.value.seq.*;
import org.basex.query.value.type.*;
import org.basex.util.hash.*;
import org.basex.util.list.*;

/**
 * Function implementation.
 *
 * @author BaseX Team, BSD License
 * @author Christian Gruen
 */
public final class FnDistinctValues extends FnDuplicateValues {
  @Override
  public Iter iter(final QueryContext qc) throws QueryException {
    final Iter values = arg(0).atomIter(qc, info);
    final Collation collation = toCollation(arg(1), qc);
    return new Iter() {
      IntSet ints = new IntSet();
      ItemSet set;

      @Override
      public Item next() throws QueryException {
        for(Item item; (item = qc.next(values)) != null;) {
          if(ints != null) {
            // try to treat items as 32-bit integers
            final int v = toInt(item);
            if(v != Integer.MIN_VALUE) {
              if(ints.add(v)) return item;
              continue;
            }
            set = ItemSet.get(collation, info);
            for(final int i : ints.keys()) set.add(Itr.get(i));
            ints = null;
          }
          // fallback
          if(set.add(item)) return item;
        }
        return null;
      }
    };
  }

  @Override
  public Value value(final QueryContext qc) throws QueryException {
    final Iter values = arg(0).atomIter(qc, info);
    final Collation collation = toCollation(arg(1), qc);

    // try to treat items as 32-bit integers
    final IntList list = new IntList();
    IntSet ints = new IntSet();
    Item item;
    while((item = qc.next(values)) != null) {
      final int v = toInt(item);
      if(v == Integer.MIN_VALUE) break;
      if(ints.add(v)) list.add(v);
    }
    final Value intseq = IntSeq.get(list.finish());
    if(item == null) return intseq;

    // fallback
    final ValueBuilder vb = new ValueBuilder(qc).add(intseq);
    final ItemSet set = ItemSet.get(collation, info);
    for(final int i : ints.keys()) set.add(Itr.get(i));
    ints = null;
    do {
      if(set.add(item)) vb.add(item);
    } while((item = qc.next(values)) != null);
    return vb.value(this);
  }

  @Override
  protected void simplifyArgs(final CompileContext cc) throws QueryException {
    super.simplifyArgs(cc);
    if(!defined(1)) arg(0, arg -> arg.simplifyFor(Simplify.DISTINCT, cc));
  }

  @Override
  protected Expr opt(final CompileContext cc) throws QueryException {
    final Expr values = arg(0);
    final SeqType st = values.seqType();
    if(st.zero()) return values;

    // X => sort() => distinct-values()  ->  X => distinct-values() => sort()
    if(SORT.is(values) && (values.args().length == 1 ||
        values.arg(0).seqType().type.instanceOf(AtomType.ANY_ATOMIC_TYPE))) {
      final ExprList list = new ExprList().add(values.args());
      list.set(0, cc.function(DISTINCT_VALUES, info, values.arg(0)));
      return cc.function(SORT, info, list.finish());
    }
    // distinct-values(distinct-values($data))  ->  distinct-values($data)
    if(DISTINCT_VALUES.is(values) && arg(1).equals(values.arg(1))) return values;

    final Expr opt = optStats(cc);
    if(opt != null) return opt;

    final AtomType type = st.type.atomic();
    if(type != null) {
      if(!defined(1)) {
        // distinct-values(1 to 10)  ->  1 to 10
        if(values instanceof Range || values instanceof RangeSeq) return values;
        // distinct-values($string)  ->  $string
        // distinct-values($node)  ->  data($node)
        if(st.zeroOrOne() && !st.mayBeArray())
          return type == st.type ? values : cc.function(Function.DATA, info, exprs);
      }
      // assign atomic type of argument
      exprType.assign(type);
    }
    return this;
  }

  /**
   * Rewrites the function call to a duplicate check.
   * @param op comparison operator
   * @param cc compilation context
   * @return new function or {@code null}
   * @throws QueryException query context
   */
  public Expr duplicates(final OpV op, final CompileContext cc) throws QueryException {
    if(op == OpV.LT) return Bln.FALSE;
    if(op == OpV.GE) return Bln.TRUE;

    final Expr dupl = cc.function(Function.DUPLICATE_VALUES, info, exprs);
    return cc.function(op == OpV.LE || op == OpV.EQ ? Function.EMPTY : Function.EXISTS, info, dupl);
  }

  /**
   * Tries to evaluate distinct values based on the database statistics.
   * @param cc compilation context
   * @return sequence of distinct values or {@code null}
   * @throws QueryException query exception
   */
  private Expr optStats(final CompileContext cc) throws QueryException {
    final Expr values = arg(0);
    if(!defined(1) && values instanceof final Path path) {
      final ArrayList<Stats> list = path.pathStats();
      if(list != null) {
        final ValueBuilder vb = new ValueBuilder(cc.qc);
        final ItemSet set = ItemSet.get(null, info);
        for(final Stats stats : list) {
          if(!StatsType.isCategory(stats.type)) return null;
          for(final byte[] value : stats.values) {
            final Atm item = Atm.get(value);
            if(set.add(item)) vb.add(item);
          }
        }
        return vb.value(this);
      }
    }
    return null;
  }
}

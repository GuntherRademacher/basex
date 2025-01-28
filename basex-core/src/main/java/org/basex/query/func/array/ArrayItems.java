package org.basex.query.func.array;

import java.util.*;

import org.basex.query.*;
import org.basex.query.CompileContext.*;
import org.basex.query.expr.*;
import org.basex.query.func.*;
import org.basex.query.iter.*;
import org.basex.query.value.*;
import org.basex.query.value.array.*;
import org.basex.query.value.item.*;
import org.basex.query.value.type.*;

/**
 * Function implementation.
 *
 * @author BaseX Team, BSD License
 * @author Christian Gruen
 */
public final class ArrayItems extends StandardFunc {
  @Override
  public Iter iter(final QueryContext qc) throws QueryException {
    final XQArray array = toArray(arg(0), qc);

    return new Iter() {
      final Iterator<Value> values = array.iterator(0);
      Iter ir;

      @Override
      public Item next() throws QueryException {
        while(true) {
          if(ir != null) {
            final Item item = qc.next(ir);
            if(item != null) return item;
          }
          if(!values.hasNext()) return null;
          ir = values.next().iter();
        }
      }
      @Override
      public Item get(final long i) throws QueryException {
        return array.get(i).item(qc, info);
      }
      @Override
      public long size() {
        return array.funcType().declType.one() ? array.structSize() : -1;
      }
    };
  }

  @Override
  public Value value(final QueryContext qc) throws QueryException {
    final XQArray array = toArray(arg(0), qc);
    return array.values(qc);
  }

  @Override
  protected Expr opt(final CompileContext cc) {
    final Type tp = arg(0).seqType().type;
    if(tp instanceof ArrayType) exprType.assign(((ArrayType) tp).valueType.with(Occ.ZERO_OR_MORE));
    return this;
  }

  @Override
  public Expr simplifyFor(final Simplify mode, final CompileContext cc) throws QueryException {
    final Expr array = arg(0);
    final Expr expr = mode.oneOf(Simplify.STRING, Simplify.NUMBER, Simplify.DATA) &&
        array.seqType().type instanceof ArrayType ? array : this;
    return cc.simplify(this, expr, mode);
  }
}
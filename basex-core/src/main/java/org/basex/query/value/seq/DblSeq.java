package org.basex.query.value.seq;

import java.io.*;
import java.util.*;

import org.basex.core.*;
import org.basex.io.in.DataInput;
import org.basex.io.out.DataOutput;
import org.basex.query.*;
import org.basex.query.value.*;
import org.basex.query.value.item.*;
import org.basex.query.value.type.*;
import org.basex.util.*;

/**
 * Sequence of items of type {@link Itr xs:double}, containing at least two of them.
 *
 * @author BaseX Team, BSD License
 * @author Christian Gruen
 */
public final class DblSeq extends NativeSeq {
  /** Values. */
  private final double[] values;

  /**
   * Constructor.
   * @param values bytes
   */
  private DblSeq(final double[] values) {
    super(values.length, AtomType.DOUBLE);
    this.values = values;
  }

  /**
   * Creates a value from the input stream. Called from {@link Store#read(DataInput, QueryContext)}.
   * @param in data input
   * @param type type
   * @param qc query context
   * @return value
   * @throws IOException I/O exception
   * @throws QueryException query exception
   */
  public static Value read(final DataInput in, final Type type, final QueryContext qc)
      throws IOException, QueryException {
    final int size = in.readNum();
    final double[] values = new double[size];
    for(int s = 0; s < size; s++) values[s] = Dbl.parse(in.readToken(), null);
    return get(values);
  }

  @Override
  public void write(final DataOutput out) throws IOException {
    out.writeNum((int) size);
    for(final double v : values) out.writeToken(Token.token(v));
  }

  @Override
  public Dbl itemAt(final long index) {
    return Dbl.get(values[(int) index]);
  }

  @Override
  public Value reverse(final QueryContext qc) {
    final int sz = (int) size;
    final double[] tmp = new double[sz];
    for(int i = 0; i < sz; i++) tmp[sz - i - 1] = values[i];
    return get(tmp);
  }

  @Override
  public boolean equals(final Object obj) {
    return this == obj || (obj instanceof final DblSeq seq ? Arrays.equals(values, seq.values) :
      super.equals(obj));
  }

  @Override
  public double[] toJava() {
    return values;
  }

  // STATIC METHODS ===============================================================================

  /**
   * Creates a sequence with the specified values.
   * @param values values
   * @return value
   */
  public static Value get(final double[] values) {
    final int vl = values.length;
    return vl == 0 ? Empty.VALUE : vl == 1 ? Dbl.get(values[0]) : new DblSeq(values);
  }
}

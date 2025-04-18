package org.basex.query.expr;

import java.util.*;

import org.basex.core.locks.*;
import org.basex.query.*;
import org.basex.query.ann.*;
import org.basex.query.util.*;
import org.basex.query.value.item.*;
import org.basex.util.*;

/**
 * Pragma for database options.
 *
 * @author BaseX Team, BSD License
 * @author Leo Woerteler
 */
public final class BaseXPragma extends Pragma {
  /** Nondeterministic flag. */
  private final boolean ndt;

  /**
   * Constructor.
   * @param name name of pragma
   * @param value value
   */
  public BaseXPragma(final QNm name, final byte[] value) {
    super(name, value);
    ndt = Token.eq(name.local(), Token.token(QueryText.NONDETERMNISTIC));
  }

  @Override
  Object init(final QueryContext qc, final InputInfo ii) {
    return null;
  }

  @Override
  void finish(final QueryContext qc, final Object state) {
  }

  @Override
  public boolean has(final Flag... flags) {
    return Flag.NDT.oneOf(flags) && ndt;
  }

  @Override
  public void accept(final ASTVisitor visitor) {
    visitor.lock(() -> {
      final ArrayList<String> list = new ArrayList<>(1);
      if(name.eq(Annotation._BASEX_LOCK.name)) {
        Collections.addAll(list, Locking.queryLocks(value));
      }
      return list;
    });
  }

  @Override
  public Pragma copy() {
    return new BaseXPragma(name, value);
  }

  @Override
  public boolean simplify() {
    return !ndt;
  }

  @Override
  public boolean equals(final Object obj) {
    return obj instanceof BaseXPragma && super.equals(obj);
  }
}

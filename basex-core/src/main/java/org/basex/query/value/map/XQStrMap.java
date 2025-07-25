package org.basex.query.value.map;

import org.basex.query.*;
import org.basex.query.value.*;
import org.basex.query.value.item.*;
import org.basex.query.value.seq.*;
import org.basex.query.value.type.*;
import org.basex.util.hash.*;
import org.basex.util.list.*;

/**
 * Unmodifiable hash map implementation for strings.
 *
 * @author BaseX Team, BSD License
 * @author Christian Gruen
 */
public final class XQStrMap extends XQHashMap {
  /** Map type. */
  private static final MapType TYPE = MapType.get(AtomType.STRING, SeqType.STRING_O);
  /** Hash map. */
  private final TokenObjectMap<byte[]> map;

  /**
   * Constructor.
   * @param capacity initial capacity
   */
  XQStrMap(final int capacity) {
    super(TYPE);
    map = new TokenObjectMap<>(capacity);
  }

  @Override
  public long structSize() {
    return map.size();
  }

  @Override
  public Str getOrNull(final Item key) throws QueryException {
    if(key.type.isStringOrUntyped()) {
      final int i = map.index(key.string(null));
      if(i != 0) return valueAt(i - 1);
    }
    return null;
  }

  @Override
  public Value keys() {
    return StrSeq.get(map.keys());
  }

  @Override
  public Value items(final QueryContext qc) {
    final long is = structSize();
    final TokenList list = new TokenList(is);
    for(int i = 1; i <= is; i++) list.add(map.value(i));
    return StrSeq.get(list);
  }

  @Override
  public Str keyAt(final int index) {
    return Str.get(map.key(index + 1));
  }

  @Override
  public Str valueAt(final int index) {
    return Str.get(map.value(index + 1));
  }

  @Override
  XQHashMap build(final Item key, final Value value) throws QueryException {
    final byte[] k = toStr(key), v = toStr(value);
    if(k != null) {
      if(v != null) {
        map.put(k, v);
        return this;
      }
      return new XQStrValueMap(map.capacity() - 2).build(this).build(key, value);
    }
    return new XQItemValueMap(map.capacity() - 2).build(this).build(key, value);
  }
}

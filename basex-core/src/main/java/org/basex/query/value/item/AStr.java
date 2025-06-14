package org.basex.query.value.item;

import java.util.*;

import org.basex.query.*;
import org.basex.query.util.*;
import org.basex.query.util.collation.*;
import org.basex.query.value.type.*;
import org.basex.util.*;
import org.basex.util.list.*;

/**
 * Abstract string item.
 *
 * @author BaseX Team, BSD License
 * @author Christian Gruen
 */
public abstract class AStr extends Item {
  /** ASCII offset flag. */
  private static final int[] ASCII = {};

  /** String data ({@code null} if not cached yet). */
  byte[] value;
  /** Character offsets. {@code null}: not cached yet, {@code ASCII}: ASCII, otherwise: offsets. */
  private int[] offsets;

  /**
   * Constructor.
   */
  AStr() {
    super(AtomType.STRING);
  }

  /**
   * Constructor, specifying a type and value.
   * @param value value
   * @param type atomic type
   */
  AStr(final byte[] value, final Type type) {
    super(type);
    this.value = value;
  }

  @Override
  public final boolean bool(final InputInfo ii) throws QueryException {
    return string(ii).length != 0;
  }

  /**
   * Checks if the string only consists of ASCII characters.
   * @param info input info (can be {@code null})
   * @return result of check
   * @throws QueryException query exception
   */
  public boolean ascii(final InputInfo info) throws QueryException {
    if(offsets == null) {
      final int[] off = Token.cpOffsets(string(info));
      offsets = off == null ? ASCII : off;
    }
    return offsets == ASCII;
  }

  /**
   * Returns the string length.
   * @param info input info (can be {@code null})
   * @return result of check
   * @throws QueryException query exception
   */
  public int length(final InputInfo info) throws QueryException {
    return ascii(info) ? string(info).length : offsets.length;
  }

  /**
   * Returns the single characters of the string.
   * @param info input info (can be {@code null})
   * @return result of check
   * @throws QueryException query exception
   */
  public byte[][] characters(final InputInfo info) throws QueryException {
    final TokenList list = new TokenList(length(info));
    Token.forEachCp(string(info), cp -> list.add(Token.cpToken(cp)));
    return list.finish();
  }

  /**
   * Returns the codepoints of the string.
   * @param info input info (can be {@code null})
   * @return codepoints
   * @throws QueryException query exception
   */
  public int[] codepoints(final InputInfo info) throws QueryException {
    final IntList list = new IntList(length(info));
    Token.forEachCp(string(info), list::add);
    return list.finish();
  }

  /**
   * Returns a substring.
   * @param info input info (can be {@code null})
   * @param start start position
   * @param end end position
   * @return substring
   * @throws QueryException query exception
   */
  public AStr substring(final InputInfo info, final int start, final int end)
      throws QueryException {
    if(start == 0 && end == length(info)) return this;

    final byte[] token = string(info);
    final boolean ascii =  ascii(info);
    final int s = ascii ? start : offsets[start];
    final int e = ascii ? end : end < offsets.length ? offsets[end] : token.length;
    return Str.get(Arrays.copyOfRange(token, s, e));
  }

  @Override
  public final boolean comparable(final Item item) {
    return item.type.isStringOrUntyped();
  }

  @Override
  public final boolean equal(final Item item, final Collation coll, final InputInfo ii)
      throws QueryException {
    return Token.eq(string(ii), item.string(ii), Collation.get(coll, ii));
  }

  @Override
  public final boolean deepEqual(final Item item, final DeepEqual deep) throws QueryException {
    return comparable(item) && Token.eq(string(deep.info), item.string(deep.info), deep);
  }

  @Override
  public final int compare(final Item item, final Collation coll, final boolean transitive,
      final InputInfo ii) throws QueryException {
    return Token.compare(string(ii), item.string(ii), Collation.get(coll, ii));
  }

  @Override
  public boolean equals(final Object obj) {
    if(this == obj) return true;
    if(!(obj instanceof final AStr a)) return false;
    return type == a.type && Token.eq(value, a.value);
  }

  @Override
  public void toString(final QueryString qs) {
    qs.quoted(value);
  }
}

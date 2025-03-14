package org.basex.query.value.map;

import org.basex.query.value.*;
import org.basex.query.value.item.*;
import org.basex.util.*;

/**
 * Empty {@link XQMap} node.
 *
 * @author BaseX Team, BSD License
 * @author Leo Woerteler
 */
final class TrieEmpty extends TrieNode {
  /** The empty node. */
  static final TrieNode VALUE = new TrieEmpty();

  /**
   * Private constructor.
   */
  private TrieEmpty() {
    super(0);
  }

  @Override
  public TrieNode put(final int hs, final int lv, final TrieUpdate update) {
    return new TrieLeaf(hs, update.key, update.value);
  }

  @Override
  TrieNode remove(final int hs, final int lv, final TrieUpdate update) {
    return this;
  }

  @Override
  Value get(final int hs, final Item ky, final int lv) {
    return null;
  }

  @Override
  void add(final TokenBuilder tb, final String indent) {
    tb.add("{}");
  }
}

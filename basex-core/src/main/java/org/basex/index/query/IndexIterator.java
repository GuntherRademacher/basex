package org.basex.index.query;

/**
 * Iterator for returning index results.
 *
 * @author BaseX Team, BSD License
 * @author Christian Gruen
 */
public interface IndexIterator {
  /** Empty iterator. */
  IndexIterator EMPTY = new IndexIterator() {
    @Override
    public boolean more() { return false; }
    @Override
    public int pre() { return 0; }
    @Override
    public int size() { return 0; }
  };

  /**
   * Returns true if more results can be returned.
   * @return result of check
   */
  boolean more();

  /**
   * Returns the next PRE value.
   * @return PRE value
   */
  int pre();

  /**
   * Returns an approximate number of index results.
   * @return approximate number of results
   */
  int size();
}

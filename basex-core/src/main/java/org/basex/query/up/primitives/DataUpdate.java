package org.basex.query.up.primitives;

import org.basex.data.*;
import org.basex.util.*;

/**
 * Update that operates on a data reference.
 *
 * @author BaseX Team, BSD License
 * @author Lukas Kircher
 */
public abstract class DataUpdate extends Update {
  /** Target data reference. */
  protected final Data data;

  /**
   * Constructor.
   * @param type type of this operation
   * @param data target data reference
   * @param info input info (can be {@code null})
   */
  protected DataUpdate(final UpdateType type, final Data data, final InputInfo info) {
    super(type, info);
    this.data = data;
  }

  /**
   * Returns the target data reference.
   * @return data
   */
  public final Data data() {
    return data;
  }
}

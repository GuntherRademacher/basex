package org.basex.util.ft;

import org.basex.util.*;

/**
 * Full-text units.
 *
 * @author BaseX Team, BSD License
 * @author Christian Gruen
 */
public enum FTUnit {
  /** Word unit. */      WORDS,
  /** Sentence unit. */  SENTENCES,
  /** Paragraph unit. */ PARAGRAPHS;

  /**
   * Returns a string representation.
   * @return string representation
   */
  @Override
  public String toString() {
    return Enums.string(this);
  }
}

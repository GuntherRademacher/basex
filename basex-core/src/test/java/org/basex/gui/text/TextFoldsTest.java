package org.basex.gui.text;

import static org.basex.util.Token.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

/**
 * Tests XML fold range detection.
 *
 * @author BaseX Team, BSD License
 * @author Christian Gruen
 */
public final class TextFoldsTest {
  /** Nested XML ranges are detected and sorted by their start line. */
  @Test public void nested() {
    final TextFolds folds = new TextFolds();
    final byte[] text = token("<a>\n  <b>\n    <c/>\n  </b>\n</a>\n");
    assertTrue(folds.refresh(text, true));
    assertTrue(folds.visible());

    assertTrue(folds.starts(0));
    assertTrue(folds.starts(4));
    assertFalse(folds.starts(10));

    assertTrue(folds.toggle(0));
    assertTrue(folds.collapsed(0));
    assertEquals(4, folds.hideStart(0));
    assertEquals(text.length, folds.hideEnd(0));

    assertTrue(folds.toggle(4));
    assertEquals(10, folds.hideStart(4));
    assertEquals(26, folds.hideEnd(4));
  }

  /** Single-line elements are not foldable. */
  @Test public void singleLine() {
    final TextFolds folds = new TextFolds();
    assertTrue(folds.refresh(token("<a><b/></a>\n"), true));
    assertFalse(folds.visible());
  }

  /** Disabled fold detection yields no ranges. */
  @Test public void disabled() {
    final TextFolds folds = new TextFolds();
    assertTrue(folds.refresh(token("<a>\n<b/>\n</a>\n"), false));
    assertFalse(folds.visible());
    assertFalse(folds.toggle(0));
  }
}

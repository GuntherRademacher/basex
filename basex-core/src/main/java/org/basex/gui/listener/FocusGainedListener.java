package org.basex.gui.listener;

import java.awt.event.*;

/**
 * Listener interface for gained focus.
 *
 * @author BaseX Team, BSD License
 * @author Christian Gruen
 */
@FunctionalInterface
public interface FocusGainedListener extends FocusListener {
  @Override
  default void focusLost(final FocusEvent e) { }
}

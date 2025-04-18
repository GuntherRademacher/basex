package org.basex.server;

import org.basex.io.out.*;
import org.junit.jupiter.api.*;

/**
 * This class tests the client/server session API with an output stream.
 *
 * @author BaseX Team, BSD License
 * @author Christian Gruen
 */
public final class ClientSessionOutTest extends ClientSessionTest {
  /** Initializes the test. */
  @Override
  @BeforeEach public void startSession() {
    out = new ArrayOutput();
    super.startSession();
  }
}

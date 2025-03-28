package org.basex.local.single;

import org.basex.*;
import org.basex.core.*;
import org.basex.core.cmd.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Test;

/**
 * This class adds and retrieves documents in a collection.
 *
 * @author BaseX Team, BSD License
 * @author Christian Gruen
 */
public final class CollAddTest extends SandboxTest {
  /**
   * Initializes the tests.
   */
  @BeforeAll
  public static void init() {
    set(MainOptions.INTPARSE, true);
  }

  /**
   * Adds 100 documents.
   */
  @Test public void add100() {
    add(100, false);
  }

  /**
   * Adds 1000 documents.
   */
  @Test public void add1000() {
    add(1000, false);
  }

  /**
   * Adds 10000 documents.
   */
  @Test public void add10000() {
    add(10000, false);
  }

  /**
   * Adds 100000 documents.
   */
  @Test public void add100000() {
    add(100000, false);
  }

  /**
   * Adds 1000 documents.
   */
  @Test public void add100Force() {
    add(100, true);
  }

  /**
   * Adds 1000 documents.
   */
  @Test public void add1000Force() {
    add(1000, true);
  }

  /**
   * Adds 1000 documents.
   */
  @Test public void add10000Force() {
    add(10000, true);
  }

  /**
   * Creates a database.
   * @param size number of documents to be added
   * @param flush force flush of updates
   */
  private static void add(final int size, final boolean flush) {
    set(MainOptions.AUTOFLUSH, flush);
    // Create test database
    execute(new CreateDB(NAME));
    // Add documents
    for(int s = 0; s < size; s++) execute(new Add(Integer.toString(s), "<xml/>"));
    // Close database
    execute(new DropDB(NAME));
  }
}

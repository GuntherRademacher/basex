package org.basex.query;

/**
 * <p>Interface for handling external query resources.
 * Implemented by XQuery modules that open new connections and resources.</p>
 *
 * <p>If the interface is implemented by a Java module that subclasses {@link QueryModule},
 * its {@link #close()} method will be called after the query has been evaluated.
 *
 * @author BaseX Team, BSD License
 * @author Christian Gruen
 */
@FunctionalInterface
public interface QueryResource {
  /**
   * Closes a resource.
   */
  void close();
}

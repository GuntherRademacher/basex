package org.basex.query;

import static org.basex.query.QueryError.*;

import java.util.*;
import java.util.Map.*;

import org.basex.core.*;
import org.basex.util.*;
import org.basex.util.options.*;

/**
 * Query-specific database options.
 *
 * @author BaseX Team, BSD License
 * @author Christian Gruen
 */
final class QueryOptions {
  /** Query context. */
  private final QueryContext qc;
  /** Global database options (will be reassigned after query execution). */
  private final HashMap<Option<?>, Object> cachedOpts = new HashMap<>();
  /** Local query options (key/value pairs), supplied by option declarations. */
  private final HashMap<Option<?>, Object> localOpts = new HashMap<>();
  /** Main options instance (for parsing entries). */
  private MainOptions dummyOptions;

  /**
   * Constructor.
   * @param qc query context
   */
  QueryOptions(final QueryContext qc) {
    this.qc = qc;
  }

  /**
   * Remembers a parsed database option.
   * @param name name of option
   * @param value value of option
   * @param parser query parser
   * @throws QueryException query exception
   */
  void add(final String name, final String value, final QueryParser parser) throws QueryException {
    final String key = name.toUpperCase(Locale.ENGLISH);
    final MainOptions options = qc.context.options;
    final Option<?> option = options.option(key);
    if(option == null) throw BASEX_OPTIONSINV_X.get(parser.info(), options.similar(name));

    // try to assign option to dummy options
    if(dummyOptions == null) dummyOptions = new MainOptions(false);
    try {
      dummyOptions.assign(key, value);
    } catch(final BaseXException ex) {
      Util.debug(ex);
      throw BASEX_OPTIONS_X_X.get(parser.info(), key, value);
    }
    // if successful, cache assigned value
    localOpts.put(option, dummyOptions.get(option));
  }

  /**
   * Compiles all options.
   */
  void compile() {
    // cache old database options at compile time; assign local ones
    dummyOptions = null;
    final MainOptions mopts = qc.context.options;
    for(final Entry<Option<?>, Object> entry : localOpts.entrySet()) {
      final Option<?> option = entry.getKey();
      cachedOpts.put(option, mopts.get(option));
      mopts.put(option, entry.getValue());
    }
  }

  /**
   * Reassigns original options.
   */
  void close() {
    cachedOpts.forEach(qc.context.options::put);
  }
}

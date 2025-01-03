package org.basex.query.func.json;

import static org.basex.query.QueryError.*;

import java.io.*;

import org.basex.io.*;
import org.basex.query.*;
import org.basex.query.func.fn.*;
import org.basex.query.value.item.*;
import org.basex.query.value.seq.*;
import org.basex.util.*;

/**
 * Function implementation.
 *
 * @author BaseX Team, BSD License
 * @author Christian Gruen
 */
public class JsonDoc extends FnJsonDoc {
  @Override
  public Item item(final QueryContext qc, final InputInfo ii) throws QueryException {
    final String source = toStringOrNull(arg(0), qc);
    return source != null ? parse(toIO(source), qc) : Empty.VALUE;
  }

  /**
   * Parses the input and creates an XML document.
   * @param io input data
   * @param qc query context
   * @return node
   * @throws QueryException query exception
   */
  protected final Item parse(final IO io, final QueryContext qc) throws QueryException {
    try {
      return converter(qc, null).convert(io);
    } catch(final IOException ex) {
      throw JSON_PARSE_X.get(info, ex);
    }
  }
}

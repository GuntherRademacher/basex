package org.basex.io.serial.csv;

import static org.basex.query.QueryError.*;
import static org.basex.util.Token.*;

import java.io.*;

import org.basex.build.csv.*;
import org.basex.io.serial.*;
import org.basex.query.value.item.*;
import org.basex.util.*;
import org.basex.util.list.*;

/**
 * This class serializes items as CSV.
 *
 * @author BaseX Team, BSD License
 * @author Christian Gruen
 */
abstract class CsvSerializer extends StandardSerializer {
  /** CSV options. */
  final CsvOptions copts;
  /** Separator. */
  final int separator;
  /** Generate quotes. */
  final boolean quotes;
  /** Generate backslashes. */
  final boolean backslashes;

  /** Header flag. */
  boolean header;

  /**
   * Constructor.
   * @param os output stream
   * @param sopts serialization parameters
   * @throws IOException I/O exception
   */
  CsvSerializer(final OutputStream os, final SerializerOptions sopts) throws IOException {
    super(os, sopts);
    copts = sopts.get(SerializerOptions.CSV);
    quotes = copts.get(CsvOptions.QUOTES);
    backslashes = copts.get(CsvOptions.BACKSLASHES);
    header = copts.get(CsvOptions.HEADER);
    separator = copts.separator();
  }

  /**
   * Prints a record with the specified entries.
   * @param entries record entries to be printed (will be reset after serialization)
   * @throws IOException I/O exception
   */
  final void record(final TokenList entries) throws IOException {
    // print fields, skip trailing empty contents
    final int fs = entries.size();
    for(int i = 0; i < fs; i++) {
      final byte[] v = entries.get(i);
      if(i != 0) out.print(separator);

      byte[] txt = v != null ? v : EMPTY;
      final boolean delim = contains(txt, separator) || contains(txt, '\n');
      final boolean special = contains(txt, '\r') || contains(txt, '\t') || contains(txt, '"');
      if(delim || special || backslashes && contains(txt, '\\')) {
        final TokenBuilder tb = new TokenBuilder();
        if(delim && !backslashes && !quotes)
          throw CSV_SERIALIZE_X_X.getIO("Output must be put into quotes", txt);

        if(quotes && (delim || special)) tb.add('"');
        final TokenParser tp = new TokenParser(txt);
        while(tp.more()) {
          final int cp = tp.next();
          if(backslashes) {
            if(cp == '\n') tb.add("\\n");
            else if(cp == '\r') tb.add("\\r");
            else if(cp == '\t') tb.add("\\t");
            else if(cp == '"') tb.add("\\\"");
            else if(cp == '\\') tb.add("\\\\");
            else if(cp == separator && !quotes) tb.add('\\').add(cp);
            else tb.add(cp);
          } else {
            if(cp == '"') tb.add('"');
            tb.add(cp);
          }
        }
        if(quotes && (delim || special)) tb.add('"');
        txt = tb.finish();
      }
      out.print(txt);
    }
    out.print('\n');
    entries.reset();
  }

  @Override
  protected void atomic(final Item value) throws IOException {
    throw CSV_SERIALIZE_X.getIO("Atomic items cannot be serialized");
  }
}

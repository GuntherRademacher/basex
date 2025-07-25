package org.basex.io.serial.csv;

import static org.basex.query.QueryError.*;

import java.io.*;

import org.basex.io.parse.csv.*;
import org.basex.io.serial.*;
import org.basex.query.*;
import org.basex.query.value.*;
import org.basex.query.value.array.*;
import org.basex.query.value.item.*;
import org.basex.query.value.map.*;
import org.basex.util.list.*;

/**
 * This class serializes a map as CSV. The input must conform to the XQuery CSV representation.
 *
 * @author BaseX Team, BSD License
 * @author Christian Gruen
 */
public final class CsvXQuerySerializer extends CsvSerializer {
  /**
   * Constructor.
   * @param os output stream
   * @param sopts serialization parameters
   * @throws IOException I/O exception
   */
  public CsvXQuerySerializer(final OutputStream os, final SerializerOptions sopts)
      throws IOException {
    super(os, sopts);
  }

  @Override
  public void serialize(final Item item) throws IOException {
    if(sep && level == 0) out.print(' ');

    if(!(item instanceof final XQMap map))
      throw CSV_SERIALIZE_X_X.getIO("Top level must be a map, found " + item.type, item);

    final TokenList tl = new TokenList();
    try {
      // print header
      if(header) {
        if(!map.contains(CsvXQueryConverter.NAMES))
          throw CSV_SERIALIZE_X.getIO("Map has no 'names' key");
        record(map.get(CsvXQueryConverter.NAMES), tl);
      }
      // print records
      if(!map.contains(CsvXQueryConverter.RECORDS))
        throw CSV_SERIALIZE_X.getIO("Map has no 'records' key");
      for(final Item record : map.get(CsvXQueryConverter.RECORDS)) {
        record(record, tl);
      }
    } catch(final QueryException ex) {
      throw new QueryIOException(ex);
    }
    sep = true;
  }

  /**
   * Serializes a single line (header or contents).
   * @param line line to be serialized
   * @param tl token list
   * @throws QueryException query exception
   * @throws IOException I/O exception
   */
  private void record(final Value line, final TokenList tl) throws QueryException, IOException {
    if(!(line instanceof final XQArray array))
      throw CSV_SERIALIZE_X_X.getIO("Array expected, found " + line.seqType(), line);
    for(final Value value : array.iterable()) {
      if(value.size() != 1) throw CSV_SERIALIZE_X_X.getIO(
          "Item expected, found " + value.seqType(), value);
      tl.add(((Item) value).string(null));
    }
    record(tl);
  }
}

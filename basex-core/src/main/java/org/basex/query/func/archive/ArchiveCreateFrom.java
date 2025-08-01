package org.basex.query.func.archive;

import static org.basex.query.QueryError.*;

import java.io.*;
import java.util.*;
import java.util.AbstractMap.*;

import org.basex.io.*;
import org.basex.io.out.*;
import org.basex.query.*;
import org.basex.query.value.*;
import org.basex.query.value.item.*;
import org.basex.query.value.seq.*;
import org.basex.util.*;
import org.basex.util.list.*;

/**
 * Function implementation.
 *
 * @author BaseX Team, BSD License
 * @author Christian Gruen
 */
public final class ArchiveCreateFrom extends ArchiveCreate {
  @Override
  public B64 item(final QueryContext qc, final InputInfo ii) throws QueryException {
    final IOFile root = new IOFile(toPath(arg(0), qc));
    final CreateFromOptions options = toOptions(arg(1), new CreateFromOptions(), qc);
    Value entries = arg(2).value(qc);

    final boolean recursive = options.get(CreateFromOptions.RECURSIVE);
    final boolean rootDir = options.get(CreateFromOptions.ROOT_DIR);
    if(!root.isDir()) throw FILE_NO_DIR_X.get(info, root);

    if(entries.isEmpty()) {
      final TokenList tl = new TokenList();
      if(recursive) {
        for(final String file : root.descendants()) tl.add(file);
      } else {
        for(final IOFile file : root.children()) {
          if(!file.isDir()) tl.add(file.name());
        }
      }
      entries = StrSeq.get(tl);
    }

    final int level = level(options);
    final String format = options.get(CreateOptions.FORMAT).toLowerCase(Locale.ENGLISH);
    final String dir = rootDir && root.parent() != null ? root.name() + '/' : "";
    final ArrayOutput ao = new ArrayOutput();
    try(ArchiveOut out = ArchiveOut.get(format, info, ao)) {
      out.level(level);
      try {
        for(final Item item : entries) {
          final IOFile file = new IOFile(root, toString(item, qc));
          if(!file.exists()) throw FILE_NOT_FOUND_X.get(info, file);
          if(file.isDir()) throw FILE_IS_DIR_X.get(info, file);
          add(new SimpleEntry<>(item, B64.get(file.read())), out, level, dir, qc);
        }
      } catch(final IOException ex) {
        throw ARCHIVE_ERROR_X.get(info, ex);
      }
    }
    return B64.get(ao.finish());
  }
}

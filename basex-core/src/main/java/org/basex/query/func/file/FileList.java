package org.basex.query.func.file;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;
import java.util.regex.*;

import org.basex.io.*;
import org.basex.query.*;
import org.basex.query.value.*;
import org.basex.query.value.seq.*;
import org.basex.util.*;
import org.basex.util.list.*;

/**
 * Function implementation.
 *
 * @author BaseX Team, BSD License
 * @author Christian Gruen
 */
public class FileList extends FileFn {
  @Override
  public Value eval(final QueryContext qc) throws QueryException, IOException {
    final Path dir = toPath(arg(0), qc).toRealPath();
    final boolean recursive = toBooleanOrFalse(arg(1), qc);
    final String pattern = toStringOrNull(arg(2), qc);

    final Pattern pttrn = pattern == null ? null :
      Pattern.compile(IOFile.regex(pattern, false), Prop.CASE ? 0 : Pattern.CASE_INSENSITIVE);
    final TokenList tl = new TokenList();
    list(dir, recursive, pttrn, tl, dir.getNameCount(), qc);
    return StrSeq.get(tl);
  }

  /**
   * Returns full file paths.
   * @param recursive recursive flag
   * @param qc query context
   * @return file paths
   * @throws QueryException query exception
   * @throws IOException I/O exception
   */
  Value paths(final boolean recursive, final QueryContext qc) throws QueryException, IOException {
    final TokenList tl = new TokenList();
    list(toPath(arg(0), qc), recursive, null, tl, -1, qc);
    return StrSeq.get(tl);
  }

  /**
   * Collects the subdirectories and files of the specified directory.
   * @param root root path
   * @param recursive recursive flag
   * @param pattern file name pattern; ignored if {@code null}
   * @param list file list
   * @param index index of root path
   * @param qc query context
   * @throws IOException I/O exception
   */
  private static void list(final Path root, final boolean recursive, final Pattern pattern,
      final TokenList list, final int index, final QueryContext qc) throws IOException {

    // filter function for adding results
    final BiConsumer<Path, Boolean> add = (child, dir) -> {
      if(pattern == null || pattern.matcher(child.getFileName().toString()).matches()) {
        final Path path = index < 0 ? child : child.subpath(index, child.getNameCount());
        list.add(get(path, dir).string());
      }
    };

    // collect directories and files first (reduces number of open directory streams)
    final ArrayList<Path> dirs = new ArrayList<>(), files = new ArrayList<>();
    try(DirectoryStream<Path> paths = Files.newDirectoryStream(root)) {
      for(final Path path : paths) {
        qc.checkStop();
        (Files.isDirectory(path) ? dirs : files).add(path);
      }
    } catch(final IOException ex) {
      // skip entries that cannot be accessed; throw exception only on root level
      if(index == -1 || index == root.getNameCount()) {
        Util.debug(ex);
        throw ex;
      }
    }

    // add directories
    for(final Path dir : dirs) {
      add.accept(dir, true);
      // recursive traversal: do not follow links
      if(recursive && !Files.isSymbolicLink(dir)) {
        list(dir, true, pattern, list, index == -1 ? -2 : index, qc);
      }
    }

    // add files
    for(final Path file : files) {
      add.accept(file, false);
    }
  }
}

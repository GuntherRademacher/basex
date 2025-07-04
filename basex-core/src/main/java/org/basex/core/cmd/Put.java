package org.basex.core.cmd;

import static org.basex.core.Text.*;

import java.io.*;
import java.util.function.*;

import org.basex.build.*;
import org.basex.core.parse.*;
import org.basex.core.users.*;
import org.basex.data.*;
import org.basex.index.resource.*;
import org.basex.io.*;
import org.basex.query.up.atomic.*;
import org.basex.util.list.*;

/**
 * Evaluates the 'put' command and replaces documents in a collection.
 *
 * @author BaseX Team, BSD License
 * @author Christian Gruen
 */
public final class Put extends ACreate {
  /** Add command. */
  private Add add;

  /**
   * Constructor.
   * The input needs to be set via {@link #setInput(InputStream)}.
   * @param path resource path
   */
  public Put(final String path) {
    this(path, null);
  }

  /**
   * Constructor.
   * @param path resource path
   * @param input input reference (local/remote file path or XML string; can be {@code null})
   */
  public Put(final String path, final String input) {
    super(Perm.WRITE, true, path, input);
  }

  @Override
  protected boolean run() {
    // check if the input source has already been initialized
    if(in == null) {
      final IO io = IO.get(args[1]);
      if(!io.exists()) return error(RES_NOT_FOUND_X, io);
      in = io.inputSource();
    }

    final String path = MetaData.normPath(args[0]);
    if(path == null) return error(PATH_INVALID_X, args[0]);

    final Data data = context.data();
    final IOFile bin = data.meta.file(path, ResourceType.BINARY);

    return update(data, () -> put(data, bin, path));
  }

  /**
   * Puts (adds or replaces) resources in the specified database.
   * @param data database
   * @param bin binary file (can be {@code null})
   * @param path target path
   * @return success flag
   */
  private boolean put(final Data data, final IOFile bin, final String path) {
    context.invalidate();

    // retrieve old list of resources
    final AtomicUpdateCache auc = new AtomicUpdateCache(data);
    final IntList docs = data.resources.docs(path);
    final int ds = docs.size();
    final IntConsumer exec = start -> {
      for(int d = start; d < ds; d++) auc.addDelete(docs.get(d));
      auc.execute(false);
    };

    int bs = 0;
    if(bin != null && bin.exists()) {
      // replace binary file if it already exists
      final BinaryPut put = new BinaryPut(path);
      put.setInput(in);
      put.lock = false;
      if(!put.run(context)) return error(put.info());
      bs = 1;
      exec.accept(0);
    } else {
      // otherwise, add new document as xml
      add = new Add(path);
      try {
        add.setInput(in);
        add.init(context, out);
        if(!add.build()) return error(add.info());

        final DataClip clip = new DataClip(add.tmpData);
        int d = 0;
        if(docs.isEmpty()) {
          auc.addInsert(data.meta.size, -1, clip);
        } else {
          auc.addReplace(docs.get(d++), clip);
        }
        exec.accept(d);
      } finally {
        add.finish();
      }
    }

    return info(RES_REPLACED_X_X, ds + bs, jc().performance);
  }

  @Override
  public void build(final CmdBuilder cb) {
    cb.init().arg(0).add(1);
  }

  @Override
  public String shortInfo() {
    return PUT + DOTS;
  }

  @Override
  public double progressInfo() {
    final Builder builder = add != null ? add.builder : null;
    return builder != null ? builder.progressInfo() : 0;
  }
}

package org.basex.io.out;

import java.io.*;

import org.basex.io.*;

/**
 * This class uses a byte buffer to speed up output stream processing.
 *
 * @author BaseX Team, BSD License
 * @author Christian Gruen
 * @author Tim Petrowsky
 */
public final class BufferOutput extends OutputStream {
  /** Buffer size. */
  private final int bufsize;
  /** Byte buffer. */
  private final byte[] buffer;
  /** Reference to the data output stream. */
  private final OutputStream out;
  /** Current buffer position. */
  private int pos;

  /**
   * Returns a buffered output stream.
   * @param os output stream
   * @return stream
   */
  public static BufferOutput get(final OutputStream os) {
    return os instanceof final BufferOutput bo ? bo : new BufferOutput(os);
  }

  /**
   * Constructor.
   * @param file target file
   * @throws IOException I/O exception
   */
  public BufferOutput(final IOFile file) throws IOException {
    this(file.outputStream());
  }

  /**
   * Constructor with a default buffer size.
   * @param out the stream to write to
   */
  BufferOutput(final OutputStream out) {
    this(out, IO.BLOCKSIZE);
  }

  /**
   * Constructor with a specific buffer size.
   * @param out the stream to write to
   * @param bufsize buffer size
   */
  BufferOutput(final OutputStream out, final int bufsize) {
    this.out = out;
    this.bufsize = bufsize;
    buffer = new byte[bufsize];
  }

  @Override
  public void write(final int b) throws IOException {
    if(pos == bufsize) flush();
    buffer[pos++] = (byte) b;
  }

  @Override
  public void flush() throws IOException {
    out.write(buffer, 0, pos);
    pos = 0;
  }

  @Override
  public void close() throws IOException {
    try {
      flush();
    } finally {
      out.close();
    }
  }
}

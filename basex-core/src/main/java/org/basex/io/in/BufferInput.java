package org.basex.io.in;

import java.io.*;

import org.basex.io.*;
import org.basex.util.list.*;

/**
 * This class uses an internal buffer to speed up input stream processing.
 *
 * @author BaseX Team, BSD License
 * @author Christian Gruen
 */
public class BufferInput extends InputStream {
  /** Byte buffer. */
  final byte[] array;
  /** Current buffer position. */
  int bpos;
  /** Current buffer size. */
  int bsize;
  /** Total length of input to be processed ({@code -1} if unknown). */
  long length;
  /** Input file. */
  private IO input;

  /** Reference to the data input stream (can be {@code null}). */
  private final InputStream is;
  /** Buffer marker to jump back ({@code -1} if not available). */
  private int bmark;
  /** Number of read bytes. */
  private long read;

  /**
   * Returns a buffered input stream.
   * @param is input stream
   * @return stream
   */
  public static BufferInput get(final InputStream is) {
    return is instanceof final BufferInput bi ? bi : new BufferInput(is);
  }

  /**
   * Returns a buffered input stream.
   * @param input input to be read
   * @return stream
   * @throws IOException I/O Exception
   */
  public static BufferInput get(final IO input) throws IOException {
    final InputStream is = input.inputStream();
    return is instanceof final BufferInput bi ? bi : new BufferInput(is, input.length());
  }

  /**
   * Constructor.
   * @param input input to be read
   * @throws IOException I/O Exception
   */
  public BufferInput(final IO input) throws IOException {
    this(input.inputStream());
    this.input = input;
    length = input.length();
  }

  /**
   * Constructor.
   * @param is input stream
   */
  protected BufferInput(final InputStream is) {
    this(is, is instanceof final BufferInput bi ? bi.length : -1);
  }

  /**
   * Initializes the file reader.
   * @param is input stream
   * @param length of input ({@code -1} if unknown)
   */
  private BufferInput(final InputStream is, final long length) {
    this.is = is;
    this.length = length;
    array = new byte[IO.BLOCKSIZE];
  }

  /**
   * Empty constructor with fixed input.
   * @param array array input
   */
  protected BufferInput(final byte[] array) {
    this.array = array;
    bsize = array.length;
    length = bsize;
    is = null;
  }

  /**
   * Returns the IO reference or {@code null}.
   * @return file reference
   */
  public final IO io() {
    return input;
  }

  /**
   * Returns the next byte. By default, this method calls {@link #readByte()};
   * {@code -1} is returned if all bytes have been read.
   * @return next byte
   * @throws IOException I/O exception
   */
  @Override
  public int read() throws IOException {
    return readByte();
  }

  /**
   * Returns the next unsigned byte.
   * {@code -1} is returned if all bytes have been read.
   * @return next unsigned byte
   * @throws IOException I/O exception
   * @see InputStream#read()
   */
  protected int readByte() throws IOException {
    final int blen = array.length;
    final byte[] buf = array;
    if(bpos >= bsize) {
      read += bsize;
      if(bsize == blen) {
        // reset mark if buffer is full
        bmark = -1;
        bsize = 0;
        bpos = 0;
      }
      int r;
      while((r = is.read(buf, bsize, blen - bsize)) == 0);
      if(r < 0) return -1;
      bsize += r;
    }
    return buf[bpos++] & 0xFF;
  }

  /**
   * Reads a string from the input stream, suffixed by a {@code 0} byte.
   * @return string
   * @throws IOException I/O Exception
   */
  public final String readString() throws IOException {
    final ByteList bl = new ByteList();
    for(int l; (l = read()) > 0;) bl.add(l);
    return bl.toString();
  }

  /**
   * Reads a byte array from the input stream, suffixed by a {@code 0} byte.
   * @return token
   * @throws IOException I/O Exception
   */
  public final byte[] readBytes() throws IOException {
    final ByteList bl = new ByteList();
    for(int l; (l = readByte()) > 0;) bl.add(l);
    return bl.finish();
  }

  @Override
  public final void close() throws IOException {
    if(is != null && !(is instanceof FilterInputStream)) is.close();
  }

  /**
   * Returns the number of read bytes.
   * @return read bytes
   */
  public final long size() {
    return read + bpos;
  }

  /**
   * Returns the input length (can be {@code -1}).
   * @return input length
   */
  public final long length() {
    return length;
  }

  @Override
  public final boolean markSupported() {
    return true;
  }

  @Override
  @SuppressWarnings("sync-override")
  public final void mark(final int m) {
    bmark = bpos;
  }

  @Override
  @SuppressWarnings("sync-override")
  public final void reset() throws IOException {
    if(bmark == -1) throw new IOException("Mark cannot be reset.");
    bpos = bmark;
  }

  /**
   * Retrieves and returns the whole data and closes the stream.
   * @return contents
   * @throws IOException I/O exception
   */
  public byte[] content() throws IOException {
    try {
      if(length > -1) {
        // input length is known in advance
        final int sl = (int) Math.min(Integer.MAX_VALUE, length);
        final byte[] bytes = new byte[sl];
        for(int c = 0; c < sl; c++) bytes[c] = (byte) readByte();
        return bytes;
      }
      // parse until end of stream
      final ByteList bl = new ByteList();
      for(int ch; (ch = readByte()) != -1;) bl.add(ch);
      return bl.finish();
    } finally {
      close();
    }
  }
}

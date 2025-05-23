package org.basex.io.out;

import static org.basex.util.Token.*;

import java.io.*;

import org.basex.io.*;

/**
 * This class is a stream-wrapper for textual data encoded in UTF8.
 *
 * @author BaseX Team, BSD License
 * @author Christian Gruen
 */
public class PrintOutput extends OutputStream {
  /** Output stream reference (can be {@code null}). */
  protected final OutputStream os;
  /** Maximum numbers of bytes to write. */
  protected long max = Long.MAX_VALUE;
  /** Number of bytes written. */
  protected long size;
  /** Number of codepoints in the current line. */
  protected long lineLength;

  /**
   * Constructor, given a filename.
   * @param file file
   * @throws IOException I/O exception
   */
  public PrintOutput(final IOFile file) throws IOException {
    this(new BufferOutput(file));
  }

  /**
   * Constructor, given an output stream.
   * @param os output stream reference (can be {@code null})
   */
  PrintOutput(final OutputStream os) {
    this.os = os;
  }

  /**
   * Returns a new instance for the given output stream.
   * @param out output stream reference
   * @return print output
   */
  public static PrintOutput get(final OutputStream out) {
    return out instanceof final PrintOutput po ? po : new PrintOutput(
           out instanceof ByteArrayOutputStream ||
           out instanceof BufferedOutputStream ||
           out instanceof BufferOutput ? out : new BufferOutput(out));
  }

  /**
   * Sets the maximum number of bytes to be written.
   * Note that the limit might break Unicode characters.
   * @param limit maximum
   */
  public final void setLimit(final int limit) {
    max = limit;
  }

  @Override
  public void write(final int b) throws IOException {
    if(size++ < max) os.write(b);
  }

  /**
   * Prints a single codepoint, and keeps track of the current line length.
   * @param cp codepoint to be printed
   * @throws IOException I/O exception
   */
  public final void print(final int cp) throws IOException {
    print(cp, null);
  }

  /**
   * Prints a single character, and keeps track of the current line length.
   * @param cp codepoint of character to be printed
   * @param fallback fallback function (can be {@code null})
   * @throws IOException I/O exception
   */
  public void print(final int cp, @SuppressWarnings("unused") final Fallback fallback)
      throws IOException {
    if(cp <= 0x7F) {
      write(cp);
      lineLength = cp == '\n' ? 0 : lineLength + 1;
    } else {
      if(cp <= 0x7FF) {
        write(cp >>  6 & 0x1F | 0xC0);
      } else {
        if(cp <= 0xFFFF) {
          write(cp >> 12 & 0x0F | 0xE0);
        } else {
          write(cp >> 18 & 0x07 | 0xF0);
          write(cp >> 12 & 0x3F | 0x80);
        }
        write(cp >>  6 & 0x3F | 0x80);
      }
      write(cp & 0x3F | 0x80);
      ++lineLength;
    }
  }

  /**
   * Prints a token to the output stream.
   * @param token token to be written
   * @throws IOException I/O exception
   */
  public void print(final byte[] token) throws IOException {
    final int tl = token.length;
    for(int t = 0; t < tl; t += cl(token, t)) print(cp(token, t));
  }

  /**
   * Prints a string to the output stream.
   * @param string string to be written
   * @throws IOException I/O exception
   */
  public void print(final String string) throws IOException {
    print(token(string));
  }

  /**
   * Prints a string and newline to the output stream.
   * @param string string to be written
   * @throws IOException I/O exception
   */
  public final void println(final String string) throws IOException {
    print(string);
    print('\n');
  }

  /**
   * Prints a token to the output stream, followed by a system-specific newline.
   * @param token token to be written
   * @throws IOException I/O exception
   */
  public final void println(final byte[] token) throws IOException {
    print(token);
    print('\n');
  }

  /**
   * Returns the number of written bytes.
   * @return number of written bytes
   */
  public final long size() {
    return size;
  }

  /**
   * Returns the length of the current line in codepoints.
   * @return the number of codepoints in the current line
   */
  public long lineLength() {
    return lineLength;
  }

  @Override
  public void flush() throws IOException {
    os.flush();
  }

  @Override
  public void close() throws IOException {
    if(os == System.out || os == System.err) flush();
    else os.close();
  }

  /**
   * Checks if the output stream is exhausted.
   * @return {@code true} if stream is exhausted
   */
  public boolean finished() {
    return size == max;
  }

  /** Fallback function for encoding problems. */
  @FunctionalInterface
  public interface Fallback {
    /**
     * Prints fallback characters if an encoding problem occurs.
     * @param cp codepoint
     * @throws IOException I/O exception
     */
    void print(int cp) throws IOException;
  }
}

package org.basex.io.in;

import static org.basex.util.Strings.*;

import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;

import org.basex.util.*;

/**
 * This abstract class specifies a single method for decoding input to UTF-8.
 * The inheriting classes are optimized for performance and faster than Java's
 * default decoders.
 *
 * @author BaseX Team, BSD License
 * @author Christian Gruen
 */
abstract class TextDecoder {
  /** Encoding. */
  String encoding;
  /** Indicates if input must be valid. */
  boolean validate;

  /**
   * Returns the next codepoint.
   * @param ti text input
   * @return next codepoint
   * @throws IOException I/O exception
   */
  abstract int read(TextInput ti) throws IOException;

  /**
   * Returns a decoder for the specified encoding.
   * @param enc encoding, normalized via {@link Strings#normEncoding}
   * @return decoder
   * @throws IOException I/O exception
   */
  static TextDecoder get(final String enc) throws IOException {
    final TextDecoder td;
    if(enc == UTF8) td = new UTF8();
    else if(enc == UTF32) td = new UTF32();
    else if(enc == UTF16LE) td = new UTF16LE();
    else if(enc == UTF16 || enc == UTF16BE) td = new UTF16BE();
    else td = new Generic(enc);
    td.encoding = enc;
    return td;
  }

  /**
   * Throws an exception for an incomplete codepoint or returns the replacement character (\\uFFFD).
   * @param incomplete add placeholder for missing byte
   * @param bytes bytes
   * @return replacement codepoint
   * @throws IOException I/O exception
   */
  final int invalid(final boolean incomplete, final byte... bytes) throws IOException {
    if(validate) {
      final TokenBuilder tb = new TokenBuilder();
      for(final int b : bytes) {
        if(!tb.isEmpty()) tb.add(", ");
        final int c = b >> 4 & 0x0F, d = b & 0x0F;
        tb.add(c + (c > 9 ? '7' : '0')).add(d + (d > 9 ? '7' : '0'));
      }
      if(incomplete) tb.add(", ??");
      throw new DecodingException("Invalid " + encoding + " character encoding: " + tb);
    }
    return Token.REPLACEMENT;
  }

  /** UTF8 Decoder. */
  private static final class UTF8 extends TextDecoder {
    /** UTF8 cache. */
    private final byte[] cache = new byte[4];

    @Override
    int read(final TextInput ti) throws IOException {
      int ch = ti.readByte();
      if(ch < 0x80) return ch;
      if(ch < 0xC0) return invalid(false, (byte) ch);
      cache[0] = (byte) ch;
      final int cl = Token.cl((byte) ch);
      for(int c = 1; c < cl; ++c) {
        ch = ti.readByte();
        cache[c] = (byte) ch;
        if(ch < 0x80) return invalid(ch < 0, Arrays.copyOf(cache, ch < 0 ? c : c + 1));
      }
      return Token.cp(cache, 0);
    }
  }

  /** UTF16LE Decoder. */
  private static final class UTF16LE extends TextDecoder {
    @Override
    int read(final TextInput ti) throws IOException {
      final int a = ti.readByte();
      if(a < 0) return a;
      final int b = ti.readByte();
      if(b < 0) return invalid(true, (byte) a);
      return a | b << 8;
    }
  }

  /** UTF16BE Decoder. */
  private static final class UTF16BE extends TextDecoder {
    @Override
    int read(final TextInput ti) throws IOException {
      final int a = ti.readByte();
      if(a < 0) return a;
      final int b = ti.readByte();
      if(b < 0) return invalid(true, (byte) a);
      return a << 8 | b;
    }
  }

  /** UTF32 Decoder. */
  private static final class UTF32 extends TextDecoder {
    @Override
    int read(final TextInput ti) throws IOException {
      final int a = ti.readByte();
      if(a < 0) return a;
      final int b = ti.readByte();
      if(b < 0) return invalid(true, (byte) a);
      final int c = ti.readByte();
      if(c < 0) return invalid(true, (byte) a, (byte) b);
      final int d = ti.readByte();
      if(d < 0) return invalid(true, (byte) a, (byte) b, (byte) c);
      return a << 24 | b << 16 | c << 8 | d;
    }
  }

  /** Generic Decoder. */
  private static final class Generic extends TextDecoder {
    /** Input cache. */
    private final byte[] cache = new byte[4];
    /** Input buffer. */
    private final ByteBuffer inc = ByteBuffer.wrap(cache);
    /** Output buffer. */
    private final CharBuffer outc = CharBuffer.wrap(new char[4]);
    /** Charset decoder. */
    private final CharsetDecoder csd;

    /**
     * Constructor.
     * @param encoding encoding
     * @throws IOException I/O exception
     */
    private Generic(final String encoding) throws IOException {
      try {
        csd = Charset.forName(encoding).newDecoder();
      } catch(final Exception ex) {
        throw new DecodingException(ex);
      }
    }

    @Override
    int read(final TextInput ti) throws IOException {
      int c = -1;
      while(++c < 4) {
        final int a = ti.readByte();
        if(a < 0) break;

        cache[c] = (byte) a;
        outc.position(0);
        inc.position(0);
        inc.limit(c + 1);
        csd.reset();
        final CoderResult cr = csd.decode(inc, outc, true);
        if(cr.isMalformed()) continue;

        // return codepoint
        int i = 0;
        final int os = outc.position();
        for(int o = 0; o < os; ++o) i |= outc.get(o) << (o << 3);
        return i;
      }
      return c == 0 ? -1 : invalid(false, cache[0]);
    }
  }
}

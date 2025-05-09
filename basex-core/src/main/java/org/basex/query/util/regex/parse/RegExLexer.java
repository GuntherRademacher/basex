package org.basex.query.util.regex.parse;

import static org.basex.util.Token.*;

import java.util.regex.*;

import org.basex.util.*;
import org.basex.util.list.*;

/**
 * A simple lexer for XML Schema regular expressions.
 *
 * @author BaseX Team, BSD License
 * @author Leo Woerteler
 */
public final class RegExLexer implements TokenManager, RegExParserConstants {
  /** The input string. */
  private final byte[] input;
  /** Input position. */
  private int pos;
  /** Start-of-line marks, beginning with the second one. {@code null} if only one. */
  private final IntList lines;

  /** Number of nested character classes. */
  private int state;
  /** Flag for first character in a character class. */
  private byte first;
  /** Token builder. */
  private final TokenBuilder tb = new TokenBuilder();
  /** Token's payload. */
  private Object payload;
  /** Skip whitespace. */
  private final boolean skipWs;
  /** Skip comments. */
  private final boolean skipCmt;

  /** Unicode category regex. */
  private static final Pattern CAT_REGEX = Pattern.compile("^L[ultmo]?|M[nce]?|N[dlo]?" +
      "|P[cdseifo]?|Z[slp]?|S[mcko]?|C[cfon]?|Is[a-zA-Z\\d\\-]+$");

  /**
   * Constructor.
   * @param input input string
   * @param strip strip whitespace
   * @param comments allow and strip comments
   */
  public RegExLexer(final byte[] input, final boolean strip, final boolean comments) {
    this.input = input;
    IntList ls = null;
    final int il = input.length;
    for(int i = 0; i < il; i++) {
      final byte c = input[i];
      if(c == '\r' || c == '\n') {
        i++;
        if(c == '\r' && i < il && input[i] == '\n') i++;
        if(i < il) {
          if(ls == null) ls = new IntList();
          ls.add(i);
        }
      }
    }
    lines = ls;
    skipWs = strip;
    skipCmt = comments;
  }

  /**
   * Consumes the next code point in the input sequence.
   * @param skipComments whether comments are allowed
   * @return the code point
   */
  private int next(final boolean skipComments) {
    final int il = input.length;
    int cp;
    for(;;) {
      if(pos >= il) return -1;
      cp = cp(input, pos);
      pos += cl(input, pos);
      if(state <= 0) {
        if(skipWs && ws(cp)) continue;
        if(skipComments && cp == '#') {
          do {
            if(pos >= il) return -1;
            cp = cp(input, pos);
            pos += cl(input, pos);
          } while(cp != '#');
          continue;
        }
      }
      break;
    }
    tb.add(cp);
    return cp;
  }

  /**
   * Default state.
   * @return token kind
   */
  private int normal() {
    final int curr = next(skipCmt);
    if(curr == -1) return 0;
    switch(curr) {
      case '^':  return LINE_START;
      case '$':  return LINE_END;
      case '?':  return Q_MARK;
      case '*':  return STAR;
      case '+':  return PLUS;
      case '.':  return WILDCARD;
      case '|':  return OR;
      case '(':
        final int p = pos, ts = tb.size();
        switch(next(skipCmt)) {
          case '?':
            switch(next(skipCmt)) {
              case ':': return NPAR_OPEN;
              case '=': return POS_LOOKAHEAD;
              case '!': return NEG_LOOKAHEAD;
              case '<':
                switch(next(skipCmt)) {
                  case '=': return POS_LOOKBEHIND;
                  case '!': return NEG_LOOKBEHIND;
                }
            }
            break;
          case '*':
            Boolean neg = null;
            switch(next(skipCmt)) {
              case 'p': if(matches("ositive_look")) neg = false; break;
              case 'n': if(matches("egative_look")) neg = true; break;
            }
            if(neg != null) {
              switch(next(skipCmt)) {
                case 'a': if(matches("head:")) return neg ? NEG_LOOKAHEAD : POS_LOOKAHEAD; break;
                case 'b': if(matches("ehind:")) return neg ? NEG_LOOKBEHIND : POS_LOOKBEHIND; break;
              }
            }
        }
        pos = p;
        tb.size(ts);
        return PAR_OPEN;
      case ')':  return PAR_CLOSE;
      case '{':
        state = -1;
        return QUANT_OPEN;
      case '\\': return escape();
      case '[':
        state++;
        first = 1;
        return BR_OPEN;
      case ']':
      case '}':
        throw error("", curr);
    }
    if(curr >= '0' && curr <= '9') return DIGIT;
    return CHAR;
  }

  /**
   * Match a string against the input.
   * @param string the string to be matched
   * @return true if the input matches the string
   */
  private boolean matches(final String string) {
    for(final char c : string.toCharArray()) if(c != next(skipCmt)) return false;
    return true;
  }

  /**
   * Escape sequence.
   * @return token kind
   */
  private int escape() {
    final int curr = next(false);
    switch(curr) {
      case 'b':
      case 'B':
        return WORD_BOUNDARY;
      case 'n':
      case 'r':
      case 't':
      case '\\':
      case '|':
      case '.':
      case '?':
      case '*':
      case '+':
      case '(':
      case ')':
      case '{':
      case '}':
      case '$':
      case '-':
      case '[':
      case ']':
      case '^':
      case '#':
        return SINGLE_ESC;
      case 's':
      case 'S':
      case 'i':
      case 'I':
      case 'c':
      case 'C':
      case 'd':
      case 'D':
      case 'w':
      case 'W':
        return MULTI_ESC;
      case 'p':
      case 'P':
        final String p = "\\" + (char) curr;
        final int nxt = next(skipCmt);
        if(nxt != '{') throw error(p, nxt);
        for(int cp = next(skipCmt); cp != '}'; cp = next(skipCmt))
          if(cp == -1) throw error(tb.toString(), -1);
        final String in = tb.toString().substring(3, tb.size() - 1);
        final Matcher m = CAT_REGEX.matcher(in);
        if(!m.matches()) throw error("{", nxt);
        payload = in;
        return CAT_ESC;
      default:
        if(curr < '0' || curr > '9') throw error("\\", (char) curr);
        return BACK_REF;
    }
  }

  /**
   * Creates an error with line and column information.
   * @param before description of preceding token
   * @param curr current character
   * @return error
   */
  private TokenMgrError error(final String before, final int curr) {
    final int[] lc = lineCol(pos);
    return new TokenMgrError(curr < 0, lc[1], lc[3],
        before, (char) curr, TokenMgrError.LEXICAL_ERROR);
  }

  /**
   * Character-class state.
   * @return token kind
   */
  private int inClass() {
    final byte fst = first;
    first = 0;
    final int curr = next(skipCmt);
    switch(curr) {
      case '\\': return escape();
      case '^':
        if(fst == 1) {
          first = -1;
          return NEG;
        }
        return CHAR;
      case '[':
        state++;
        first = 1;
        return BR_OPEN;
      case ']':
        state--;
        return BR_CLOSE;
      case '-':
        return pos < input.length && input[pos] == '[' ? TO : CHAR;
    }
    return CHAR;
  }

  /**
   * Quantifier state.
   * @return token kind
   */
  private int inQuantifier() {
    final int curr = next(skipCmt);
    if(curr == ',') return COMMA;
    if(curr == '}') {
      state = 0;
      return QUANT_CLOSE;
    }
    if(curr < '0' || curr > '9') throw error("{", curr);
    final int il = input.length;
    while(pos < il) {
      final byte b = input[pos];
      if(b < '0' || b > '9') break;
      tb.add(b);
      pos++;
    }
    payload = tb.toString();
    return NUMBER;
  }

  /**
   * Calculates line and column position of the current match.
   * @param start start position
   * @return line and column positions
   */
  private int[] lineCol(final int start) {
    final int[] lc = new int[4];
    if(lines != null) {
      final int ls = lines.size();
      int ln = 0, st = 0;
      for(int i = 0; i < 2; i++) {
        final int curr = i == 0 ? start : pos;
        while(ln < ls && lines.get(ln) < curr) {
          st = lines.get(ln);
          ln++;
        }
        lc[i] = ln + 1;
        lc[i + 2] = curr - st + 1;
      }
    } else {
      lc[0] = lc[1] = 1;
      lc[2] = start;
      lc[3] = pos;
    }
    return lc;
  }

  @Override
  public Token getNextToken() {
    if(pos >= input.length) return new Token(EOF);
    final int start = pos;
    payload = null;
    final int type = state > 0 ? inClass() : state < 0 ? inQuantifier() : normal();
    final Token tok = new RegExToken(type, start, tb.toString());
    tb.size(0);
    return tok;
  }

  /**
   * Parser token.
   * @author Leo Woerteler
   */
  class RegExToken extends Token {
    /** the token's payload. */
    private final Object obj = payload;
    /**
     * Constructor.
     * @param k token kind
     * @param start start of the token
     * @param img image string
     */
    RegExToken(final int k, final int start, final String img) {
      kind = k;
      image = img;
      final int[] lc = lineCol(start);
      beginLine = lc[0];
      endLine = lc[1];
      beginColumn = lc[2];
      endColumn = lc[3];
    }

    @Override
    public Object getValue() {
      return obj;
    }
  }
}

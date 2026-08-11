package org.basex.gui.text;

import static org.basex.util.Token.*;

import java.util.*;

import org.basex.util.list.*;

/**
 * Fold ranges for read-only serialized XML text.
 *
 * @author BaseX Team, BSD License
 * @author Christian Gruen
 */
final class TextFolds {
  /** Fold start line positions. */
  private final IntList starts = new IntList();
  /** First hidden text positions. */
  private final IntList hideStarts = new IntList();
  /** First visible text positions after hidden ranges. */
  private final IntList hideEnds = new IntList();
  /** Collapsed fold start line positions. */
  private final HashSet<Integer> collapsed = new HashSet<>();

  /** Text the ranges were built for. */
  private byte[] text;
  /** Fold detection flag. */
  private boolean enabled;

  /**
   * Refreshes the fold ranges.
   * @param txt text
   * @param enable enable fold detection
   * @return {@code true} if the ranges have changed
   */
  boolean refresh(final byte[] txt, final boolean enable) {
    if(text == txt && enabled == enable) return false;
    text = txt;
    enabled = enable;
    starts.reset();
    hideStarts.reset();
    hideEnds.reset();
    collapsed.clear();
    if(enable) build(txt);
    return true;
  }

  /**
   * Clears all fold ranges.
   */
  void reset() {
    text = null;
    starts.reset();
    hideStarts.reset();
    hideEnds.reset();
    collapsed.clear();
  }

  /**
   * Indicates if fold markers can be rendered.
   * @return result of check
   */
  boolean visible() {
    return enabled && !starts.isEmpty();
  }

  /**
   * Toggles the fold at the specified line start.
   * @param start line start
   * @return {@code true} if a fold was toggled
   */
  boolean toggle(final int start) {
    if(index(start) == -1) return false;
    if(!collapsed.remove(start)) collapsed.add(start);
    return true;
  }

  /**
   * Checks if the specified line starts a fold.
   * @param start line start
   * @return result of check
   */
  boolean starts(final int start) {
    return index(start) != -1;
  }

  /**
   * Checks if the fold at the specified line start is collapsed.
   * @param start line start
   * @return result of check
   */
  boolean collapsed(final int start) {
    return collapsed.contains(start);
  }

  /**
   * Returns the first hidden position of the specified collapsed fold.
   * @param start line start
   * @return first hidden position, or {@code -1}
   */
  int hideStart(final int start) {
    final int i = index(start);
    return i != -1 && collapsed(start) ? hideStarts.get(i) : -1;
  }

  /**
   * Returns the first visible position after the specified collapsed fold.
   * @param start line start
   * @return first visible position, or {@code -1}
   */
  int hideEnd(final int start) {
    final int i = index(start);
    return i != -1 && collapsed(start) ? hideEnds.get(i) : -1;
  }

  /**
   * Returns the first visible position after a collapsed range with the specified hidden start.
   * @param hideStart first hidden position
   * @return first visible position, or {@code -1}
   */
  int hideEndAt(final int hideStart) {
    for(int i = 0, is = hideStarts.size(); i < is; i++) {
      if(hideStarts.get(i) == hideStart && collapsed(starts.get(i))) return hideEnds.get(i);
    }
    return -1;
  }

  /**
   * Builds XML fold ranges.
   * @param txt text
   */
  private void build(final byte[] txt) {
    final ArrayList<Open> stack = new ArrayList<>();
    final int tl = txt.length;
    for(int p = 0, lineStart = 0; p < tl;) {
      final byte b = txt[p];
      if(b == '\n') {
        lineStart = p + 1;
        p++;
        continue;
      }
      if(b != '<') {
        p++;
        continue;
      }

      final int tagLineStart = lineStart, close = tagEnd(txt, p);
      if(close == -1) break;
      final int ns = nameStart(txt, p, close);
      if(ns != -1) {
        final boolean end = txt[p + 1] == '/';
        final int ne = nameEnd(txt, ns, close);
        final String name = string(txt, ns, ne - ns);
        if(end) {
          close(txt, name, close, stack);
        } else if(!selfClosed(txt, close)) {
          final int hideStart = afterLine(txt, close);
          if(hideStart != -1) stack.add(new Open(name, tagLineStart, hideStart));
        }
      }

      for(int t = p; t <= close; t++) {
        if(txt[t] == '\n') lineStart = t + 1;
      }
      p = close + 1;
    }
  }

  /**
   * Closes the matching start tag.
   * @param txt text
   * @param name tag name
   * @param close end of the closing tag
   * @param stack stack of open tags
   */
  private void close(final byte[] txt, final String name, final int close,
      final ArrayList<Open> stack) {
    for(int s = stack.size() - 1; s >= 0; s--) {
      final Open open = stack.get(s);
      if(!open.name.equals(name)) continue;
      while(stack.size() > s) stack.remove(stack.size() - 1);
      final int hideEnd = afterLine(txt, close);
      if(hideEnd != -1 && open.hideStart < hideEnd) {
        starts.add(open.start);
        hideStarts.add(open.hideStart);
        hideEnds.add(hideEnd);
      }
      return;
    }
  }

  /**
   * Returns the end of a tag, respecting quoted attribute values.
   * @param txt text
   * @param start start position
   * @return end position, or {@code -1}
   */
  private static int tagEnd(final byte[] txt, final int start) {
    int quote = 0;
    for(int p = start + 1, tl = txt.length; p < tl; p++) {
      final int ch = txt[p];
      if(quote != 0) {
        if(ch == quote) quote = 0;
      } else if(ch == '"' || ch == '\'') {
        quote = ch;
      } else if(ch == '>') {
        return p;
      }
    }
    return -1;
  }

  /**
   * Returns the start of a tag name.
   * @param txt text
   * @param start start of the tag
   * @param close end of the tag
   * @return name start, or {@code -1}
   */
  private static int nameStart(final byte[] txt, final int start, final int close) {
    final int p = start + 1;
    if(p >= close) return -1;
    final byte ch = txt[p];
    if(ch == '!' || ch == '?') return -1;
    final int s = ch == '/' ? p + 1 : p;
    return s < close && nameChar(txt[s]) ? s : -1;
  }

  /**
   * Returns the end of a tag name.
   * @param txt text
   * @param start name start
   * @param close end of the tag
   * @return name end
   */
  private static int nameEnd(final byte[] txt, final int start, final int close) {
    int p = start;
    while(p < close && nameChar(txt[p])) p++;
    return p;
  }

  /**
   * Checks if a tag is self-closed.
   * @param txt text
   * @param close end of the tag
   * @return result of check
   */
  private static boolean selfClosed(final byte[] txt, final int close) {
    int p = close - 1;
    while(p >= 0 && txt[p] <= ' ') p--;
    return p >= 0 && txt[p] == '/';
  }

  /**
   * Returns the first position after the line containing the specified position.
   * @param txt text
   * @param pos position
   * @return position after the newline, or {@code -1}
   */
  private static int afterLine(final byte[] txt, final int pos) {
    for(int p = pos, tl = txt.length; p < tl; p++) {
      if(txt[p] == '\n') return p + 1;
    }
    return -1;
  }

  /**
   * Checks if a byte can occur in a tag name.
   * @param ch character
   * @return result of check
   */
  private static boolean nameChar(final byte ch) {
    return ch >= 'A' && ch <= 'Z' || ch >= 'a' && ch <= 'z' || ch >= '0' && ch <= '9' ||
      ch == '_' || ch == '-' || ch == '.' || ch == ':';
  }

  /**
   * Returns the index of the fold starting at the specified line.
   * @param start line start
   * @return index, or {@code -1}
   */
  private int index(final int start) {
    for(int i = 0, is = starts.size(); i < is; i++) {
      if(starts.get(i) == start) return i;
    }
    return -1;
  }

  /** Open tag. */
  private record Open(String name, int start, int hideStart) { }
}

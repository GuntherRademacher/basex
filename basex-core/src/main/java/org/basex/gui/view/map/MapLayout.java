package org.basex.gui.view.map;

import org.basex.data.*;
import org.basex.gui.*;
import org.basex.gui.view.*;

/**
 * Defines shared things of TreeMap layout algorithms.
 *
 * @author BaseX Team, BSD License
 * @author Joerg Hauser
 */
final class MapLayout {
  /** List of rectangles. */
  final MapRects rectangles = new MapRects();
  /** Font size. */
  private final int size;
  /** Data reference. */
  private final Data data;
  /** Map algorithm to use in this layout. */
  private final MapAlgo algo;
  /** Text lengths. */
  private final int[] textLen;
  /** GUI options. */
  private final GUIOptions gopts;

  /** Layout rectangle. */
  final MapRect layout;

  /**
   * Constructor.
   * @param data data reference to use in this layout
   * @param textLen text lengths array
   * @param gopts gui options
   */
  MapLayout(final Data data, final int[] textLen, final GUIOptions gopts) {
    this.data = data;
    this.textLen = textLen;
    this.gopts = gopts;
    size = GUIConstants.fontSize + 4;

    layout = switch(gopts.get(GUIOptions.MAPOFFSETS)) {
      // no title, small border
      case 1  -> new MapRect(0, 2, 0, 2);
      // title, no border
      case 2  -> new MapRect(0, size, 0, size);
      // title, border
      case 3  -> new MapRect(2, size - 1, 4, size + 1);
      // title, large border
      case 4  -> new MapRect(size >> 2, size, size >> 1, size + (size >> 2));
      // no title, no border
      default -> new MapRect(0, 0, 0, 0);
    };
    // select method to construct this treemap
    // may should be placed in makeMap to define different method for different levels
    algo = switch(gopts.get(GUIOptions.MAPALGO)) {
      case 1  -> new StripAlgo();
      case 2  -> new SquarifiedAlgo();
      case 3  -> new SliceDiceAlgo();
      case 4  -> new BinaryAlgo();
      default -> new SplitAlgo();
    };
  }

  /**
   * Returns all children of the specified node.
   * @param par parent node
   * @return children
   */
  private MapList children(final int par) {
    final MapList list = new MapList();
    final int last = par + ViewData.size(data, par);
    final boolean atts = gopts.get(GUIOptions.MAPATTS);
    int p = par + (atts ? 1 : data.attSize(par, data.kind(par)));
    while(p < last) {
      list.add(p);
      p += ViewData.size(data, p);
    }
    return list;
  }

  /**
   * Recursively splits rectangles.
   * @param r parent rectangle
   * @param l children array
   * @param ns start array position
   * @param ne end array position
   */
  void makeMap(final MapRect r, final MapList l, final int ns, final int ne) {
    if(ne - ns == 0) {
      // one rectangle left, add it and go deeper
      r.pre = l.get(ns);
      putRect(r);
    } else {
      int nn = 0;
      if(r.level == 0) {
        final int is = l.size();
        for(int i = 0; i < is; ++i) nn += ViewData.size(data, l.get(i));
      } else {
        nn = l.get(ne) - l.get(ns) + ViewData.size(data, l.get(ne));
      }
      l.initWeights(textLen, nn, data, gopts.get(GUIOptions.MAPWEIGHT));

      // call recursion for next deeper levels
      final MapRects rects = algo.calcMap(r, l, ns, ne);
      for(final MapRect rect : rects) {
        if(rect.x + rect.w <= r.x + r.w && rect.y + rect.h <= r.y + r.h)
          putRect(rect);
      }
    }
  }

  /**
   * One rectangle left, add it and continue with its children.
   * @param r parent rectangle
   */
  private void putRect(final MapRect r) {
    // position, with and height calculated using sizes of former level
    final int x = r.x + layout.x;
    final int y = r.y + layout.y;
    final int w = r.w - layout.w;
    final int h = r.h - layout.h;

    // skip too small rectangles and metadata in file systems
    if(w < size && h < size || w <= 2 || h <= 2) {
      rectangles.add(r);
      return;
    }

    rectangles.add(r);
    final MapList ch = children(r.pre);
    final int cs = ch.size();
    if(cs != 0) {
      makeMap(new MapRect(x, y, w, h, r.pre, r.level + 1), ch, 0, cs - 1);
    }
  }
}

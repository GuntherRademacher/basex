package org.basex.gui;

import static org.basex.gui.GUIMenuCmd.*;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import org.basex.core.*;
import org.basex.gui.layout.*;
import org.basex.gui.view.*;
import org.basex.gui.view.map.*;
import org.basex.util.*;
import org.basex.util.hash.*;
import org.basex.util.list.*;

/**
 * GUI Constants used in different views.
 *
 * To add a new view, please proceed as follows:
 *
 * All views have unique names, which are defined below.
 * The following steps are necessary to add a new view
 * (the implementation of the existing views might help you):
 *
 * <ul>
 *  <li> define a unique name for your view (e.g. {@code map})</li>
 *  <li> add a string for your view, as shown below</li>
 *  <li> add the string in the {@link #VIEWS} string below</li>
 *  <li> create your view implementation in a new sub package
 *    (e.g. {@link MapView}).
 *  <li> add a new {@link View} instance in the {@link GUI} constructor.</li>
 * </ul>
 *
 * Add some more code to allow switching on/off your view:
 *
 * <ul>
 *  <li> add a boolean visibility flag with the view name included
 *    in the {@link GUIOptions} class {@link GUIOptions#SHOWMAP})</li>
 *  <li> add strings for the menu text.</li>
 *  <li> optionally add localized translations in the .lang files
 *    (e.g. {@code c_showmap} and {@code c_showmaptt})</li>
 *  <li> add a corresponding command in the {@link GUIMenuCmd} class
 *   (e.g. {@link GUIMenuCmd#C_SHOW_MAP})and add a reference in the
 *   {@link #MENUITEMS} menu structure</li>
 * </ul>
 *
 * @author BaseX Team, BSD License
 * @author Christian Gruen
 */
public final class GUIConstants {

  // VIEW NAMES ===================================================================================

  /** Internal name of the Map View. */
  public static final String MAPVIEW = "map";
  /** Internal name of the Tree View. */
  public static final String FOLDERVIEW = "folder";
  /** Internal name of the Text View. */
  public static final String TEXTVIEW = "text";
  /** Internal name of the Table View. */
  public static final String TABLEVIEW = "table";
  /** Internal name of the Info View. */
  public static final String INFOVIEW = "info";
  /** Internal name of the Explore View. */
  public static final String EXPLOREVIEW = "explore";
  /** Internal name of the Plot View. */
  public static final String PLOTVIEW = "plot";
  /** Internal name of the Tree View. */
  public static final String TREEVIEW = "tree";
  /** Internal name of the Editor View. */
  public static final String EDITORVIEW = "editor";

  /**
   * Default GUI Layout. The layout is formatted as follows:
   * The character 'H' or 'V' adds a new horizontal or vertical level,
   * and a level is closed again with the '-' character. All views are
   * separated with spaces, and all views must be specified in this layout.
   * This layout is displayed as soon as a database is opened.
   */
  public static final String VIEWS = "V H " + EDITORVIEW + ' ' + FOLDERVIEW +
    ' ' + MAPVIEW + ' ' + PLOTVIEW + ' ' + " - H " + TEXTVIEW + ' ' + INFOVIEW +
    ' ' + TABLEVIEW + ' ' + TREEVIEW + ' ' + EXPLOREVIEW + " - -";

  // TOOLBAR ======================================================================================

  /** Toolbar entries, containing the button commands. */
  static final GUIMenuCmd[] TOOLBAR = {
    C_CREATE, C_OPEN_MANAGE, C_PROPERTIES, C_CLOSE, null,
    C_SHOW_EDITOR, C_SHOW_RESULT, C_INDENT_RESULT, C_SHOW_INFO, null,
    C_SHOW_MAP, C_SHOW_TREE, C_SHOW_FOLDER, C_SHOW_PLOT, C_SHOW_TABLE, C_SHOW_EXPLORE, null,
    C_GO_HOME, C_GO_BACK, C_GO_UP, C_GO_FORWARD, C_FILTER_NODES, null,
    C_PREFERENCES
  };

  // MENUBARS =====================================================================================

  /** Top menu entries. */
  static final String[] MENUBAR = {
    Text.DATABASE, Text.EDITOR, Text.VIEW, Text.VISUALIZATION, Text.OPTIONS, Text.HELP
  };

  /**
   * Two-dimensional menu entries, containing the menu item commands.
   * {@link GUIPopupCmd#SEPARATOR} references serve as menu separators.
   */
  static final GUICommand[][] MENUITEMS = { {
    C_CREATE, C_OPEN_MANAGE, SEPARATOR, C_PROPERTIES, C_EXPORT, C_CLOSE,
    Prop.MAC ? null : SEPARATOR, Prop.MAC ? null : C_EXIT
  }, {
    C_EDIT_NEW, C_EDIT_OPEN, C_EDIT_REOPEN, C_EDIT_SAVE, C_EDIT_SAVE_AS, C_EDIT_CLOSE,
    C_EDIT_CLOSE_ALL, SEPARATOR,
    C_GO, C_STOP, C_INDENT_RESULT, C_EXTERNAL_VARIABLES, SEPARATOR,
    C_FORMAT, C_COMMENT, C_SORT, SEPARATOR,
    C_LOWER_CASE, C_UPPER_CASE, C_TITLE_CASE, SEPARATOR,
    C_JUMP_TO_BRACKET, C_JUMP_TO_FILE, C_NEXT_ERROR, SEPARATOR
  }, {
    C_SHOW_EDITOR, C_SHOW_PROJECT, C_FIND_CONTENTS, SEPARATOR,
    C_SHOW_RESULT, C_SHOW_INFO, SEPARATOR,
    C_SHOW_BUTTONS, C_SHOW_INPUT_BAR, C_SHOW_STATUS_BAR, Prop.MAC ? null : C_FULLSCREEN
  }, {
    C_SHOW_MAP, C_SHOW_TREE, C_SHOW_FOLDER, C_SHOW_PLOT, C_SHOW_TABLE, C_SHOW_EXPLORE,
  }, {
    C_INDENT_RESULT, SEPARATOR,
    C_RT_EXECUTION, C_RT_FILTERING, SEPARATOR,
    C_PACKAGES, Prop.MAC ? null : SEPARATOR,
    C_COLOR, C_FONTS, Prop.MAC ? null : C_PREFERENCES
  }, {
    C_HELP, SEPARATOR, C_COMMUNITY, C_CHECK_FOR_UPDATES, SEPARATOR,
    C_SHOW_MEM, Prop.MAC ? null : C_ABOUT
  }};

  /** Context menu entries. */
  public static final GUIMenuCmd[] POPUP = {
    C_GO_BACK, C_GO_UP, C_GO_FORWARD, C_FILTER_NODES, null,
    C_COPY_NODES, C_PASTE_NODES, C_DELETE_NODES, C_NEW_NODES, C_EDIT_NODES, null,
    C_COPY_PATH
  };

  // CURSORS ======================================================================================

  /** Arrow cursor. */
  public static final Cursor CURSORARROW = new Cursor(Cursor.DEFAULT_CURSOR);
  /** Hand cursor. */
  public static final Cursor CURSORHAND = new Cursor(Cursor.HAND_CURSOR);
  /** Wait cursor. */
  public static final Cursor CURSORWAIT = new Cursor(Cursor.WAIT_CURSOR);
  /** Left/Right arrow cursor. */
  public static final Cursor CURSORMOVEH = new Cursor(Cursor.E_RESIZE_CURSOR);
  /** Move cursor. */
  public static final Cursor CURSORMOVEV = new Cursor(Cursor.N_RESIZE_CURSOR);
  /** Text cursor. */
  public static final Cursor CURSORTEXT = new Cursor(Cursor.TEXT_CURSOR);
  /** Move cursor. */
  public static final Cursor CURSORMOVE = new Cursor(Cursor.MOVE_CURSOR);

  /** Icon type. */
  public enum Msg {
    /** Warning message. */
    WARN("warn", "warning"),
    /** Error message. */
    ERROR("error", "error"),
    /** Success message. */
    SUCCESS("ok", "information"),
    /** Question message. */
    QUESTION("warn", "question"),
    /** Yes/no/cancel message. */
    YESNOCANCEL("warn", "question");

    /** Small icon. */
    public final Icon small;
    /** Large icon. */
    public final Icon large;

    /**
     * Constructor.
     * @param s small icon
     * @param l large icon
     */
    Msg(final String s, final String l) {
      small = BaseXImages.icon(s);
      large = UIManager.getIcon("OptionPane." + l + "Icon");
    }
  }

  // DUMMY OBJECTS ============================================================

  /** Dummy text field. */
  private static final JTextField TEXTFIELD = new JTextField();
  /** Dummy label, used for size calculations. */
  private static final JLabel LABEL = new JLabel();

  // COLORS =======================================================================================

  /** Background color. */
  public static final Color BACK = new Color(TEXTFIELD.getBackground().getRGB());
  /** Text color. */
  public static final Color TEXT = new Color(TEXTFIELD.getForeground().getRGB());
  /** Panel color. */
  public static final Color PANEL = new Color(LABEL.getBackground().getRGB());

  /** Dark theme. */
  private static final boolean INVERT = BACK.getRed() + BACK.getGreen() + BACK.getBlue() < 384;

  /** Color: red. */
  public static final Color RED = color(224, 0, 0);
  /** Color: light red. */
  public static final Color LRED = color(255, 216, 216);
  /** Color: green. */
  public static final Color GREEN = color(32, 160, 32);
  /** Color: cyan. */
  public static final Color BLUE = color(32, 96, 192);
  /** Color: purple. */
  public static final Color PURPLE = color(160, 32, 160);
  /** Color: gray. */
  public static final Color GRAY = color(0, 160, 160);
  /** Color: dark gray. */
  public static final Color DGRAY = color(112, 112, 112);

  /** Cell color. */
  public static Color lgray;
  /** Button color. */
  public static Color gray;
  /** Middle gray color. */
  public static Color mgray;
  /** Background color. */
  public static Color dgray;

  /** Cached color gradient. */
  private static final Color[] COLORS = new Color[100];

  /** Second bright GUI color. */
  public static Color color1;
  /** Middle color. */
  public static Color color2;
  /** Middle color. */
  public static Color color3;
  /** Dark color. */
  public static Color color4;

  /** Mark color. */
  public static Color colormark1;
  /** Second mark color. */
  public static Color colormark2;
  /** Third mark color. */
  public static Color colormark3;
  /** Fourth mark color. */
  public static Color colormark4;

  /** Alpha color. */
  public static Color color1A;
  /** Transparent background color. */
  public static Color color2A;
  /** Cursor background color. */
  public static Color color3A;
  /** Mark color, custom alpha value. */
  public static Color colormark1A;
  /** Second mark color, custom alpha value. */
  public static Color colormark2A;

  // FONTS ========================================================================================

  /** Name of monospace font. */
  public static final String MONOFONT = Prop.WIN ? "Consolas" : Font.MONOSPACED;
  /** Font. */
  public static Font font;
  /** Bold Font. */
  public static Font bfont;
  /** Monospace font. */
  public static Font mfont;
  /** Default monospace font. */
  public static Font dmfont;
  /** Current font size. */
  public static int fontSize;

  /** Names of available fonts. */
  private static String[] fonts;
  /** Characters for monospace detection. */
  private static final char[] MONOSPACE =  " !,-.01:<ILMWilmw".toCharArray();

  /**
   * Returns the names of all fonts that are available on the system.
   * @return fonts
   */
  public static String[] fonts() {
    if(fonts == null) {
      final StringList list = new StringList();
      for(final String name : GraphicsEnvironment.getLocalGraphicsEnvironment().
          getAvailableFontFamilyNames()) {
        boolean add = true;
        final Font f = new Font(name, Font.PLAIN, 100);
        for(final char ch : MONOSPACE) {
          if(!f.canDisplay(ch)) {
            add = false;
            break;
          }
        }
        if(add) list.add(name);
      }
      fonts = list.finish();
    }
    return fonts;
  }

  /**
   * Returns the names of all monospace fonts that are available on the system.
   * @return fonts
   */
  public static String[] monoFonts() {
    final StringList monos = new StringList();
    for(final String name : fonts()) {
      if(isMono(LABEL.getFontMetrics(new Font(name, Font.PLAIN, 100)))) monos.add(name);
    }
    return monos.finish();
  }

  /**
   * Checks if the font metrics refer to a monospaced font.
   * Slight deviations are tolerated.
   * @param fm font metrics
   * @return result of check
   */
  public static boolean isMono(final FontMetrics fm) {
    final IntSet set = new IntSet(4);
    for(final char ch : MONOSPACE) {
      final int w = fm.charWidth(ch);
      if(w == 0 || set.add(w) && set.size() > 2) return false;
    }
    return true;
  }

  // KEYS =========================================================================================

  /** No modifier. */
  public static final int NO_MOD = 0;
  /** Shift key. */
  public static final int SHIFT = InputEvent.SHIFT_DOWN_MASK;
  /** Alt key. */
  public static final int ALT = InputEvent.ALT_DOWN_MASK;
  /** Control key. */
  public static final int CTRL = InputEvent.CTRL_DOWN_MASK;
  /** Shortcut key (CTRL/META). */
  public static final int META = Prop.MAC ? InputEvent.META_DOWN_MASK :
    InputEvent.CTRL_DOWN_MASK;

  // INITIALIZATION ===============================================================================

  /** Private constructor, preventing class instantiation. */
  private GUIConstants() { }

  /**
   * Initializes UI settings.
   * @param opts gui options
   */
  public static synchronized void init(final GUIOptions opts) {
    lgray = color(224, 224, 224);
    gray = color(160, 160, 160);
    mgray = color(128, 128, 128);
    dgray = color(64, 64, 64);

    // create color array
    final int r = opts.get(GUIOptions.COLORRED);
    final int g = opts.get(GUIOptions.COLORGREEN);
    final int b = opts.get(GUIOptions.COLORBLUE);
    final int cl = COLORS.length;
    for(int c = 1; c < cl + 1; ++c) {
      COLORS[c - 1] = color(Math.max(255 - c * r, 0),
        Math.max(255 - c * g, 0), Math.max(255 - c * b, 0));
    }

    color1 = color(darker(r, 24), darker(g, 25), darker(b, 40));
    color2 = color(darker(r, 32), darker(g, 32), darker(b, 44));
    color3 = color(darker(r, 48), darker(g, 50), darker(b, 40));
    color4 = color(darker(r, 140), darker(g, 100), darker(b, 70));
    colormark1 = color(darker(r, 16), darker(g, 120), darker(b, 240));
    colormark2 = color(darker(r, 16), darker(g, 80), darker(b, 160));
    colormark3 = color(darker(r, 32), darker(g, 160), darker(b, 320));
    colormark4 = color(darker(r, 1), darker(g, 40), darker(b, 80));

    final Color col = COLORS[16];
    color1A = color(darker(r, 110), darker(g, 150), darker(b, 160), 100);
    color2A = color(col.getRed(), col.getGreen(), col.getBlue(), 50);
    color3A = color(col.getRed(), col.getGreen(), col.getBlue(), 30);
    colormark1A = color(darker(r, 32), darker(g, 160), darker(b, 320), 100);
    colormark2A = color(darker(r, 12), darker(g, 60), darker(b, 120), 100);

    final String name = opts.get(GUIOptions.FONT);

    fontSize = opts.get(GUIOptions.FONTSIZE);
    font  = new Font(name, Font.PLAIN, fontSize);
    mfont = new Font(opts.get(GUIOptions.MONOFONT), Font.PLAIN, fontSize);
    bfont = new Font(name, Font.BOLD, fontSize);
    dmfont = new Font(opts.get(GUIOptions.MONOFONT), Font.PLAIN, LABEL.getFont().getSize());
  }

  /**
   * Returns the specified color from the color gradient.
   * @param i color index
   * @return color
   */
  public static Color color(final int i) {
    return COLORS[Math.min(COLORS.length - 1, i)];
  }

  // PRIVATE METHODS ==============================================================================

  /**
   * Combines the color value with specified factor and returns a new value.
   * @param c color
   * @param f factor
   * @return converted color value
   */
  private static int darker(final int c, final int f) {
    return Math.max(0, 255 - c * f / 10);
  }

  /**
   * Returns the specified color with the specified RGB values, or its inverted version
   * if {@link #INVERT} is true.
   * @param r red component
   * @param g green component
   * @param b blue component
   * @return converted color
   */
  private static Color color(final int r, final int g, final int b) {
    return INVERT ? new Color(255 - r, 255 - g, 255 - b) : new Color(r, g, b);
  }

  /**
   * Returns the specified color with the specified RGB and alpha values, or its inverted version
   * if {@link #INVERT} is true.
   * @param r red component
   * @param g green component
   * @param b blue component
   * @param a alpha component
   * @return converted color
   */
  private static Color color(final int r, final int g, final int b, final int a) {
    return INVERT ? new Color(255 - r, 255 - g, 255 - b, 255 - a) : new Color(r, g, b, a);
  }
}

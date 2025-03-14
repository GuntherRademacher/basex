package org.basex.gui.dialog;

import static org.basex.core.Text.*;

import java.awt.*;
import org.basex.core.*;
import org.basex.gui.*;
import org.basex.gui.layout.*;
import org.basex.gui.layout.BaseXFileChooser.Mode;
import org.basex.io.*;

/**
 * CSV parser panel.
 *
 * @author BaseX Team, BSD License
 * @author Christian Gruen
 */
final class DialogXmlParser extends DialogParser {
  /** Internal XML parsing. */
  private final BaseXCheckBox intparse;
  /** Whitespace stripping. */
  private final BaseXCheckBox stripWS;
  /** Namespace stripping. */
  private final BaseXCheckBox stripNS;
  /** DTD mode. */
  private final BaseXCheckBox dtd;
  /** Use XML Catalog. */
  private final BaseXCheckBox usecat;
  /** Use XInclude. */
  private final BaseXCheckBox xinclude;
  /** Catalog file. */
  private final BaseXTextField cfile;
  /** Browse Catalog file. */
  private final BaseXButton browsec;

  /**
   * Constructor.
   * @param dialog dialog reference
   * @param opts main options
   */
  DialogXmlParser(final BaseXDialog dialog, final MainOptions opts) {
    intparse = new BaseXCheckBox(dialog, INT_PARSER, MainOptions.INTPARSE, opts).bold();
    stripWS = new BaseXCheckBox(dialog, STRIP_WS, MainOptions.STRIPWS, opts).bold();
    stripNS = new BaseXCheckBox(dialog, STRIP_NS, MainOptions.STRIPNS, opts).bold();
    dtd = new BaseXCheckBox(dialog, PARSE_DTDS, MainOptions.DTD, opts).bold();
    xinclude = new BaseXCheckBox(dialog, USE_XINCLUDE, MainOptions.XINCLUDE, opts).bold();
    // catalog resolver
    final boolean cat = !opts.get(MainOptions.CATALOG).isEmpty();
    usecat = new BaseXCheckBox(dialog, USE_CATALOG_FILE, cat).bold();
    cfile = new BaseXTextField(dialog, opts.get(MainOptions.CATALOG));
    browsec = new BaseXButton(dialog, BROWSE_D);
    browsec.addActionListener(e -> {
      final GUIOptions gopts = dialog.gui().gopts;
      final BaseXFileChooser fc = new BaseXFileChooser(dialog, FILE_OR_DIR,
          gopts.get(GUIOptions.INPUTPATH)).filter(XML_DOCUMENTS, true, IO.XMLSUFFIX);

      final IO file = fc.select(Mode.FDOPEN);
      if(file != null) cfile.setText(file.path());
    });

    final BaseXBack pp = new BaseXBack(new RowLayout());
    pp.add(intparse);
    pp.add(new BaseXLabel(H_INT_PARSER, true, false));
    pp.add(stripWS);
    pp.add(new BaseXLabel(H_STRIP_WS, false, false).border(0, 0, 8, 0));
    pp.add(stripNS);
    pp.add(dtd);
    pp.add(xinclude);
    final BaseXBack p = new BaseXBack(new TableLayout(2, 2, 8, 0));
    p.add(usecat);
    p.add(new BaseXLabel());
    p.add(cfile);
    p.add(browsec);
    pp.add(p);

    add(pp, BorderLayout.WEST);
    action(true);
  }

  @Override
  boolean action(final boolean active) {
    final boolean ip = intparse.isSelected(), uc = usecat.isSelected();
    intparse.setEnabled(!uc);
    xinclude.setEnabled(!ip);
    usecat.setEnabled(!ip);
    cfile.setEnabled(uc);
    browsec.setEnabled(uc);
    return true;
  }

  @Override
  void update() {
  }

  @Override
  void setOptions(final GUI gui) {
    gui.set(MainOptions.INTPARSE, intparse.isSelected());
    gui.set(MainOptions.STRIPWS, stripWS.isSelected());
    gui.set(MainOptions.STRIPNS, stripNS.isSelected());
    gui.set(MainOptions.DTD, dtd.isSelected());
    gui.set(MainOptions.XINCLUDE, xinclude.isSelected());
    gui.set(MainOptions.CATALOG, usecat.isSelected() ? cfile.getText() : "");
  }
}

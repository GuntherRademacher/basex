package org.basex.tests.w3c;

import static org.basex.tests.w3c.QT3Constants.*;

import java.util.*;

import javax.xml.namespace.*;

import org.basex.core.*;
import org.basex.query.util.format.*;
import org.basex.query.value.item.*;
import org.basex.tests.bxapi.*;
import org.basex.tests.bxapi.xdm.*;
import org.basex.util.list.*;

/**
 * Driver environment for the {@link QT3TS} test suite driver.
 *
 * @author BaseX Team, BSD License
 * @author Christian Gruen
 */
final class QT3Env {
  /** Namespaces: prefix, URI. */
  final ArrayList<HashMap<String, String>> namespaces;
  /** Sources: role, file, validation, URI, xml:id. */
  final ArrayList<HashMap<String, String>> sources;
  /** Resources. */
  final ArrayList<HashMap<String, String>> resources;
  /** Parameters: name, as, select, declared. */
  final ArrayList<HashMap<String, String>> params;
  /** Schemas: URI, file, xml:id. */
  final HashMap<String, String> schemas;
  /** Collations: URI, default. */
  final HashMap<String, String> collations;
  /** Decimal Formats: decimal-separator, grouping-separator,
      digit, pattern-separator, infinity, NaN, per-mille,
      minus-sign, name, percent, zero-digit. */
  final HashMap<QName, DecFormatOptions> decFormats;
  /** Static Base URI. */
  final String baseURI;
  /** Name. */
  final String name;

  /** Collection URI. */
  final String collURI;
  /** Initial context item. */
  final XdmValue context;
  /** Collection context flag. */
  final boolean collContext;
  /** Collection sources. */
  final StringList collSources;

  /**
   * Constructor.
   * @param ctx database context
   * @param env environment item
   * @throws BaseXException database exception
   */
  QT3Env(final Context ctx, final XdmValue env) throws BaseXException {
    name = XQuery.string('@' + NNAME, env, ctx);
    sources = list(ctx, env, SOURCE);
    resources = list(ctx, env, RESOURCE);
    params = list(ctx, env, PARAM);
    namespaces = list(ctx, env, NAMESPACE);
    ArrayList<HashMap<String, String>> al = list(ctx, env, SCHEMA);
    schemas = al.isEmpty() ? null : al.get(0);
    al = list(ctx, env, COLLATION);
    collations = al.isEmpty() ? null : al.get(0);
    final String uri = string(STATIC_BASE_URI, ctx, env);
    baseURI = uri;

    // collections
    collURI = XQuery.string("*:collection/@uri", env, ctx);

    collContext = new XQuery("*:collection/*:source/@role = '.'", ctx).
        context(env).value().getBoolean();

    collSources = new StringList();
    for(final XdmItem iatt : new XQuery("*:collection/*:source/@file", ctx).context(env))
      collSources.add(iatt.getString());

    decFormats = new HashMap<>();
    for(final XdmItem item : new XQuery("*:decimal-format", ctx).context(env)) {
      final XdmValue value = new XQuery(
        "for $n in @name " +
        "let $b := substring-before($n, ':') " +
        "return QName(if($b) then namespace-uri-for-prefix($b, .) else '', $n)",
        ctx).context(item).value();
      final DecFormatOptions options = new DecFormatOptions();
      final QNm qnm = value.size() != 0 ? (QNm) value.internal() : QNm.EMPTY;
      decFormats.put(qnm.toJava(), options);
      for(final XdmItem it2 : new XQuery("@*[name() != 'name']", ctx).context(item)) {
        options.assign(it2.getName().getLocalPart(), it2.getString());
      }
    }

    final String c = XQuery.string("*:context-item/@select", env, ctx);
    context = c.isEmpty() ? null : new XQuery(c, ctx).value();
  }

  /**
   * Returns a list of all attributes of the specified element in a map.
   * @param ctx database context
   * @param env root element
   * @param elem element to be parsed
   * @return map list
   */
  static ArrayList<HashMap<String, String>> list(final Context ctx, final XdmValue env,
      final String elem) {

    final ArrayList<HashMap<String, String>> list = new ArrayList<>();
    for(final XdmItem item : new XQuery("*:" + elem, ctx).context(env)) {
      list.add(map(ctx, item));
    }
    return list;
  }

  /**
   * Returns all attributes of the specified element in a map.
   * @param ctx database context
   * @param env root element
   * @return map
   */
  static HashMap<String, String> map(final Context ctx, final XdmValue env) {
    final HashMap<String, String> map = new HashMap<>();
    for(final XdmItem item : new XQuery("@*", ctx).context(env)) {
      final QName qnm = item.getName();
      final String name = qnm.getLocalPart();
      String value = item.getString();
      if(name.equals("name") && value.contains(":") && !value.startsWith("Q{")) {
        value = new XQuery(
            "@Q{" + qnm.getNamespaceURI() + "}name!expanded-QName(resolve-QName(., ..))",
            ctx).context(env).value().getString();
      }
      map.put(name, value);
    }
    return map;
  }

  /**
   * Returns a single attribute string.
   * @param elm name of element
   * @param ctx database context
   * @param env root element
   * @return map
   */
  static String string(final String elm, final Context ctx, final XdmValue env) {
    final XdmItem item = new XQuery("*:" + elm, ctx).context(env).next();
    return item == null ? null :
      new XQuery("string(@*)", ctx).context(item).next().getString();
  }
}

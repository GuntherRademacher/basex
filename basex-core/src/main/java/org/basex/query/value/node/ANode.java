package org.basex.query.value.node;

import static org.basex.query.QueryError.*;
import static org.basex.query.util.DeepEqualOptions.*;
import static org.basex.query.value.type.NodeType.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import org.basex.api.dom.*;
import org.basex.core.*;
import org.basex.data.*;
import org.basex.io.out.*;
import org.basex.io.out.DataOutput;
import org.basex.io.serial.*;
import org.basex.query.*;
import org.basex.query.CompileContext.*;
import org.basex.query.expr.*;
import org.basex.query.iter.*;
import org.basex.query.util.*;
import org.basex.query.util.collation.*;
import org.basex.query.util.list.*;
import org.basex.query.value.item.*;
import org.basex.query.value.type.*;
import org.basex.util.*;

/**
 * Abstract node type.
 *
 * @author BaseX Team, BSD License
 * @author Christian Gruen
 */
public abstract class ANode extends Item {
  /** QName: xml:base. */
  static final QNm XML_BASE = new QNm(QueryText.BASE, QueryText.XML_URI);
  /** Node Types. */
  private static final NodeType[] TYPES = {
    DOCUMENT_NODE, ELEMENT, TEXT, ATTRIBUTE, COMMENT, PROCESSING_INSTRUCTION
  };
  /** Static node counter. */
  private static final AtomicInteger ID = new AtomicInteger();
  /** Unique node ID. ID can get negative, as subtraction of IDs is used for all comparisons. */
  public final int id = ID.incrementAndGet();

  /**
   * Constructor.
   * @param type item type
   */
  ANode(final NodeType type) {
    super(type);
  }

  @Override
  public final void write(final DataOutput out) throws IOException {
    final ArrayOutput ao = new ArrayOutput();
    Serializer.get(ao).serialize(this);
    out.writeToken(ao.finish());
  }

  @Override
  public boolean test(final QueryContext qc, final InputInfo ii, final long pos) {
    return true;
  }

  @Override
  public final boolean bool(final InputInfo ii) {
    return true;
  }

  @Override
  public final byte[] string(final InputInfo ii) {
    return string();
  }

  /**
   * Returns the string value.
   * @return string value
   */
  public abstract byte[] string();

  @Override
  public final Expr simplifyFor(final Simplify mode, final CompileContext cc)
      throws QueryException {
    Expr expr = this;
    if(mode == Simplify.STRING) {
      // boolean(<a>A</a>)  ->  boolean('A')
      expr = Str.get(string());
    } else if(mode.oneOf(Simplify.DATA, Simplify.NUMBER)) {
      // data(<a>A</a>)  ->  data(xs:untypedAtomic('A'))
      expr = Atm.get(string());
    }
    return cc.simplify(this, expr, mode);
  }

  @Override
  public final boolean comparable(final Item item) {
    return item.type.isStringOrUntyped();
  }

  @Override
  public final boolean equal(final Item item, final Collation coll, final InputInfo ii)
      throws QueryException {
    return item.type.isStringOrUntyped()
        ? Token.eq(string(), item.string(ii), Collation.get(coll, ii))
        : item.equal(this, coll, ii);
  }

  @Override
  public final int compare(final Item item, final Collation coll, final boolean transitive,
      final InputInfo ii) throws QueryException {
    return item.type.isStringOrUntyped()
        ? Token.compare(string(), item.string(ii), Collation.get(coll, ii))
        : -item.compare(this, coll, transitive, ii);
  }

  @Override
  public final boolean deepEqual(final Item item, final DeepEqual deep) throws QueryException {
    final Type type1 = type, type2 = item.type;
    if(type1 != type2) return false;
    final ANode node1 = this, node2 = (ANode) item;
    if(node1.is(node2)) return true;

    QNm name1 = node1.qname(), name2 = node2.qname();
    if(type1 == NAMESPACE_NODE) return name1.eq(name2) && Token.eq(node1.string(), node2.string());

    // compare names
    final DeepEqualOptions options = deep.options;
    if(name1 != null && (!name1.eq(name2) ||
        options.get(NAMESPACE_PREFIXES) && !Token.eq(name1.prefix(), name2.prefix())
    )) return false;
    // compare values
    if(type1.oneOf(TEXT, COMMENT, PROCESSING_INSTRUCTION, ATTRIBUTE) &&
        !Token.eq(node1.string(), node2.string(), deep)) return false;
    // compare base URIs
    if(options.get(BASE_URI)) {
      if(deep.nested) return Token.eq(node1.baseURI(), node2.baseURI());
      final Uri uri1 = node1.baseURI(Uri.EMPTY, true, deep.info);
      final Uri uri2 = node2.baseURI(Uri.EMPTY, true, deep.info);
      if(!uri1.eq(uri2)) return false;
    }
    if(type1 == ELEMENT) {
      // compare attributes
      final BasicNodeIter iter1 = node1.attributeIter();
      BasicNodeIter iter2 = node2.attributeIter();
      if(iter1.size() != iter2.size()) return false;

      for(ANode attr1; (attr1 = iter1.next()) != null;) {
        name1 = attr1.qname();
        for(ANode attr2;;) {
          attr2 = iter2.next();
          if(attr2 == null) return false;
          name2 = attr2.qname();
          if(name1.eq(name2)) {
            final Bln eq = deep.itemsEqual(attr1, attr2);
            if(eq == Bln.TRUE || eq == null &&
                (!options.get(NAMESPACE_PREFIXES) || Token.eq(name1.prefix(), name2.prefix())) &&
                Token.eq(attr1.string(), attr2.string(), deep)) break;
            return false;
          }
        }
        iter2 = node2.attributeIter();
      }

      // compare namespaces
      if(options.get(IN_SCOPE_NAMESPACES)) {
        final Atts atts1 = deep.nested ? node1.namespaces() : node1.nsScope(null);
        final Atts atts2 = deep.nested ? node2.namespaces() : node2.nsScope(null);
        if(!atts1.equals(atts2)) return false;
      }
    } else if(type1 != DOCUMENT_NODE) {
      return true;
    }

    final Function<ANode, ANodeList> children = node -> {
      final ANodeList nl = new ANodeList();
      for(final ANode child : node.childIter()) {
        if(deep.qc != null) deep.qc.checkStop();
        final Type tp = child.type;
        if((tp != PROCESSING_INSTRUCTION || options.get(PROCESSING_INSTRUCTIONS)) &&
            (tp != COMMENT || options.get(COMMENTS))) {
          nl.add(tp != TEXT || nl.isEmpty() || nl.peek().type != NodeType.TEXT ? child.finish() :
            new FTxt(Token.concat(nl.pop().string(), child.string())));
        }
      }
      if(options.get(WHITESPACE) != Whitespace.PRESERVE && !preserve()) {
        for(int n = nl.size() - 1; n >= 0; n--) {
          final ANode child = nl.get(n);
          if(child.type == TEXT && Token.ws(child.string())) nl.remove(n);
        }
      }
      return nl;
    };

    final ANodeList list1 = children.apply(node1), list2 = children.apply(node2);
    final int size1 = list1.size();
    if(size1 != list2.size()) return false;
    deep.nested = true;

    // respect order
    if(name1 == null || !options.unordered(name1)) {
      for(final NodeIter iter1 = list1.iter(), iter2 = list2.iter();;) {
        if(deep.qc != null) deep.qc.checkStop();
        final ANode child1 = iter1.next();
        if(child1 == null) return true;
        if(!deep.equal(child1, iter2.next())) return false;
      }
    }

    // ignore order
    for(int l1 = size1 - 1; l1 >= 0; l1--) {
      boolean found = false;
      for(int l2 = list2.size() - 1; !found && l2 >= 0; l2--) {
        if(deep.qc != null) deep.qc.checkStop();
        if(deep.equal(list1.get(l1), list2.get(l2))) {
          list2.remove(l2);
          found = true;
        }
      }
      if(!found) return false;
    }
    return true;
  }

  /**
   * Returns if whitespace needs to be preserved.
   * @return node kind
   */
  private boolean preserve() {
    final QNm xs = new QNm(DataText.XML_SPACE, QueryText.XML_URI);
    for(ANode node = this; node != null; node = node.parent()) {
      if(node.type == ELEMENT) {
        final byte[] v = node.attribute(xs);
        if(v != null) return Token.eq(v, DataText.PRESERVE);
      }
    }
    return false;
  }

  @Override
  public final Item atomValue(final QueryContext qc, final InputInfo ii) {
    return atomItem(qc, ii);
  }

  @Override
  public final Item atomItem(final QueryContext qc, final InputInfo ii) {
    return type.oneOf(PROCESSING_INSTRUCTION, COMMENT) ? Str.get(string()) : Atm.get(string());
  }

  @Override
  public abstract ANode materialize(Predicate<Data> test, InputInfo ii, QueryContext qc)
      throws QueryException;

  @Override
  public final boolean materialized(final Predicate<Data> test, final InputInfo ii) {
    return test.test(data());
  }

  /**
   * Creates a database node copy from this node.
   * @param qc query context
   * @return database node
   * @throws QueryException query exception
   */
  public final DBNode copy(final QueryContext qc) throws QueryException {
    return copy(qc.context.options, qc);
  }

  /**
   * Creates a database node copy from this node.
   * @param options main options
   * @param qc query context (can be {@code null}; if supplied, allows interruption of process)
   * @return database node
   * @throws QueryException query exception
   */
  public final DBNode copy(final MainOptions options, final QueryContext qc) throws QueryException {
    final MemData data = new MemData(options);
    new DataBuilder(data, qc).build(this);
    return new DBNode(data);
  }

  /**
   * Returns a finalized node instance. This method is called when iterating through node results:
   * If a single node instances is recycled, it needs to be duplicated in the final step.
   * @return node
   */
  public abstract ANode finish();

  /**
   * Returns the name (optional prefix, local name) of an attribute, element or
   * processing instruction. This function is possibly evaluated faster than {@link #qname()},
   * as no {@link QNm} instance may need to be created.
   * @return name, or {@code null} if node has no name
   */
  public byte[] name() {
    return null;
  }

  /**
   * Returns the QName (optional prefix, local name) of an attribute, element or
   * processing instruction.
   * @return name, or {@code null} if node has no QName
   */
  public QNm qname() {
    return null;
  }

  /**
   * Returns all namespaces defined for the node.
   * Overwritten by {@link FElem} and {@link DBNode}.
   * @return namespace array or {@code null}
   */
  public Atts namespaces() {
    return null;
  }

  /**
   * Returns a copy of the namespace hierarchy.
   * @param sc static context (can be {@code null})
   * @return namespaces
   */
  public final Atts nsScope(final StaticContext sc) {
    final Atts ns = new Atts();
    for(ANode node = this; node != null; node = node.parent()) {
      final Atts nsp = node.namespaces();
      if(nsp != null) {
        for(int a = nsp.size() - 1; a >= 0; a--) {
          final byte[] name = nsp.name(a);
          if(!ns.contains(name)) ns.add(name, nsp.value(a));
        }
      }
    }
    if(sc != null) sc.ns.inScope(ns);
    return ns;
  }

  /**
   * Recursively finds the URI for the specified prefix.
   * @param prefix prefix
   * @return URI or {@code null}
   */
  public final byte[] uri(final byte[] prefix) {
    final Atts ns = namespaces();
    if(ns != null) {
      final byte[] s = ns.value(prefix);
      if(s != null) return s;
      final ANode n = parent();
      if(n != null) return n.uri(prefix);
    }
    return prefix.length == 0 ? Token.EMPTY : null;
  }

  /**
   * Returns the base URI of the node.
   * @return base URI
   */
  public byte[] baseURI() {
    return Token.EMPTY;
  }

  /**
   * Returns the static base URI of a node.
   * @param base static base URI
   * @param empty return empty URI if a node has no base URI, or {@code null} otherwise
   * @param info input info (can be {@code null})
   * @return base URI or {@code null}
   * @throws QueryException query exception
   */
  public Uri baseURI(final Uri base, final boolean empty, final InputInfo info)
      throws QueryException {

    if(!type.oneOf(NodeType.ELEMENT, NodeType.DOCUMENT_NODE) && parent() == null) {
      return empty ? Uri.EMPTY : null;
    }
    Uri uri = Uri.EMPTY;
    ANode nd = this;
    do {
      if(nd == null) return base.resolve(uri, info);
      final Uri bu = Uri.get(nd.baseURI(), false);
      if(!bu.isValid()) throw INVURI_X.get(info, nd.baseURI());
      uri = bu.resolve(uri, info);
      if(nd.type == NodeType.DOCUMENT_NODE && nd instanceof DBNode) break;
      nd = nd.parent();
    } while(!uri.isAbsolute());
    return uri;
  }

  /**
   * Checks if two nodes are identical.
   * @param node node to be compared
   * @return result of check
   */
  public abstract boolean is(ANode node);

  /**
   * Checks the document order of two nodes.
   * @param node node to be compared
   * @return {@code 0} if the nodes are identical, or {@code 1}/{@code -1}
   * if the node appears after/before the argument
   */
  public abstract int compare(ANode node);

  /**
   * Compares two nodes for their unique order.
   * @param node1 first node
   * @param node2 node to be compared
   * @return result of comparison (-1, 0, 1)
   */
  static int compare(final ANode node1, final ANode node2) {
    // cache parents of first node
    final ANodeList nl = new ANodeList();
    for(ANode node = node1; node != null; node = node.parent()) {
      if(node == node2) return 1;
      nl.add(node);
    }
    // find the lowest common ancestor
    ANode c2 = node2;
    LOOP:
    for(ANode node = node2; (node = node.parent()) != null;) {
      final int is = nl.size();
      for(int i = 1; i < is; i++) {
        if(node == node1) return -1;
        if(!nl.get(i).is(node)) continue;
        // check which node appears as first LCA child
        final ANode c1 = nl.get(i - 1);
        for(final ANode c : node.childIter()) {
          if(c.is(c1)) return -1;
          if(c.is(c2)) return 1;
        }
        break LOOP;
      }
      c2 = node;
    }
    return Integer.signum(node1.id - node2.id);
  }

  /**
   * Returns the root of a node (the topmost ancestor without parent node).
   * @return root node
   */
  public final ANode root() {
    final ANode p = parent();
    return p == null ? this : p.root();
  }

  /**
   * Returns the parent node.
   * @return parent node or {@code null}
   */
  public abstract ANode parent();

  /**
   * Sets the parent node.
   * @param par parent node
   */
  public abstract void parent(FNode par);

  /**
   * Indicates if the node has children.
   * @return result of test
   */
  public abstract boolean hasChildren();

  /**
   * Indicates if the node has attributes.
   * @return result of test
   */
  public abstract boolean hasAttributes();

  /**
   * Returns the value of the specified attribute.
   * @param name attribute to be found
   * @return attribute value or {@code null}
   */
  public final byte[] attribute(final QNm name) {
    final BasicNodeIter iter = attributeIter();
    while(true) {
      final ANode node = iter.next();
      if(node == null) return null;
      if(node.qname().eq(name)) return node.string();
    }
  }

  /**
   * Returns a light-weight ancestor-or-self axis iterator.
   * Before nodes are added to a result, they must be finalized via {@link ANode#finish()}.
   * @param self include self node
   * @return iterator
   */
  public BasicNodeIter ancestorIter(final boolean self) {
    return new BasicNodeIter() {
      private ANode node = ANode.this;
      private boolean slf = self;

      @Override
      public ANode next() {
        if(slf) {
          slf = false;
        } else {
          node = node.parent();
        }
        return node;
      }
    };
  }

  /**
   * Returns a light-weight attribute axis iterator with {@link Iter#size()} and
   * {@link Iter#get(long)} implemented.
   * Before nodes are added to a result, they must be finalized via {@link ANode#finish()}.
   * @return iterator
   */
  public abstract BasicNodeIter attributeIter();

  /**
   * Returns a light-weight child axis iterator.
   * Before nodes are added to a result, they must be finalized via {@link ANode#finish()}.
   * @return iterator
   */
  public abstract BasicNodeIter childIter();

  /**
   * Returns an iterator for all descendant nodes.
   * @param self include self node
   * @return node iterator
   */
  public BasicNodeIter descendantIter(final boolean self) {
    return new BasicNodeIter() {
      private final Stack<BasicNodeIter> iters = new Stack<>();
      private ANode last;

      @Override
      public ANode next() {
        final BasicNodeIter ir = last != null ? last.childIter() : self ? selfIter() : childIter();
        last = ir.next();
        if(last == null) {
          while(!iters.isEmpty()) {
            last = iters.peek().next();
            if(last != null) break;
            iters.pop();
          }
        } else {
          iters.add(ir);
        }
        return last;
      }
    };
  }

  /**
   * Returns an iterator for all descendant nodes.
   * @param self include self node
   * @return node iterator
   */
  public BasicNodeIter followingIter(final boolean self) {
    return new BasicNodeIter() {
      private BasicNodeIter iter;

      @Override
      public ANode next() {
        if(iter == null) {
          final ANodeList list = new ANodeList();
          if(self) list.add(finish());
          ANode node = ANode.this, root = node.parent();
          while(root != null) {
            final BasicNodeIter ir = root.childIter();
            if(node.type != ATTRIBUTE) {
              for(final ANode nd : ir) {
                if(nd.is(node)) break;
              }
            }
            for(final ANode nd : ir) {
              list.add(nd.finish());
              addDescendants(nd.childIter(), list);
            }
            node = root;
            root = root.parent();
          }
          iter = list.iter();
        }
        return iter.next();
      }
    };
  }

  /**
   * Returns a light-weight following-sibling axis iterator.
   * Before nodes are added to a result, they must be finalized via {@link ANode#finish()}.
   * @param self include self node
   * @return iterator
   */
  public BasicNodeIter followingSiblingIter(final boolean self) {
    final ANode root = parent();
    if(root == null || type == ATTRIBUTE) return self ? selfIter() : BasicNodeIter.EMPTY;

    return new BasicNodeIter() {
      private final BasicNodeIter iter = root.childIter();
      private boolean found;

      @Override
      public ANode next() {
        for(ANode n; !found && (n = iter.next()) != null;) {
          if(n.is(ANode.this)) {
            found = true;
            if(self) return n;
          }
        }
        return iter.next();
      }
    };
  }

  /**
   * Returns a light-weight parent axis iterator.
   * Before nodes are added to a result, they must be finalized via {@link ANode#finish()}.
   * @return iterator
   */
  public final BasicNodeIter parentIter() {
    return new BasicNodeIter() {
      private boolean called;

      @Override
      public ANode next() {
        if(called) return null;
        called = true;
        return parent();
      }
    };
  }

  /**
   * Returns a light-weight preceding axis iterator.
   * Before nodes are added to a result, they must be finalized via {@link ANode#finish()}.
   * @param self include self node
   * @return iterator
   */
  public BasicNodeIter precedingIter(final boolean self) {
    return new BasicNodeIter() {
      private BasicNodeIter iter;

      @Override
      public ANode next() {
        if(iter == null) {
          final ANodeList list = new ANodeList();
          if(self) list.add(finish());
          ANode node = ANode.this, root = node.parent();
          while(root != null) {
            if(node.type != ATTRIBUTE) {
              final ANodeList tmp = new ANodeList();
              for(final ANode c : root.childIter()) {
                if(c.is(node)) break;
                tmp.add(c.finish());
                addDescendants(c.childIter(), tmp);
              }
              for(int t = tmp.size() - 1; t >= 0; t--) list.add(tmp.get(t));
            }
            node = root;
            root = root.parent();
          }
          iter = list.iter();
        }
        return iter.next();
      }
    };
  }

  /**
   * Returns a light-weight preceding-sibling axis iterator.
   * Before nodes are added to a result, they must be finalized via {@link ANode#finish()}.
   * @param self include self node
   * @return iterator
   */
  public final BasicNodeIter precedingSiblingIter(final boolean self) {
    final ANode root = parent();
    if(root == null || type == ATTRIBUTE) return self ? selfIter() : BasicNodeIter.EMPTY;

    return new BasicNodeIter() {
      private ANodeList list;
      private int l;

      @Override
      public ANode next() {
        if(list == null) {
          list = new ANodeList();
          for(final ANode node : root.childIter()) {
            final boolean last = node.is(ANode.this);
            if(!last || self) list.add(node.finish());
            if(last) break;
          }
          l = list.size();
        }
        return l > 0 ? list.get(--l) : null;
      }
    };
  }

  /**
   * Returns a self axis iterator.
   * @return iterator
   */
  public final BasicNodeIter selfIter() {
    return new BasicNodeIter() {
      private boolean called;

      @Override
      public ANode next() {
        if(called) return null;
        called = true;
        return ANode.this;
      }
    };
  }

  /**
   * Adds nodes of a child iterator and its descendants.
   * @param children child nodes
   * @param nodes node cache
   */
  private static void addDescendants(final BasicNodeIter children, final ANodeList nodes) {
    for(final ANode node : children) {
      nodes.add(node.finish());
      addDescendants(node.childIter(), nodes);
    }
  }

  /**
   * Returns a database kind for the specified node type.
   * @return node kind
   */
  public int kind() {
    return kind((NodeType) type);
  }

  /**
   * Returns a database kind for the specified node type.
   * @param type node type
   * @return node kind, or {@code -1} if no corresponding database kind exists
   */
  public static int kind(final NodeType type) {
    return switch(type) {
      case DOCUMENT_NODE -> Data.DOC;
      case ELEMENT -> Data.ELEM;
      case TEXT -> Data.TEXT;
      case ATTRIBUTE -> Data.ATTR;
      case COMMENT -> Data.COMM;
      case PROCESSING_INSTRUCTION -> Data.PI;
      default -> -1;
    };
  }

  /**
   * Returns a node type for the specified database kind.
   * @param k database kind
   * @return node type
   */
  public static NodeType type(final int k) {
    return TYPES[k];
  }

  @Override
  public abstract BXNode toJava() throws QueryException;
}

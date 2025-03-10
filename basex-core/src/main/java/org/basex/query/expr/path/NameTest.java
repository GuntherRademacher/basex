package org.basex.query.expr.path;

import org.basex.data.*;
import org.basex.query.value.item.*;
import org.basex.query.value.node.*;
import org.basex.query.value.type.*;
import org.basex.util.*;

/**
 * Name test.
 *
 * @author BaseX Team, BSD License
 * @author Christian Gruen
 */
public final class NameTest extends Test {
  /** QName test. */
  public final QNm qname;
  /** Part of name to be tested. */
  public final NamePart part;
  /** Local name. */
  public final byte[] local;
  /** Default element namespace. */
  private final byte[] defaultNs;

  /** Perform only local check at runtime. */
  private boolean simple;

  /**
   * Convenience constructor for element tests.
   * @param name node name
   */
  public NameTest(final QNm name) {
    this(name, NamePart.FULL, NodeType.ELEMENT, null);
  }

  /**
   * Constructor.
   * @param qname name
   * @param part part of name to be tested
   * @param type node type
   * @param defaultNs default element namespace (used for optimizations, can be {@code null})
   */
  public NameTest(final QNm qname, final NamePart part, final NodeType type,
      final byte[] defaultNs) {

    super(type);
    this.qname = qname;
    this.part = part;
    this.defaultNs = defaultNs != null ? defaultNs : Token.EMPTY;
    local = qname.local();
  }

  @Override
  public Test optimize(final Data data) {
    // skip optimizations if data reference is not known at compile time
    if(data == null) return this;

    // skip optimizations if more than one namespace is defined in the database
    final byte[] dataNs = data.defaultNs();
    if(dataNs == null) return this;

    // check if test may yield results
    if(part == NamePart.FULL && !qname.hasURI()) {
      // element and db default namespaces are different: no results
      if(type != NodeType.ATTRIBUTE && !Token.eq(dataNs, defaultNs)) return null;
      // namespace is irrelevant/identical: only check local name
      simple = true;
    }

    // check existence of local element/attribute names
    return type == NodeType.PROCESSING_INSTRUCTION || part() != NamePart.LOCAL ||
      (type == NodeType.ELEMENT ? data.elemNames : data.attrNames).contains(local) ? this : null;
  }

  @Override
  public Test copy() {
    return this;
  }

  @Override
  public boolean matches(final ANode node) {
    if(node.type != type) return false;
    switch(part()) {
      // namespaces wildcard: only check local name
      case LOCAL: return Token.eq(local, Token.local(node.name()));
      // name wildcard: only check namespace
      case URI: return Token.eq(qname.uri(), node.qname().uri());
      // check attributes, or check everything
      default: return qname.eq(node.qname());
    }
  }

  /**
   * Checks if the specified name matches the test.
   * @param qName name
   * @return result of check
   */
  public boolean matches(final QNm qName) {
    switch(part()) {
      // namespaces wildcard: only check local name
      case LOCAL: return Token.eq(local, qName.local());
      // name wildcard: only check namespace
      case URI: return Token.eq(qname.uri(), qName.uri());
      // check everything
      default: return qname.eq(qName);
    }
  }

  @Override
  public Boolean matches(final SeqType seqType) {
    final Type tp = seqType.type;
    if(tp.intersect(type) == null) return Boolean.FALSE;
    final Test test = seqType.test();
    if(tp == type && test instanceof NameTest) {
      final NameTest np = (NameTest) test;
      if(np.part == NamePart.FULL || np.part == part) return matches(np.qname);
    }
    return null;
  }

  /**
   * Returns the name part relevant at runtime.
   * @return name part
   */
  public NamePart part() {
    return simple ? NamePart.LOCAL : part;
  }

  @Override
  public boolean instanceOf(final Test test) {
    if(!(test instanceof NameTest)) return super.instanceOf(test);
    final NameTest nt = (NameTest) test;
    if(type != nt.type) return false;
    switch(nt.part) {
      case FULL:
        switch(part) {
          case FULL: return qname.eq(nt.qname);
          case LOCAL:
          case URI: return false;
          default: throw Util.notExpected();
        }
      case LOCAL:
        switch(part) {
          case LOCAL:
          case FULL: return Token.eq(qname.local(), nt.qname.local());
          case URI: return false;
          default: throw Util.notExpected();
        }
      case URI:
        switch(part) {
          case URI:
          case FULL: return Token.eq(qname.uri(), nt.qname.uri());
          case LOCAL: return false;
          default: throw Util.notExpected();
        }
      default: throw Util.notExpected();
    }
  }

  @Override
  public Test intersect(final Test test) {
    if(test instanceof NameTest) {
      final NameTest nt = (NameTest) test;
      if(type == nt.type) {
        if(part == nt.part || part == NamePart.FULL) {
          if(nt.matches(qname)) return this;
        } else if(nt.part == NamePart.FULL) {
          return test.intersect(this);
        } else {
          final boolean lcl = part == NamePart.LOCAL;
          final QNm qnm = new QNm(lcl ? local : nt.local, lcl ? nt.qname.uri() : qname.uri());
          return new NameTest(qnm, NamePart.FULL, type, defaultNs);
        }
      }
    } else if(test instanceof KindTest) {
      if(type.instanceOf(test.type)) return this;
    } else if(test instanceof UnionTest) {
      return test.intersect(this);
    }
    // DocTest, InvDocTest
    return null;
  }

  @Override
  public boolean equals(final Object obj) {
    if(!(obj instanceof NameTest)) return false;
    final NameTest nt = (NameTest) obj;
    return type == nt.type && part == nt.part && qname.eq(nt.qname);
  }

  @Override
  public String toString(final boolean full) {
    final boolean pi = type == NodeType.PROCESSING_INSTRUCTION;
    final TokenBuilder tb = new TokenBuilder();

    // add URI part
    final byte[] prefix = qname.prefix(), uri = qname.uri();
    if(part == NamePart.LOCAL && !pi) {
      if(!(full && type == NodeType.ATTRIBUTE)) tb.add("*:");
    } else if(prefix.length > 0) {
      tb.add(prefix).add(':');
    } else if(uri.length != 0) {
      tb.add("Q{").add(uri).add('}');
    }
    // add local part
    if(part == NamePart.URI) {
      tb.add('*');
    } else {
      tb.add(qname.local());
    }
    final String test = tb.toString();
    return full || pi ? type.toString(test) : test;
  }
}

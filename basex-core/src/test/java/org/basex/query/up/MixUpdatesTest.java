package org.basex.query.up;

import static org.basex.query.QueryError.*;
import static org.basex.query.func.Function.*;

import org.basex.*;
import org.basex.core.*;
import org.junit.jupiter.api.*;

/**
 * Tests for the {@link MainOptions#MIXUPDATES} flag.
 *
 * @author BaseX Team, BSD License
 * @author Christian Gruen
 */
public final class MixUpdatesTest extends SandboxTest {
  /** Prepare tests. */
  @BeforeAll public static void beforeClass() {
    set(MainOptions.MIXUPDATES, true);
  }

  /** Resets optimizations. */
  @AfterEach public void init() {
    inline(false);
    unroll(false);
  }

  /** Transform expression containing a simple expression. */
  @Test public void transSimple() {
    error("<a/> update { '' }", UPMODIFY);
    error("copy $a := <a/> modify '' return $a", UPMODIFY);
  }

  /** Update test. */
  @Test public void list() {
    query("delete node <a/>, 1," + _UPDATE_OUTPUT.args(2), "1\n2");
  }

  /** Update test. */
  @Test public void evalUpdate() {
    query(_XQUERY_EVAL_UPDATE.args("1"), 1);
    query(_XQUERY_EVAL_UPDATE.args("1") + ", 2", "1\n2");
  }

  /** Test method. */
  @Test public void output() {
    query(_UPDATE_OUTPUT.args("x") + ", 1", "1\nx");
  }

  /** Annotations. */
  @Test public void annotations() {
    query("declare %updating function local:x() { 1 }; local:x()", 1);
  }

  /** Updating functions. */
  @Test public void updatingFunctions() {
    query("declare %updating function local:b() {" + _UPDATE_OUTPUT.args(1) + " }; local:b()", 1);

    query("declare function local:not-used() { local:b#0 };"
        + "declare %updating function local:b() {" + _UPDATE_OUTPUT.args(1) + " }; local:b()", 1);

    query("function($a) {" + _UPDATE_OUTPUT.args(1) + " }(1)", 1);
    query(_UPDATE_OUTPUT.args(" ?") + "(1)", 1);
    query("update:output#1(1)", 1);
    query("declare function local:a() { 1 }; local:a#0()", 1);
    query("declare function local:a() { local:b#0 };"
        + "declare function local:b() {" + _UPDATE_OUTPUT.args(1) + " }; local:a()()", 1);
  }

  /** Test method. */
  @Test public void functionLookup() {
    query("declare function local:a() {" + _UPDATE_OUTPUT.args(1) + " };"
        + "function-lookup(xs:QName('local:a'), 0)()", 1);
  }

  /** Test method. */
  @Test public void flwor() {
    query("<x>X</x> update { let $_ := delete node text() where 0 return $_ }", "<x/>");
    query("<x>X</x> update { let $_ := delete node text() return () }", "<x/>");
  }

  /** Test method. */
  @Test public void functionItem() {
    query("let $x := <a>a</a> update {} return (delete node $x/text(), [$x])", "[<a/>]");
  }

  /** Test method. */
  @Test public void xqueryEval() {
    query(_XQUERY_EVAL.args("function($x) { function() { $x }  }(4)") + "()", 4);
  }

  /** Test method. */
  @Test public void gh1281() {
    query("declare function local:f() {" + _UPDATE_OUTPUT.args(1) + " }; local:f()", 1);
  }

  /**  Reject updating functions in built-in higher-order function. */
  @Test public void updatingHof() {
    inline(true);
    query("for-each(1, update:output#1)", 1);
    query("apply(update:output#1, [1])", 1);
  }

  /** Test method. */
  @Test public void gh2136() {
    query(_XQUERY_EVAL.args(" '<a/> update {}'"), "<a/>");
    query(_XQUERY_EVAL.args(" 'function() { <a/> update {} }'") + "()", "<a/>");
    query(_XQUERY_EVAL.args(" 'function() { <a/> update {} }'") + " ! .()", "<a/>");

    query("<x/> !" + FOR_EACH_PAIR.args(" .", " .", " contains#2"), true);
    query("<x/> !" + _UPDATE_FOR_EACH_PAIR.args(" .", " .", " contains#2"), true);

    query("<x/> !" + FOR_EACH_PAIR.args(" .", " .",
        " function($k, $v) {" + _UPDATE_OUTPUT.args(1) + "}"), 1);
    query("<x/> !" + _UPDATE_FOR_EACH_PAIR.args(" .", " .",
        " function($k, $v) {" + _UPDATE_OUTPUT.args(1) + "}"), 1);

    query("<x/> !" + FOR_EACH_PAIR.args(" .", " .",
        " function($k, $v) { update:output#1 }") + "(1)", 1);
    query("<x/> !" + _UPDATE_FOR_EACH_PAIR.args(" .", " .",
        " function($k, $v) { update:output#1 }") + "(1)", 1);

    query("<x/> !" + FOR_EACH_PAIR.args(" .", " .",
        " function($k, $v) { update:output#1 }") + " ! .(1)", 1);
    query("<x/> !" + _UPDATE_FOR_EACH_PAIR.args(" .", " .",
        " function($k, $v) { update:output#1 }") + " ! .(1)", 1);
  }
}

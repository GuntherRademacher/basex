package org.basex.query.util.parse;

import static org.basex.query.QueryError.*;
import static org.basex.util.Token.*;

import java.util.*;

import org.basex.query.*;
import org.basex.query.expr.*;
import org.basex.query.value.item.*;
import org.basex.query.var.*;
import org.basex.util.*;

/**
 * Organizes local variables.
 *
 * @author BaseX Team, BSD License
 * @author Christian Gruen
 */
public final class LocalVars {
  /** Stack of variable contexts. */
  private final ArrayList<VarContext> vars = new ArrayList<>();
  /** Query parser. */
  private final QueryParser parser;

  /**
   * Constructor.
   * @param parser query parser
   */
  public LocalVars(final QueryParser parser) {
    this.parser = parser;
  }

  /**
   * Creates and registers a new local variable in the current scope.
   * @param var variable to be added (can be {@code null})
   * @return variable
   */
  public Var add(final Var var) {
    if(var != null) vars.get(vars.size() - 1).add(var);
    return var;
  }

  /**
   * Creates and registers local variables in the current scope.
   * @param vrs variables to be added (can be {@code null})
   */
  public void add(final Var... vrs) {
    for(final Var var : vrs) add(var);
  }

  /**
   * Tries to resolve a local variable reference.
   * @param name variable name
   * @param info input info (can be {@code null})
   * @return variable reference or {@code null}
   */
  public VarRef resolveLocal(final QNm name, final InputInfo info) {
    int l = vars.size();
    Var var = null;

    // look up through the scopes until we find the declaring scope
    while(--l >= 0) {
      var = vars.get(l).stack.get(name);
      if(var != null) break;
    }

    // looked through all scopes, must be a static variable
    if(var == null) return null;

    // go down through the scopes and add bindings to their closures
    final int ls = vars.size();
    while(++l < ls) {
      final VarContext vctx = vars.get(l);
      final Var local = new Var(var.name, var.seqType(), parser.qc, info);
      vctx.add(local);
      vctx.bindings.put(local, new VarRef(info, var));
      var = local;
    }

    // return the properly propagated variable reference
    return new VarRef(info, var);
  }

  /**
   * Resolves the referenced variable as a local or static variable and returns a reference to it.
   * IF the variable is not declared, the specified error is thrown.
   * @param name variable name
   * @param info input info (can be {@code null})
   * @return referenced variable
   * @throws QueryException if the variable is not defined
   */
  public ParseExpr resolve(final QNm name, final InputInfo info) throws QueryException {
    // local variable
    final VarRef ref = resolveLocal(name, info);
    if(ref != null) return ref;

    // static variable
    final byte[] uri = name.uri();

    // accept variable reference...
    // - if a variable uses the module or an imported URI, or
    // - if it is specified in the main module
    final QNm module = parser.sc.module;
    final boolean hasImport = parser.moduleURIs.contains(uri);
    if(module == null || eq(module.uri(), uri) || hasImport)
      return parser.qc.vars.newRef(name, info, hasImport);

    throw parser.error(VARUNDEF_X, info, '$' + string(name.string()));
  }

  /**
   * Pushes a new variable context onto the stack.
   * @param global create global mapping for non-local variables
   * @return global map or {@code null})
   */
  public HashMap<Var, Expr> pushContext(final boolean global) {
    final HashMap<Var, Expr> map = global ? new HashMap<>() : null;
    vars.add(new VarContext(map));
    return map;
  }

  /**
   * Pops one variable context from the stack.
   * @return the removed context's variable scope
   */
  public VarScope popContext() {
    return vars.remove(vars.size() - 1).vs;
  }

  /**
   * Opens a new sub-scope inside the current one. The returned marker has to be supplied to the
   * corresponding call to {@link #closeScope(int)} in order to mark the variables as inaccessible.
   * @return marker for the current bindings
   */
  public int openScope() {
    return vars.get(vars.size() - 1).stack.size();
  }

  /**
   * Closes the sub-scope and marks all contained variables as inaccessible.
   * @param marker marker for the start of the sub-scope
   */
  public void closeScope(final int marker) {
    vars.get(vars.size() - 1).stack.size(marker);
  }
}

package org.basex.query;

import static org.basex.core.Text.*;
import static org.basex.query.QueryError.*;

import java.util.*;

import org.basex.query.expr.*;
import org.basex.query.value.*;
import org.basex.query.value.item.*;
import org.basex.query.value.seq.*;
import org.basex.util.*;
import org.basex.util.list.*;

/**
 * Thrown to indicate an exception during the parsing or evaluation of a query.
 *
 * @author BaseX Team, BSD License
 * @author Christian Gruen
 */
public class QueryException extends Exception {
  /** Static exception. */
  static final QueryException ERROR = new QueryException("") {
    @Override
    public synchronized Throwable fillInStackTrace() { return this; }
  };

  /** Stack. */
  private final ArrayList<InputInfo> stack = new ArrayList<>();
  /** Error QName. */
  private final QNm name;
  /** Error value. */
  private Value value = Empty.VALUE;
  /** Error reference ({@code null} for dynamic error messages). */
  private QueryError error;
  /** Error line and column. */
  private InputInfo info;
  /** Marked error column. */
  private int markedCol;
  /** Marks if this exception is catchable by a {@code try/catch} expression. */
  private boolean catchable = true;

  /**
   * Constructor, specifying an exception or error. {@link QueryError#BASEX_ERROR_X} will be set
   * as error code.
   * @param cause exception or error
   */
  public QueryException(final Throwable cause) {
    this(Util.message(cause));
    initCause(cause);
  }

  /**
   * Constructor, specifying a simple error message. {@link QueryError#BASEX_ERROR_X} will be set
   * as error code.
   * @param message error message
   */
  public QueryException(final String message) {
    this(null, BASEX_ERROR_X, message);
  }

  /**
   * Default constructor.
   * @param info input info (can be {@code null})
   * @param error error reference
   * @param ext error extension
   */
  public QueryException(final InputInfo info, final QueryError error, final Object... ext) {
    this(info, error.qname(), error.message(), ext);
    this.error = error;
  }

  /**
   * Constructor, specifying the error code and message as string.
   * @param info input info (can be {@code null})
   * @param name error code
   * @param message error message
   * @param ext error extension
   */
  public QueryException(final InputInfo info, final QNm name, final String message,
      final Object... ext) {

    super(message(message, ext, info));
    this.name = name;
    if(info != null) info(info);
    for(final Object o : ext) {
      if(o instanceof Throwable) {
        initCause((Throwable) o);
        break;
      }
    }
  }

  /**
   * Returns the column number of the error.
   * @return column number, or {@code 0} if unknown
   */
  public final int column() {
    return info == null ? 0 : info.column();
  }

  /**
   * Returns the column number of a marked error.
   * @return column number
   */
  public final int markedColumn() {
    return markedCol;
  }

  /**
   * Returns the line number of the error.
   * @return line number, or {@code 0} if unknown
   */
  public final int line() {
    return info == null ? 0 : info.line();
  }

  /**
   * Returns the input path.
   * @return input path, or {@code null} if unknown
   */
  public final String path() {
    return info == null ? null : info.path();
  }

  /**
   * Sets code suggestions.
   * @param qp query parser
   * @return self reference
   */
  public final QueryException suggest(final InputParser qp) {
    pos(qp);
    return this;
  }

  /**
   * Adds an input info to the stack.
   * @param ii input info (can be {@code null})
   * @return self reference
   */
  public final QueryException add(final InputInfo ii) {
    if(ii != null) stack.add(ii);
    return this;
  }

  /**
   * Sets input info.
   * @param ii input info (can be {@code null})
   * @return self reference
   */
  public final QueryException info(final InputInfo ii) {
    info = ii;
    return this;
  }

  /**
   * Returns the input info.
   * @return input info (can be {@code null})
   */
  public final InputInfo info() {
    return info;
  }

  /**
   * Sets the error value.
   * @param val error value (can be {@code null})
   * @return self reference
   */
  public final QueryException value(final Value val) {
    value = val;
    return this;
  }

  /**
   * Sets an error.
   * @param err error
   * @return self reference
   */
  final QueryException error(final QueryError err) {
    error = err;
    return this;
  }

  /**
   * Adopts input information from the specified parser.
   * @param parser parser
   */
  final void pos(final InputParser parser) {
    markedCol = parser.mark;
    if(info != null) return;
    // check if line/column information has already been added
    parser.pos = Math.min(parser.mark, parser.length);
    info = parser.info();
  }

  /**
   * Returns the error name.
   * @return error name
   */
  public final QNm qname() {
    return name;
  }

  /**
   * Returns the error.
   * @return error (can be {@code null})
   */
  public final QueryError error() {
    return error;
  }

  /**
   * Returns the error value.
   * @return error value
   */
  public final Value value() {
    return value;
  }

  /**
   * Checks if the exception matches the specified type.
   * @param type error type
   * @return result of check
   */
  public final boolean matches(final ErrType type) {
    return error != null && error.toString().startsWith(type.name());
  }

  @Override
  public final String getLocalizedMessage() {
    return super.getMessage();
  }

  @Override
  public final String getMessage() {
    final TokenBuilder tb = new TokenBuilder();
    if(info != null) tb.add(STOPPED_AT).add(info).add(COL).add(NL);
    final byte[] code = name.local();
    if(code.length != 0) tb.add('[').add(name.prefixId(QueryText.ERROR_URI)).add("] ");
    tb.add(getLocalizedMessage());
    if(!stack.isEmpty()) {
      tb.add(NL).add(NL).add(STACK_TRACE).add(COL);
      for(final InputInfo ii : stack) tb.add(NL).add(LI).add(ii);
    }
    return tb.toString();
  }

  /**
   * Returns a stack trace expression.
   * @return expression
   */
  public final Str stackTrace() {
    final TokenBuilder tb = new TokenBuilder();
    if(info != null) tb.add(info).add('\n');
    for(final InputInfo stck : stack) tb.add(stck).add('\n');
    return Str.get(tb.finish());
  }

  /**
   * Checks if this exception can be caught by a {@code try/catch} expression.
   * @return result of check
   */
  public final boolean isCatchable() {
    return catchable;
  }

  /**
   * Makes this exception uncatchable by a {@code try/catch} expression.
   * @return self reference for convenience
   */
  public final QueryException notCatchable() {
    catchable = false;
    return this;
  }

  /**
   * Creates the error message from the specified text and extension array.
   * @param info input info (can be {@code null})
   * @param text text message with optional placeholders
   * @param ext info extensions
   * @return argument
   */
  private static String message(final String text, final Object[] ext, final InputInfo info) {
    final TokenList list = new TokenList(ext.length);
    for(final Object e : ext) {
      list.add(normalize(e instanceof ExprInfo ? ((ExprInfo) e).toErrorString() : e, info));
    }
    return Util.info(text, (Object[]) list.finish());
  }
}

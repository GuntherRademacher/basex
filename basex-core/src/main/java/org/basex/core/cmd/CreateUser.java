package org.basex.core.cmd;

import static org.basex.core.Text.*;

import org.basex.core.*;
import org.basex.core.parse.*;
import org.basex.core.parse.Commands.Cmd;
import org.basex.core.parse.Commands.CmdCreate;
import org.basex.core.users.*;

/**
 * Evaluates the 'create user' command and creates a new user.
 *
 * @author BaseX Team, BSD License
 * @author Christian Gruen
 */
public final class CreateUser extends AUser {
  /**
   * Default constructor.
   * @param name username
   * @param pw password
   */
  public CreateUser(final String name, final String pw) {
    super(name, pw);
  }

  @Override
  protected boolean run() {
    final String name = args[0], password = args[1];
    if(!Databases.validName(name)) return error(NAME_INVALID_X, name);
    if(name.equals(UserText.ADMIN)) return error(ADMIN_STATIC);

    final Users users = context.users;
    users.add(new User(name, password));
    users.write();
    return info(USER_CREATED_X, args[0]);
  }

  @Override
  public void build(final CmdBuilder cb) {
    cb.init(Cmd.CREATE + " " + CmdCreate.USER).arg(0);
    if(!cb.password()) cb.arg(1);
  }
}

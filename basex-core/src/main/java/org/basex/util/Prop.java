package org.basex.util;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.Map.*;
import java.util.concurrent.*;
import java.util.jar.*;
import java.util.stream.*;

import org.basex.io.*;
import org.basex.util.options.*;

/**
 * This class contains constants and system properties which are used all around the project.
 *
 * @author BaseX Team, BSD License
 * @author Christian Gruen
 */
public final class Prop {
  /** Project name. */
  public static final String NAME = "BaseX";
  /** Current version (major and minor number, optionally followed by development tag). */
  private static final String CURRENT_VERSION = "12.1 beta";

  /** Name of project in lower case. */
  public static final String PROJECT = NAME.toLowerCase(Locale.ENGLISH);
  /** Version string. */
  public static final String VERSION;

  /** System-specific newline string. */
  public static final String NL = System.lineSeparator();
  /** Returns the system's default character set. */
  public static final Charset CHARSET = Charset.defaultCharset();
  /** Java vendor. */
  public static final String JAVA_VENDOR = System.getProperty("java.vendor");
  /** Java version. */
  public static final String JAVA_VERSION = System.getProperty("java.version");
  /** OS architecture. */
  public static final String OS_ARCH = System.getProperty("os.arch");
  /** OS flag. */
  public static final String OS = System.getProperty("os.name");
  /** Flag denoting if OS belongs to Mac family. */
  public static final boolean MAC = OS.startsWith("Mac");
  /** Flag denoting if OS belongs to Windows family. */
  public static final boolean WIN = OS.startsWith("Windows");
  /** Respect lower/upper case when doing file comparisons. */
  public static final boolean CASE = !(MAC || WIN);

  /** Prefix for project specific options. */
  public static final String DBPREFIX = "org.basex.";
  /** System property for specifying database home directory. */
  public static final String PATH = DBPREFIX + "path";

  /** Application URL (can be {@code null}). */
  public static final URL LOCATION;
  /** System's temporary directory. */
  public static final String TEMPDIR = dir(System.getProperty("java.io.tmpdir"));
  /** Project home directory. */
  public static final String HOMEDIR;

  /** Availability of ICU. */
  public static final boolean ICU = Reflect.available("com.ibm.icu.text.BreakIterator");

  /** Global options, assigned by the starter classes and the web.xml context parameters. */
  private static final Map<String, String> OPTIONS = new ConcurrentHashMap<>();

  static {
    URL location = null;
    final ProtectionDomain pd = Prop.class.getProtectionDomain();
    if(pd != null) {
      final CodeSource cs = pd.getCodeSource();
      if(cs != null) location = cs.getLocation();
    }
    LOCATION = location;

    // check system property 'org.basex.path'
    String homedir = System.getProperty(PATH);
    // check if current working directory contains configuration file
    if(homedir == null) homedir = configDir(System.getProperty("user.dir"));
    // check if application directory contains configuration file
    if(homedir == null) homedir = configDir(applicationDir(LOCATION));
    // fallback: choose home directory (linux: check HOME variable, GH-773)
    if(homedir == null) {
      final String home = WIN ? null : System.getenv("HOME");
      homedir = dir(home != null ? home : System.getProperty("user.home")) + PROJECT;
    }
    HOMEDIR = dir(homedir);

    final Attributes atts = manifest();
    String version = atts.getValue("Implementation-Version");
    if(version != null && version.contains("-SNAPSHOT")) {
      final String build = atts.getValue("Implementation-Build");
      if(build != null) version += ' ' + build;
    } else {
      version = CURRENT_VERSION;
    }
    VERSION = version.replace("-SNAPSHOT", " beta");
  }

  // STATIC OPTIONS ===============================================================================

  /** Language (applied after restart). */
  public static String language = "English";
  /** Debug mode. */
  public static boolean debug;

  /** Private constructor. */
  private Prop() { }

  // STATIC METHODS ===============================================================================

  /**
   * Checks if one of the files .basexhome or .basex are found in the specified directory.
   * @param dir directory (can be {@code null})
   * @return configuration directory (can be {@code null})
   */
  private static String configDir(final String dir) {
    if(dir != null) {
      final String home = IO.BASEXSUFFIX + "home";
      final IOFile file = new IOFile(dir, home);
      if(file.exists() || new IOFile(dir, IO.BASEXSUFFIX).exists()) return dir;
    }
    return null;
  }

  /**
   * Returns the application directory.
   * @param location location of application (can be {@code null})
   * @return application directory (can be {@code null})
   */
  private static String applicationDir(final URL location) {
    try {
      if(location != null && "file".equals(location.getProtocol())) {
        return new IOFile(Paths.get(location.toURI()).toString()).dir();
      }
    } catch(final Exception ex) {
      Util.stack(ex);
    }
    return null;
  }

  /**
   * Attaches a directory separator to the specified directory string.
   * @param path directory path
   * @return directory string
   */
  private static String dir(final String path) {
    return path.isEmpty() || Strings.endsWith(path, '/') || Strings.endsWith(path, '\\') ?
      path : path + File.separator;
  }

  /**
   * Sets a global option.
   * @param option option
   * @param value value
   */
  public static void put(final Option<?> option, final String value) {
    put(DBPREFIX + option.name().toLowerCase(Locale.ENGLISH), value);
  }

  /**
   * Sets a global option.
   * @param name name of the option
   * @param value value
   */
  public static void put(final String name, final String value) {
    OPTIONS.put(name, value);
  }

  /**
   * Removes all global options.
   */
  public static void clear() {
    OPTIONS.clear();
  }

  /**
   * Returns a system property or global option. System properties override global options.
   * @param name name of the option
   * @return global option
   */
  public static String get(final String name) {
    final String value = System.getProperty(name);
    return value != null ? value : OPTIONS.get(name);
  }

  /**
   * Returns all global options and system properties.
   * System properties override global options.
   * @return entry set
   */
  public static List<Entry<String, String>> entries() {
    // properties from starter classes and web.xml context parameters
    final HashMap<String, String> entries = new HashMap<>(OPTIONS);
    // override with system properties
    System.getProperties().forEach((key, value) -> entries.put(key.toString(), value.toString()));
    // return list sorted by key
    return entries.entrySet().stream().sorted(Entry.comparingByKey()).collect(Collectors.toList());
  }

  /**
   * Sets a system property if it has not been set before.
   * @param name name of the property
   * @param value value
   */
  public static void setSystem(final String name, final String value) {
    if(System.getProperty(name) == null) System.setProperty(name, value);
  }

  /**
   * Returns the attributes from the MANIFEST.MF file.
   * @return attributes, or empty attribute list if the file is not found or cannot be parsed
   */
  private static Attributes manifest() {
    if(LOCATION != null) {
      try {
        final String jar = LOCATION.getFile();
        final ClassLoader cl = Prop.class.getClassLoader();
        for(final URL url : Collections.list(cl.getResources("META-INF/MANIFEST.MF"))) {
          if(url.getFile().contains(jar)) {
            try(InputStream in = url.openStream()) {
              return new Manifest(in).getMainAttributes();
            }
          }
        }
      } catch(final IOException ex) {
        Util.stack(ex);
      }
    }
    return new Attributes(0);
  }
}

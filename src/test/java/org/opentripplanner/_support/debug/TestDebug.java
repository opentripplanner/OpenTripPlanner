package org.opentripplanner._support.debug;

/**
 * Sometimes it is convenient to include debug logging in a unit test. This class uses the
 * {@code System.err} for logging - to make sure the output is flushed. Using the standard logging
 * framework for this is a bit unnecessary. Use this class instead of System err/out.
 * <p>
 * To turn on debugging set {@code testDebug} as an environment variable or system property -
 * if set this class will print debug info to the console. Use:
 * <pre>
 * $ export testDebug=true | java ...
 * or
 * $ java -DtestDebug ..
 * </pre>
 * In IntelliJ I recommend adding the system property in the <em>JUnit template</em>. In the test
 * drop down, choose <b>Edit Configuration...</b> then <b>Edit Configuration Templates..</b> and
 * choose <b>JUnit</b>.
 */
public class TestDebug {

  private static Boolean enabled = enabled();

  /** This is a utility class - only static methods */
  private TestDebug() {}

  public static boolean on() {
    return enabled;
  }

  public static boolean off() {
    return !enabled;
  }

  public static void print(Object value) {
    if (enabled()) {
      System.err.print(value);
    }
  }

  public static void println() {
    if (enabled()) {
      System.err.println();
    }
  }

  public static void println(Object value) {
    if (enabled()) {
      System.err.println(value);
    }
  }

  private static boolean enabled() {
    boolean sysDebug = System.getProperties().containsKey("testDebug");
    boolean envDebug = System.getenv().containsKey("testDebug");
    return sysDebug || envDebug;
  }
}

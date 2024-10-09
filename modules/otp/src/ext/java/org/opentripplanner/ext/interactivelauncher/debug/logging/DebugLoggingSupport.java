package org.opentripplanner.ext.interactivelauncher.debug.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.LoggerFactory;

/**
 * Responsible for integration with the OTP Debug log configuration, reading loggers from the slf4j
 * context and setting DEBUG level on selected loggers back.
 * <p>
 * The log names are transformed to be more human-readable:
 * <pre>org.opentripplanner.routing.algorithm  -->  o.o.routing.algorithm</pre>
 */
class DebugLoggingSupport {

  private static final String OTP = Pattern.quote("org.opentripplanner.") + ".*";
  private static final String GRAPHQL = Pattern.quote("fea");
  private static final String NAMED_LOGGERS = Pattern.quote("[A-Z0-9_]*");

  private static final Pattern LOG_MATCHER_PATTERN = Pattern.compile(
    "(" + OTP + "|" + GRAPHQL + "|" + NAMED_LOGGERS + ")"
  );

  static List<String> listConfiguredDebugLoggers() {
    List<String> result = new ArrayList<>();
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    for (Logger log : context.getLoggerList()) {
      var name = log.getName();
      if (name.equals("ROOT") || log.getLevel() == null) {
        continue;
      }
      if (log.getLevel().toInt() <= Level.DEBUG.toInt()) {
        if (LOG_MATCHER_PATTERN.matcher(name).matches()) {
          result.add(name);
        }
      }
    }
    return result;
  }

  static void configureDebugLogging(String logger, boolean debug) {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    var log = context.getLogger(logger);
    log.setLevel(debug ? Level.DEBUG : Level.INFO);
  }
}

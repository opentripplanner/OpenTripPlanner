package org.opentripplanner.ext.interactivelauncher;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.LoggerFactory;

/**
 * Responsible for integration with the OTP Debug log configuraton, reading loggers from the slf4j
 * context and setting DEBUG level on selected loggers back.
 * <p>
 * The log names are transformed to be more human readable:
 * <pre>org.opentripplanner.routing.algorithm  -->  o.o.routing.algorithm</pre>
 */
public class DebugLoggingSupport {

    public static List<String> getLogs() {
        List<String> result = new ArrayList<>();
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        for (Logger log : context.getLoggerList()) {
            var name = log.getName();
            if (
                    name.matches("(org\\.opentripplanner\\..*|[A-Z0-9_]*)")
                    && !name.equals("ROOT")
            ) {
                result.add(logDisplayName(name));
            }
        }
        return result;
    }


    public static void configureDebugLogging(Map<String, Boolean> loggers) {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        for (Logger log : context.getLoggerList()) {
            var name = logDisplayName(log.getName());
            if(loggers.getOrDefault(name, false))  {
                log.setLevel(Level.DEBUG);
            }
        }
    }

    private static String logDisplayName(String name) {
        return name.replace("org.opentripplanner.", "o.o.");
    }
}

package org.opentripplanner.standalone.configure;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestLoggerFactory {

  /**
   * Programmatically (i.e. not in XML) create a Logback logger for routing requests.
   * See http://stackoverflow.com/a/17215011/778449
   */
  public static Logger createLogger(@Nullable String filename) {
    if (filename == null) {
      return null;
    }

    LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
    PatternLayoutEncoder ple = new PatternLayoutEncoder();
    ple.setPattern("%d{yyyy-MM-dd'T'HH:mm:ss.SSS} %msg%n");
    ple.setContext(lc);
    ple.start();
    FileAppender<ILoggingEvent> fileAppender = new FileAppender<>();
    fileAppender.setFile(filename);
    fileAppender.setEncoder(ple);
    fileAppender.setContext(lc);
    fileAppender.start();
    ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(
      "REQ_LOG"
    );
    logger.addAppender(fileAppender);
    logger.setLevel(Level.INFO);
    logger.setAdditive(false);
    return logger;
  }
}

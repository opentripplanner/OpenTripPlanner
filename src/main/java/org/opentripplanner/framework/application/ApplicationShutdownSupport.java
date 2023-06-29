package org.opentripplanner.framework.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for managing application shutdown.
 */
public final class ApplicationShutdownSupport {

  private static final Logger LOG = LoggerFactory.getLogger(ApplicationShutdownSupport.class);

  private ApplicationShutdownSupport() {}

  /**
   * Attempt to add a shutdown hook. If the application is already shutting down, the shutdown hook
   * will not be added.
   *
   * @return true if the shutdown hook is successfully added, false otherwise.
   */
  public static boolean addShutdownHook(Thread shutdownHook, String shutdownHookName) {
    try {
      LOG.info("Adding shutdown hook {}", shutdownHookName);
      Runtime.getRuntime().addShutdownHook(shutdownHook);
      return true;
    } catch (IllegalStateException ignore) {
      LOG.info(
        "OTP is already shutting down, the shutdown hook {} will not be added",
        shutdownHookName
      );
      return false;
    }
  }
}

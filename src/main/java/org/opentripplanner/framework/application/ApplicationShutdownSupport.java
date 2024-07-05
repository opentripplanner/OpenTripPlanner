package org.opentripplanner.framework.application;

import java.util.Optional;
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
   * will be executed immediately.
   *
   * @param hookName the name of the thread
   * @param shutdownHook the payload to be executed in the thread
   * @return an Optional possibly containing the created thread, needed to un-schedule the shutdown hook
   */
  public static Optional<Thread> addShutdownHook(String hookName, Runnable shutdownHook) {
    final Thread shutdownThread = new Thread(shutdownHook, hookName);
    try {
      LOG.info("Adding shutdown hook '{}'.", hookName);
      Runtime.getRuntime().addShutdownHook(shutdownThread);
      return Optional.of(shutdownThread);
    } catch (IllegalStateException ignore) {
      LOG.info("OTP is already shutting down, running shutdown hook '{}' immediately.", hookName);
      shutdownThread.start();
    }
    return Optional.empty();
  }

  /**
   * Remove a previously scheduled shutdown hook.
   *
   * @param shutdownThread an Optional possibly containing a thread
   */
  public static void removeShutdownHook(Thread shutdownThread) {
    LOG.info("Removing shutdown hook '{}'.", shutdownThread.getName());
    Runtime.getRuntime().removeShutdownHook(shutdownThread);
  }
}

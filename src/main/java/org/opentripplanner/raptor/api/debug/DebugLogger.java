package org.opentripplanner.raptor.api.debug;

/**
 * The use of the API should provide a debug logger which map to what ever logging api the caller
 * use.
 */
@FunctionalInterface
public interface DebugLogger {
  static DebugLogger noop() {
    return (topic, message) -> {};
  }

  /**
   * Check if debugging is enabled before doing heavy work like calculating statistics before
   * logging it.
   * <p/>
   * PLEASE IMPLEMENT THIS AND RETURN TRUE TO ENABLE DEBUGGING.
   */
  default boolean isEnabled() {
    return false;
  }

  /**
   * Prepare the debug logger for searching direction FORWARD or REVERSE. This method is optional to
   * implement, the default do nothing.
   * <p>
   * The method is called once before each search begin.
   */
  default void setSearchDirection(boolean forward) {
    // do nothing
  }

  /**
   * Implement this method to provide logging.
   */
  void debug(DebugTopic topic, String message);

  /**
   * Handy method which uses the Java String#format to format the message.
   */
  default void debug(DebugTopic topic, String format, Object... args) {
    if (isEnabled()) {
      debug(topic, String.format(format, args));
    }
  }
}

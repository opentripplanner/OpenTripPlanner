package org.opentripplanner.transit.raptor.api.debug;


/**
 * The use of the API should provide a debug logger witch map to what ever logging api
 * the caller use.
 */
@FunctionalInterface
public interface DebugLogger {
    /**
     * Check if debugging is enabled before doing heavy work like calculating statistics
     * before logging it.
     * <p/>
     * PLEASE IMPLEMENT THIS AND RETURN TRUE TO ENABLE DEBUGGING.
     */
    default boolean isEnabled(DebugTopic topic) {
        return false;
    }

    /**
     * Implement this method to provide logging.
     */
    void debug(DebugTopic topic, String message);

    /**
     * Handy method witch uses the Java String#format to format the message.
     */
    default void debug(DebugTopic topic, String format, Object... args) {
        if(isEnabled(topic)) {
            debug(topic, String.format(format, args));
        }
    }
}

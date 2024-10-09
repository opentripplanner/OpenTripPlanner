package org.opentripplanner.framework.application;

import java.util.Map;
import javax.annotation.Nullable;
import org.opentripplanner.framework.lang.StringUtils;
import org.slf4j.MDC;

/**
 * This class is used to copy the log MDC(Mapped Diagnostic Context) from a parent thread to
 * its children. The slf4j Mapped Diagnostic Context uses the thread local to store
 * context properties like a correlation-id or http request trace information.
 */
public class LogMDCSupport {

  private static boolean enabled = false;

  /** private constructor to prevent creating new instances */
  private LogMDCSupport() {}

  /**
   * Enable the parent to child thread propagation of the log MDC.
   */
  public static void enable() {
    LogMDCSupport.enabled = true;
  }

  public static boolean isRequestTracingInLoggingEnabled() {
    return LogMDCSupport.enabled;
  }

  /**
   * Put key/value into the thread local MDC context map. If the value is {@code null}/empty, then
   * the key is removed(if it exists).
   */
  public static void putLocal(String key, String value) {
    if (enabled && StringUtils.hasValue(key)) {
      if (StringUtils.hasValue(value)) {
        MDC.put(key, value);
      } else {
        MDC.remove(key);
      }
    }
  }

  /**
   * Remove a log key from the thread local log context map. If the key is {@code null}/empty or
   * does not exist in the log context, then nothing happens and the method returns.
   */
  public static void removeLocal(String key) {
    if (enabled && StringUtils.hasValue(key)) {
      MDC.remove(key);
    }
  }

  /**
   * Get value from local thread log context.
   */
  public static String getLocalValue(String logKey) {
    return MDC.get(logKey);
  }

  /**
   * Get the MDC thread local context map.
   */
  public static Map<String, String> getContext() {
    return enabled ? MDC.getCopyOfContextMap() : Map.of();
  }

  /**
   * Set MDC thread local context map using the given {@code parentContextMap}.
   */
  public static void setLocal(@Nullable Map<String, String> parentContextMap) {
    if (enabled && parentContextMap != null && !parentContextMap.isEmpty()) {
      MDC.setContextMap(parentContextMap);
    }
  }

  /**
   * Clear the local MDC context map - thread is done, no more logging?
   */
  public static void clearLocal() {
    if (enabled) {
      MDC.clear();
    }
  }
}

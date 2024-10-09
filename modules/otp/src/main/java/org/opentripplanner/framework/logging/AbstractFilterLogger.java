package org.opentripplanner.framework.logging;

import org.slf4j.Logger;
import org.slf4j.Marker;

/**
 * This class can be used to filter logging events with level:
 * <ul>
 *     <li>INFO</li>
 *     <li>WARNING</li>
 *     <li>ERROR</li>
 * </ul>
 * DEBUG and TRACE events are not filtered.
 * <p>
 * The primary use-case for this class is to prevent a spamming the log with the same kind
 * of events. There are two concrete implementations:
 * <ul>
 *     <li>{@link MaxCountLogger} - Log N events, then mute. This is suitable for data import.</li>
 * </ul>
 *
 * <p>
 * This class wrap the original logger and forward some log events, based on the
 * implementation of the {@link #mute()} method.
 * <p>
 * @deprecated This hide the actual logger in the log, the AbstractFilterLogger becomes thelogger -
               this make it difficult to find the log statement in the code when  investigating.
 */
@Deprecated
public abstract class AbstractFilterLogger implements Logger {

  private final Logger delegate;

  AbstractFilterLogger(Logger delegate) {
    this.delegate = delegate;
  }

  @Override
  public String getName() {
    return delegate.getName();
  }

  @Override
  public boolean isTraceEnabled() {
    return delegate.isTraceEnabled();
  }

  @Override
  public void trace(String msg) {
    delegate.trace(msg);
  }

  @Override
  public void trace(String format, Object arg) {
    delegate.trace(format, arg);
  }

  @Override
  public void trace(String format, Object arg1, Object arg2) {
    delegate.trace(format, arg1, arg2);
  }

  @Override
  public void trace(String format, Object... arguments) {
    delegate.trace(format, arguments);
  }

  @Override
  public void trace(String msg, Throwable t) {
    delegate.trace(msg, t);
  }

  @Override
  public boolean isTraceEnabled(Marker marker) {
    return delegate.isTraceEnabled(marker);
  }

  @Override
  public void trace(Marker marker, String msg) {
    delegate.trace(marker, msg);
  }

  @Override
  public void trace(Marker marker, String format, Object arg) {
    delegate.trace(marker, format, arg);
  }

  @Override
  public void trace(Marker marker, String format, Object arg1, Object arg2) {
    delegate.trace(marker, format, arg1, arg2);
  }

  @Override
  public void trace(Marker marker, String format, Object... argArray) {
    delegate.trace(marker, format, argArray);
  }

  @Override
  public void trace(Marker marker, String msg, Throwable t) {
    delegate.trace(marker, msg, t);
  }

  @Override
  public boolean isDebugEnabled() {
    return delegate.isDebugEnabled();
  }

  @Override
  public void debug(String msg) {
    delegate.debug(msg);
  }

  @Override
  public void debug(String format, Object arg) {
    delegate.debug(format, arg);
  }

  @Override
  public void debug(String format, Object arg1, Object arg2) {
    delegate.debug(format, arg1, arg2);
  }

  @Override
  public void debug(String format, Object... arguments) {
    delegate.debug(format, arguments);
  }

  @Override
  public void debug(String msg, Throwable t) {
    delegate.debug(msg, t);
  }

  @Override
  public boolean isDebugEnabled(Marker marker) {
    return delegate.isTraceEnabled(marker);
  }

  @Override
  public void debug(Marker marker, String msg) {
    delegate.debug(marker, msg);
  }

  @Override
  public void debug(Marker marker, String format, Object arg) {
    delegate.debug(marker, format, arg);
  }

  @Override
  public void debug(Marker marker, String format, Object arg1, Object arg2) {
    delegate.debug(marker, format, arg1, arg2);
  }

  @Override
  public void debug(Marker marker, String format, Object... arguments) {
    delegate.debug(marker, format, arguments);
  }

  @Override
  public void debug(Marker marker, String msg, Throwable t) {
    delegate.debug(marker, msg, t);
  }

  @Override
  public boolean isInfoEnabled() {
    return delegate.isInfoEnabled();
  }

  @Override
  public void info(String msg) {
    if (mute()) {
      return;
    }
    delegate.info(msg);
  }

  @Override
  public void info(String format, Object arg) {
    if (mute()) {
      return;
    }
    delegate.info(format, arg);
  }

  @Override
  public void info(String format, Object arg1, Object arg2) {
    if (mute()) {
      return;
    }
    delegate.info(format, arg1, arg2);
  }

  @Override
  public void info(String format, Object... arguments) {
    if (mute()) {
      return;
    }
    delegate.info(format, arguments);
  }

  @Override
  public void info(String msg, Throwable t) {
    if (mute()) {
      return;
    }
    delegate.info(msg, t);
  }

  @Override
  public boolean isInfoEnabled(Marker marker) {
    return delegate.isInfoEnabled(marker);
  }

  @Override
  public void info(Marker marker, String msg) {
    if (mute()) {
      return;
    }
    delegate.info(marker, msg);
  }

  @Override
  public void info(Marker marker, String format, Object arg) {
    if (mute()) {
      return;
    }
    delegate.info(marker, format, arg);
  }

  @Override
  public void info(Marker marker, String format, Object arg1, Object arg2) {
    if (mute()) {
      return;
    }
    delegate.info(marker, format, arg1, arg2);
  }

  @Override
  public void info(Marker marker, String format, Object... arguments) {
    if (mute()) {
      return;
    }
    delegate.info(marker, format, arguments);
  }

  @Override
  public void info(Marker marker, String msg, Throwable t) {
    if (mute()) {
      return;
    }
    delegate.info(marker, msg, t);
  }

  @Override
  public boolean isWarnEnabled() {
    return delegate.isWarnEnabled();
  }

  @Override
  public void warn(String msg) {
    if (mute()) {
      return;
    }
    delegate.warn(msg);
  }

  @Override
  public void warn(String format, Object arg) {
    if (mute()) {
      return;
    }
    delegate.warn(format, arg);
  }

  @Override
  public void warn(String format, Object... arguments) {
    if (mute()) {
      return;
    }
    delegate.warn(format, arguments);
  }

  @Override
  public void warn(String format, Object arg1, Object arg2) {
    if (mute()) {
      return;
    }
    delegate.warn(format, arg1, arg2);
  }

  @Override
  public void warn(String msg, Throwable t) {
    if (mute()) {
      return;
    }
    delegate.warn(msg, t);
  }

  @Override
  public boolean isWarnEnabled(Marker marker) {
    return delegate.isWarnEnabled(marker);
  }

  @Override
  public void warn(Marker marker, String msg) {
    if (mute()) {
      return;
    }
    delegate.warn(marker, msg);
  }

  @Override
  public void warn(Marker marker, String format, Object arg) {
    if (mute()) {
      return;
    }
    delegate.warn(marker, format, arg);
  }

  @Override
  public void warn(Marker marker, String format, Object arg1, Object arg2) {
    if (mute()) {
      return;
    }
    delegate.warn(marker, format, arg1, arg2);
  }

  @Override
  public void warn(Marker marker, String format, Object... arguments) {
    if (mute()) {
      return;
    }
    delegate.warn(marker, format, arguments);
  }

  @Override
  public void warn(Marker marker, String msg, Throwable t) {
    if (mute()) {
      return;
    }
    delegate.warn(marker, msg, t);
  }

  @Override
  public boolean isErrorEnabled() {
    return delegate.isErrorEnabled();
  }

  @Override
  public void error(String msg) {
    if (mute()) {
      return;
    }
    delegate.error(msg);
  }

  @Override
  public void error(String format, Object arg) {
    if (mute()) {
      return;
    }
    delegate.error(format, arg);
  }

  @Override
  public void error(String format, Object arg1, Object arg2) {
    if (mute()) {
      return;
    }
    delegate.error(format, arg1, arg2);
  }

  @Override
  public void error(String format, Object... arguments) {
    if (mute()) {
      return;
    }
    delegate.error(format, arguments);
  }

  @Override
  public void error(String msg, Throwable t) {
    if (mute()) {
      return;
    }
    delegate.error(msg, t);
  }

  @Override
  public boolean isErrorEnabled(Marker marker) {
    return delegate.isErrorEnabled(marker);
  }

  @Override
  public void error(Marker marker, String msg) {
    if (mute()) {
      return;
    }
    delegate.error(marker, msg);
  }

  @Override
  public void error(Marker marker, String format, Object arg) {
    if (mute()) {
      return;
    }
    delegate.error(marker, format, arg);
  }

  @Override
  public void error(Marker marker, String format, Object arg1, Object arg2) {
    if (mute()) {
      return;
    }
    delegate.error(marker, format, arg1, arg2);
  }

  @Override
  public void error(Marker marker, String format, Object... arguments) {
    if (mute()) {
      return;
    }
    delegate.error(marker, format, arguments);
  }

  @Override
  public void error(Marker marker, String msg, Throwable t) {
    if (mute()) {
      return;
    }
    delegate.error(marker, msg, t);
  }

  /**
   * This method is called from all {@code info, warn, and error}, if it return {@code true} the log
   * event is muted, if not the event is logged.
   */
  abstract boolean mute();

  Logger getDelegate() {
    return delegate;
  }
}

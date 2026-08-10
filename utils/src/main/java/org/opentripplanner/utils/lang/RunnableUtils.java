package org.opentripplanner.utils.lang;

/**
 * Utilities extending Java {@link Runnable} features.
 */
public class RunnableUtils {

  private RunnableUtils() {}

  /**
   * A runnable that does nothing.
   */
  public static final Runnable NOOP = () -> {};
}

package org.opentripplanner.raptor.spi;

/**
 * This enum describe the direction which a search is performed.
 * <p>
 * The normal way is to search {@link #FORWARD} from the origin to the destination.
 * <p>
 * Set search direction to {@link #REVERSE} to performed a search from the destination to the
 * origin. This will traverse the transit graph backwards in time. This is used in Raptor to produce
 * heuristics, and is normally not something you would like to do unless you are testing or
 * analyzing. This should not be confused with <em>Range Raptor iterations</em>> which
 * step-backward-in-time (start with the last minute of the search window), but searches {@link
 * #FORWARD}. {@link #REVERSE} search is supported by the current implementation of RangeRaptor.
 * <p>
 * In the Raptor code we will refer to the origin and and destination assuming the search direction
 * is {@link #FORWARD}.
 */
public enum SearchDirection {
  /**
   * Search from origin to destination, forward in time.
   */
  FORWARD,

  /**
   * Search from destination to origin, backward in time.
   */
  REVERSE;

  public boolean isForward() {
    return this == FORWARD;
  }

  public boolean isInReverse() {
    return this == REVERSE;
  }
}

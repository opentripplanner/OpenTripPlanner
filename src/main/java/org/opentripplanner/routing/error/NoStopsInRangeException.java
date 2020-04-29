package org.opentripplanner.routing.error;

import java.util.List;

/**
 * Indicates that a vertex was found, but no stops could be found within the search radius.
 */
public class NoStopsInRangeException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  List<String> missing;

  public NoStopsInRangeException(List<String> missing) {
    super("vertices not found: " + missing.toString());
    this.missing = missing;
  }

  public List<String> getMissing() {
    return missing;
  }
}

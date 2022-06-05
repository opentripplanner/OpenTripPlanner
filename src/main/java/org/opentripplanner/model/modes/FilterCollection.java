package org.opentripplanner.model.modes;

import java.util.Collection;
import java.util.List;
import org.opentripplanner.transit.model.network.SubMode;
import org.opentripplanner.transit.model.network.TransitMode;

/**
 * This classes is used to combine more than one filter into one. It keep a list of filters and
 * iterate over them in tha same order as they are added.
 */
public class FilterCollection implements AllowTransitModeFilter {

  /**
   * Note! We use a lits for fast iteratin, the performance overhead of using
   * e.g. a Set here is significant. A test performed on the Norwegian dataset
   * showed an increase in in pattern filtering time from ~25 ms to ~40 ms for
   * List versus Set.
   */
  private final List<AllowTransitModeFilter> filters;

  public FilterCollection(Collection<AllowTransitModeFilter> filters) {
    this.filters = List.copyOf(filters);
  }

  @Override
  public boolean allows(TransitMode transitMode, SubMode netexSubMode) {
    // Performance is important here, do not use streams
    for (AllowTransitModeFilter it : filters) {
      if (it.allows(transitMode, netexSubMode)) {
        return true;
      }
    }
    return false;
  }
}

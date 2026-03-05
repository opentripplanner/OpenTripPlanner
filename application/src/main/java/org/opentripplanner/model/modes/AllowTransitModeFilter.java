package org.opentripplanner.model.modes;

import java.io.Serializable;
import java.util.Collection;
import org.opentripplanner.transit.model.basic.MainAndSubMode;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;

/**
 * Used to filter out modes for routing requests.
 */
public interface AllowTransitModeFilter extends Serializable {
  static AllowTransitModeFilter of(Collection<MainAndSubMode> modes) {
    return FilterFactory.create(modes);
  }

  /**
   * Check if this filter allows the provided TransitMode
   */
  boolean match(TransitMode transitMode, SubMode netexSubMode);

  /**
   * Returns {@code true} if this filter is selective about which modes it allows, i.e. it does
   * not accept all modes. This is used to determine whether trip-level filtering is needed for
   * {@link org.opentripplanner.transit.model.network.TripPattern}s that contain trips with
   * different modes or submodes. For such multi-mode patterns, pattern-level filtering alone is
   * insufficient and each trip must be checked individually against the mode filter.
   * <p>
   * The default is {@code false}, which is appropriate for filters that accept all modes (like
   * {@link AllowAllModesFilter}). Selective filters should override this to return {@code true}.
   */
  default boolean isModeSelective() {
    return false;
  }
}

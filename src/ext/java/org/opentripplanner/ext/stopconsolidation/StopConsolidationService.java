package org.opentripplanner.ext.stopconsolidation;

import java.util.List;
import java.util.Optional;
import org.opentripplanner.ext.stopconsolidation.model.StopReplacement;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.site.StopLocation;

public interface StopConsolidationService {
  /**
   * A flat list of pairs of stops that should be replaced.
   */
  List<StopReplacement> replacements();

  /**
   * Returns the list of secondary stops that need to be replaced in TripPatterns with their
   * primary equivalent.
   */
  List<FeedScopedId> secondaryStops();

  /**
   * Is the given stop a primary stop as defined by the stop consolidation configuration?
   */
  boolean isPrimaryStop(StopLocation stop);

  /**
   * Is the given stop a secondary stop as defined by the stop consolidation configuration?
   */
  boolean isSecondaryStop(StopLocation stop);

  /**
   * Are any stop consolidations defined?
   */
  boolean isActive();

  /**
   * For a given primary stop look up secondary feed as it was originally defined in the agency's feed.
   */
  StopLocation agencySpecificStop(StopLocation stop, Agency agency);

  /**
   * For a given stop id return the primary stop if it is part of a consolidated stop group.
   */
  Optional<StopLocation> primaryStop(FeedScopedId id);
}

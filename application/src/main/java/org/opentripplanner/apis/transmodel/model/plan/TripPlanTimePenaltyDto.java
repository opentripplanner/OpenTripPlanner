package org.opentripplanner.apis.transmodel.model.plan;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.opentripplanner.framework.model.TimeAndCost;
import org.opentripplanner.model.plan.Itinerary;

/**
 * A simple data-transfer-object used to map from an itinerary to the API specific
 * type. It is needed because we need to pass in the "appliedTo" field, which does not
 * exist in the domain model.
 */
public record TripPlanTimePenaltyDto(String appliesTo, TimeAndCost penalty) {
  static List<TripPlanTimePenaltyDto> of(Itinerary itinerary) {
    return Stream.of(
      of("access", itinerary.accessPenalty()),
      of("egress", itinerary.egressPenalty())
    )
      .filter(Objects::nonNull)
      .toList();
  }

  /**
   * Package local to be unit-testable.
   */
  @Nullable
  static TripPlanTimePenaltyDto of(String appliedTo, TimeAndCost penalty) {
    return penalty == null || penalty.isZero()
      ? null
      : new TripPlanTimePenaltyDto(appliedTo, penalty);
  }
}

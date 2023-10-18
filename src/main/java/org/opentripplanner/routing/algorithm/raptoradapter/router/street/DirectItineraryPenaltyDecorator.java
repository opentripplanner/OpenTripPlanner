package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import java.util.List;
import java.util.Objects;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.StreetLegBuilder;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.framework.TimeAndCostPenaltyForEnum;

public class DirectItineraryPenaltyDecorator {

  private final StreetMode directMode;
  private final TimeAndCostPenaltyForEnum<StreetMode> penalty;

  public DirectItineraryPenaltyDecorator(
    StreetMode directMode,
    TimeAndCostPenaltyForEnum<StreetMode> penalty
  ) {
    Objects.requireNonNull(directMode);
    Objects.requireNonNull(penalty);
    this.directMode = directMode;
    this.penalty = penalty;
  }

  public List<Itinerary> applyPenalty(List<Itinerary> itineraries) {
    return itineraries
      .stream()
      .map(itin -> {
        if (itin.hasTransit()) {
          throw new IllegalArgumentException(
            "The itinerary %s contains transit legs. No direct penalty can be applied.".formatted(
                itin
              )
          );
        }
        var penalty = this.penalty.valueOf(directMode);
        if (!penalty.isEmpty()) {
          itin.transformStreetLegs(sl ->
            StreetLegBuilder
              .of(sl)
              .withGeneralizedCost((int) (sl.getGeneralizedCost() * penalty.costFactor()))
              .build()
          );

          var cost = itin.getLegs().stream().mapToInt(Leg::getGeneralizedCost).sum();

          itin.setGeneralizedCost(cost);
        }
        return itin;
      })
      .toList();
  }
}

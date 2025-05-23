package org.opentripplanner.ext.fares.impl;

import java.time.Duration;
import java.util.Collection;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import org.opentripplanner.ext.fares.model.FareRuleSet;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.model.fare.ItineraryFare;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.leg.ScheduledTransitLeg;
import org.opentripplanner.routing.core.FareType;
import org.opentripplanner.transit.model.basic.Money;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * This calculator is maintained by IBI Group.
 */
public class HighestFareInFreeTransferWindowFareService extends DefaultFareService {

  private final boolean analyzeInterlinedTransfers;
  private final Duration freeTransferWindow;

  public HighestFareInFreeTransferWindowFareService(
    Collection<FareRuleSet> regularFareRules,
    Duration freeTransferWindow,
    boolean analyzeInterlinedTransfers
  ) {
    addFareRules(FareType.regular, regularFareRules);
    this.freeTransferWindow = freeTransferWindow;
    this.analyzeInterlinedTransfers = analyzeInterlinedTransfers;
  }

  public boolean analyzeInterlinedTransfers() {
    return analyzeInterlinedTransfers;
  }

  public Duration freeTransferWindow() {
    return freeTransferWindow;
  }

  /**
   * The fare calculation is designed to charge the rider incrementally as they use each service.
   * The total cost of the itinerary will be equal to the leg of the journey that had the maximum
   * fare. Additionally, a free transfer window is taken into account that will apply the current
   * highest cost within the free transfer window and then reset to a new window and calculate
   * additional free transfers from there.
   */
  @Override
  protected ItineraryFare calculateFaresForType(
    Currency currency,
    FareType fareType,
    List<Leg> legs,
    Collection<FareRuleSet> fareRules
  ) {
    var zero = Money.ofFractionalAmount(currency, 0);
    Money cost = zero;
    Money currentTransferWindowCost = zero;
    // The initial value of -1 indicates that the free transfer window end time has not yet been set
    long freeTransferWindowEndTimeEpochSeconds = -1;
    for (var leg : legs) {
      Optional<Money> rideCost = calculateCost(fareType, List.of(leg), fareRules);

      if (leg.startTime().toEpochSecond() > freeTransferWindowEndTimeEpochSeconds) {
        // free transfer window has expired or has not yet been initialized. Reset some items and add to the
        // overall cost. This is fine to do if the free transfer window hasn't been initialized since the
        // overall cost will be 0.
        cost = cost.plus(currentTransferWindowCost);
        // reset current window cost
        currentTransferWindowCost = zero;
        // reset transfer window end time to trigger recalculation in next block
        freeTransferWindowEndTimeEpochSeconds = -1;
      }

      // recalculate free transfer window if needed
      if (freeTransferWindowEndTimeEpochSeconds == -1) {
        // the new transfer window end time should be calculated by adding the ride's start time (which is in
        // seconds past the epoch) and the number of equivalent seconds in the free transfer window minutes.
        freeTransferWindowEndTimeEpochSeconds = leg
          .startTime()
          .plus(freeTransferWindow)
          .toEpochSecond();
      }

      currentTransferWindowCost = Money.max(currentTransferWindowCost, rideCost.orElse(zero));
    }
    cost = cost.plus(currentTransferWindowCost);
    var fp = FareProduct.of(
      new FeedScopedId("fares", fareType.name()),
      fareType.name(),
      cost
    ).build();
    var fare = ItineraryFare.empty();
    if (cost.greaterThan(zero)) {
      fare.addItineraryProducts(List.of(fp));
    }
    return fare;
  }

  @Override
  protected boolean shouldCombineInterlinedLegs(
    ScheduledTransitLeg current,
    ScheduledTransitLeg previous
  ) {
    return !analyzeInterlinedTransfers;
  }
}

package org.opentripplanner.ext.stopconsolidation;

import java.util.ArrayList;
import java.util.Objects;
import org.opentripplanner.ext.stopconsolidation.model.ConsolidatedStopLeg;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.ItineraryBuilder;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.leg.ScheduledTransitLeg;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.ItineraryDecorator;

/**
 * A decorating filter that checks if a transit leg contains any consolidated stops and if it does,
 * then replaces it with the appropriate, agency-specific stop name. This is so that the physical
 * signage and in-vehicle display matches what OTP returns as a board/alight stop name.
 *
 * TODO: Split removing short legs out of this clas, even if it is a Sandbox feature the filter
 *       contract is broken.
 */
public class DecorateConsolidatedStopNames implements ItineraryDecorator {

  private static final int MAX_INTRA_STOP_WALK_DISTANCE_METERS = 15;
  private final StopConsolidationService service;

  public DecorateConsolidatedStopNames(StopConsolidationService service) {
    this.service = Objects.requireNonNull(service);
  }

  @Override
  public Itinerary decorate(Itinerary itinerary) {
    var builder = itinerary.copyOf();
    replaceConsolidatedStops(builder);
    removeShortWalkLegs(builder);
    return builder.build();
  }

  /**
   * If the itinerary has a "from" stop that is the secondary stop of a
   * {@link org.opentripplanner.ext.stopconsolidation.model.ConsolidatedStopGroup}
   * then we replace its name with the primary name of the agency that is
   * operating the route, so that the name in the result matches the physical signage on the stop.
   * <p>
   * If the leg has a "to" stop that is a primary stop, then we don't want to show the stop that's on
   * the signage but what is shown _inside_ the vehicle. That's why we use the agency-specific (aka
   * secondary) stop.
   * <p>
   * This follows the somewhat idiosyncratic logic of the consolidated stops feature.
   */
  private void replaceConsolidatedStops(ItineraryBuilder builder) {
    builder.transformTransitLegs(leg -> {
      if (leg instanceof ScheduledTransitLeg stl && needsToRenameStops(stl)) {
        var agency = leg.agency();
        // to show the name on the stop signage we use the primary stop's name
        var from = service.primaryStop(stl.from().stop.getId()).orElse(stl.from().stop);
        // to show the name that's on the display inside the vehicle we use the agency-specific name
        var to = service.agencySpecificStop(stl.to().stop, agency);
        return ConsolidatedStopLeg.of(stl).withFrom(from).withTo(to).build();
      } else {
        return leg;
      }
    });
  }

  /**
   * Removes walk legs from and to a consolidated stop if they are deemed "short". This means that
   * they are from a different element of the consolidated stop.
   */
  private void removeShortWalkLegs(ItineraryBuilder builder) {
    var legs = new ArrayList<>(builder.legs());
    var first = legs.getFirst();
    if (service.isPartOfConsolidatedStop(first.to().stop) && isShortWalkLeg(first)) {
      legs.removeFirst();
    }
    var last = legs.getLast();
    if (service.isPartOfConsolidatedStop(last.from().stop) && isShortWalkLeg(last)) {
      legs.removeLast();
    }
    var filteredLegs = legs.stream().filter(l -> !isTransferWithinConsolidatedStop(l)).toList();
    builder.withLegs(filteredLegs);
  }

  private boolean isTransferWithinConsolidatedStop(Leg l) {
    return (
      isShortWalkLeg(l) &&
      service.isPartOfConsolidatedStop(l.from().stop) &&
      service.isPartOfConsolidatedStop(l.to().stop)
    );
  }

  private static boolean isShortWalkLeg(Leg leg) {
    return leg.isWalkingLeg() && leg.distanceMeters() < MAX_INTRA_STOP_WALK_DISTANCE_METERS;
  }

  /**
   * Figures out if the from/to stops are part of a consolidated stop group and therefore
   * some stops need to be replaced.
   * <p>
   * Please consult the Javadoc of {@link DecorateConsolidatedStopNames#replaceConsolidatedStops(Itinerary)}
   * for details of this idiosyncratic business logic and in particular why the logic is not the same
   * for the from/to stops.
   */
  private boolean needsToRenameStops(ScheduledTransitLeg stl) {
    return (service.isSecondaryStop(stl.from().stop) || service.isPrimaryStop(stl.to().stop));
  }
}

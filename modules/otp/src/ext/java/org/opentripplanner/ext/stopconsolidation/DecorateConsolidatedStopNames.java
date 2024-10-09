package org.opentripplanner.ext.stopconsolidation;

import java.util.Objects;
import org.opentripplanner.ext.stopconsolidation.model.ConsolidatedStopLeg;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.ScheduledTransitLeg;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.ItineraryDecorator;

/**
 * A decorating filter that checks if a transit leg contains any consolidated stops and if it does,
 * then replaces it with the appropriate, agency-specific stop name. This is so that the physical
 * signage and in-vehicle display matches what OTP returns as a board/alight stop name.
 */
public class DecorateConsolidatedStopNames implements ItineraryDecorator {

  private final StopConsolidationService service;

  public DecorateConsolidatedStopNames(StopConsolidationService service) {
    this.service = Objects.requireNonNull(service);
  }

  @Override
  public void decorate(Itinerary itinerary) {
    replaceConsolidatedStops(itinerary);
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
  private void replaceConsolidatedStops(Itinerary i) {
    i.transformTransitLegs(leg -> {
      if (leg instanceof ScheduledTransitLeg stl && needsToRenameStops(stl)) {
        var agency = leg.getAgency();
        // to show the name on the stop signage we use the primary stop's name
        var from = service.primaryStop(stl.getFrom().stop.getId()).orElse(stl.getFrom().stop);
        // to show the name that's on the display inside the vehicle we use the agency-specific name
        var to = service.agencySpecificStop(stl.getTo().stop, agency);
        return new ConsolidatedStopLeg(stl, from, to);
      } else {
        return leg;
      }
    });
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
    return (service.isSecondaryStop(stl.getFrom().stop) || service.isPrimaryStop(stl.getTo().stop));
  }
}

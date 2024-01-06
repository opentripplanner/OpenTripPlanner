package org.opentripplanner.ext.stopconsolidation;

import java.util.Objects;
import org.opentripplanner.ext.stopconsolidation.model.ConsolidatedStopLeg;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.ScheduledTransitLeg;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.ItineraryDecorator;

/**
 * A decorating filter that checks if a transit leg contains any primary stops and if it does,
 * then replaces it with the secondary, agency-specific stop name. This is so that the in-vehicle
 * display matches what OTP returns as a board/alight stop name.
 */
public class DecorateConsolidatedStopNames implements ItineraryDecorator {

  private final StopConsolidationService service;

  public DecorateConsolidatedStopNames(StopConsolidationService service) {
    this.service = Objects.requireNonNull(service);
  }

  @Override
  public void decorate(Itinerary itinerary) {
    replacePrimaryNamesWithSecondary(itinerary);
  }

  /**
   * If the itinerary has a from/to that is the primary stop of a {@link org.opentripplanner.ext.stopconsolidation.model.ConsolidatedStopGroup}
   * then we replace its name with the secondary name of the agency that is
   * operating the route, so that the name in the result matches the name in the in-vehicle
   * display.
   */
  private void replacePrimaryNamesWithSecondary(Itinerary i) {
    i.transformTransitLegs(leg -> {
      if (leg instanceof ScheduledTransitLeg stl && needsToRenameStops(stl)) {
        var agency = leg.getAgency();
        return new ConsolidatedStopLeg(
          stl,
          service.agencySpecificName(stl.getFrom().stop, agency),
          service.agencySpecificName(stl.getTo().stop, agency)
        );
      } else {
        return leg;
      }
    });
  }

  private boolean needsToRenameStops(ScheduledTransitLeg stl) {
    return (service.isPrimaryStop(stl.getFrom().stop) || service.isPrimaryStop(stl.getTo().stop));
  }
}

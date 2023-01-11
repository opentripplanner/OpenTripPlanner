package org.opentripplanner.ext.siri;

import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.service.TransitService;
import uk.org.siri.siri20.EstimatedVehicleJourney;
import uk.org.siri.siri20.FramedVehicleJourneyRefStructure;
import uk.org.siri.siri20.MonitoredVehicleJourneyStructure;

/**
 * This class is responsible for resolving references to various entities in the transit model for
 * the SIRI updaters
 */
public class EntityResolver {

  private final TransitService transitService;

  private final String feedId;

  public EntityResolver(TransitService transitService, String feedId) {
    this.transitService = transitService;
    this.feedId = feedId;
  }

  /**
   * Resolve a {@link Trip} either by resolving a service journey id from EstimatedVehicleJourney ->
   * FramedVehicleJourneyRef -> DatedVehicleJourneyRef or a dated service journey id from
   * EstimatedVehicleJourney -> DatedVehicleJourneyRef.
   */
  public Trip resolveTrip(EstimatedVehicleJourney journey) {
    Trip trip = resolveTrip(journey.getFramedVehicleJourneyRef());
    if (trip != null) {
      return trip;
    }

    if (journey.getDatedVehicleJourneyRef() != null) {
      String datedServiceJourneyId = journey.getDatedVehicleJourneyRef().getValue();
      TripOnServiceDate tripOnServiceDate = transitService.getTripOnServiceDateById(
        new FeedScopedId(feedId, datedServiceJourneyId)
      );

      if (tripOnServiceDate != null) {
        return tripOnServiceDate.getTrip();
      }
    }

    return null;
  }

  /**
   * Resolve a {@link Trip} by resolving a service journey id from MonitoredVehicleJourney ->
   * FramedVehicleJourneyRef -> DatedVehicleJourneyRef.
   */
  public Trip resolveTrip(MonitoredVehicleJourneyStructure journey) {
    return resolveTrip(journey.getFramedVehicleJourneyRef());
  }

  /**
   * Resolve a {@link Trip} by resolving a service journey id from FramedVehicleJourneyRef ->
   * DatedVehicleJourneyRef.
   */
  private Trip resolveTrip(FramedVehicleJourneyRefStructure journey) {
    if (journey != null) {
      String serviceJourneyId = journey.getDatedVehicleJourneyRef();
      return transitService.getTripForId(new FeedScopedId(feedId, serviceJourneyId));
    }
    return null;
  }

  /**
   * Resolve a {@link RegularStop} from a quay id.
   */
  public RegularStop resolveQuay(String quayRef) {
    return transitService.getRegularStop(new FeedScopedId(feedId, quayRef));
  }
}

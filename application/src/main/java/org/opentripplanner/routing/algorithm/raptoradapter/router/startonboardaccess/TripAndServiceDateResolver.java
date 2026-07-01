package org.opentripplanner.routing.algorithm.raptoradapter.router.startonboardaccess;

import java.time.LocalDate;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.routing.api.request.TripOnDateReference;
import org.opentripplanner.routing.api.request.TripOnDateReferenceWithTripAndDate;
import org.opentripplanner.routing.api.request.TripOnDateReferenceWithTripOnServiceDateId;
import org.opentripplanner.routing.error.InvalidRoutingInputException;
import org.opentripplanner.transit.service.TransitService;

/**
 * Resolves a {@link TripOnDateReference} to a {@link TripAndServiceDate}. Called up-front by both
 * {@link TripScheduleIndexResolver} and {@link TripLocationResolver} callers.
 */
public class TripAndServiceDateResolver {

  private final TransitService transitService;

  public TripAndServiceDateResolver(TransitService transitService) {
    this.transitService = transitService;
  }

  public TripAndServiceDate resolve(TripOnDateReference reference) {
    switch (reference) {
      case TripOnDateReferenceWithTripOnServiceDateId(FeedScopedId id):
        var tripOnServiceDate = transitService.getTripOnServiceDate(id);
        if (tripOnServiceDate == null) {
          throw new InvalidRoutingInputException("TripOnServiceDate not found: " + id);
        }
        return new TripAndServiceDate(
          tripOnServiceDate.getTrip(),
          tripOnServiceDate.getServiceDate()
        );
      case TripOnDateReferenceWithTripAndDate(FeedScopedId id, LocalDate serviceDate):
        var trip = transitService.getTrip(id);
        if (trip == null) {
          throw new InvalidRoutingInputException("Trip not found: " + id);
        }
        return new TripAndServiceDate(trip, serviceDate);
    }
  }
}

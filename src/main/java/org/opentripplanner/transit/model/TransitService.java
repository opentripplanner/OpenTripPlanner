package org.opentripplanner.transit.model;

import org.opentripplanner.raptor.spi.RaptorTransitDataProvider;
import org.opentripplanner.transit.model.api.TransitRoutingRequest;
import org.opentripplanner.transit.model.calendar.ServiceCalendar;
import org.opentripplanner.transit.model.plan.RoutingRequestDataProvider;
import org.opentripplanner.transit.model.trip.TripOnDate;

/**
 * Aggregate root for transit data.
 */
public class TransitService {

  private final ServiceCalendar serviceCalendar;

  public TransitService(ServiceCalendar serviceCalendar) {
    this.serviceCalendar = serviceCalendar;
  }

  public RaptorTransitDataProvider<TripOnDate> createRoutingRequestDataProvider(
    TransitRoutingRequest request
  ) {
    // TODO RTM
    return new RoutingRequestDataProvider(request, null, null, null, null, null, null);
  }
}

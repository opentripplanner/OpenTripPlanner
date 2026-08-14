package org.opentripplanner.updater.trip.siri;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripIdAndServiceDate;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.service.TransitService;

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

  public FeedScopedId resolveId(String entityId) {
    return new FeedScopedId(feedId, entityId);
  }

  /**
   * Resolve a {@link Trip} either by resolving a service journey id from the journey's
   * FramedVehicleJourneyRef -> DatedVehicleJourneyRef, from its DatedVehicleJourneyRef, or from its
   * EstimatedVehicleJourneyCode (for a trip that was previously added by a real-time message).
   */
  @Nullable
  Trip resolveTrip(EstimatedVehicleJourneyWrapper journey) {
    var trip = journey.vehicleJourneyIdAndServiceDate().map(v -> resolveTrip(v.vehicleJourneyId()));
    if (trip.isPresent()) {
      return trip.get();
    }

    Optional<TripOnServiceDate> tripOnServiceDate = journey
      .datedVehicleJourneyRef()
      .map(jf -> transitService.getTripOnServiceDate(resolveId(jf)));
    if (tripOnServiceDate.isPresent()) {
      return tripOnServiceDate.get().getTrip();
    }

    // It is possible that the trip has previously been added, resolve the added trip
    return journey
      .code()
      .map(c -> transitService.getTrip(resolveId(c.asServiceJourneyId())))
      .orElse(null);
  }

  public TripOnServiceDate resolveTripOnServiceDate(String datedServiceJourneyId) {
    return resolveTripOnServiceDate(resolveId(datedServiceJourneyId));
  }

  @Nullable
  TripOnServiceDate resolveTripOnServiceDate(
    VehicleJourneyIdAndServiceDate vehicleJourneyIdAndServiceDate
  ) {
    return resolveTripOnServiceDate(
      vehicleJourneyIdAndServiceDate.vehicleJourneyId(),
      vehicleJourneyIdAndServiceDate.serviceDate()
    );
  }

  @Nullable
  private TripOnServiceDate resolveTripOnServiceDate(
    String serviceJourneyId,
    @Nullable LocalDate serviceDate
  ) {
    if (serviceDate == null) {
      return null;
    }

    return transitService.getTripOnServiceDate(
      new TripIdAndServiceDate(resolveId(serviceJourneyId), serviceDate)
    );
  }

  private TripOnServiceDate resolveTripOnServiceDate(FeedScopedId datedServiceJourneyId) {
    return transitService.getTripOnServiceDate(datedServiceJourneyId);
  }

  FeedScopedId resolveDatedServiceJourneyId(EstimatedVehicleJourneyWrapper journey) {
    if (journey.datedVehicleJourneyRef().isPresent()) {
      return resolveId(journey.datedVehicleJourneyRef().get());
    }

    // The added TripOnServiceDate is registered under the DatedServiceJourney-normalized id, so the
    // code must be viewed the same way here for the read path to match the write path.
    return journey
      .code()
      .map(estimatedVehicleJourneyCode ->
        resolveId(estimatedVehicleJourneyCode.asDatedServiceJourneyId())
      )
      .orElse(null);
  }

  public Trip resolveTrip(String serviceJourneyId) {
    return transitService.getTrip(resolveId(serviceJourneyId));
  }

  /**
   * Resolve a {@link RegularStop} from a scheduled stop point or quay id.
   *
   * @see org.opentripplanner.transit.service.TransitRepository#findStopByScheduledStopPoint(FeedScopedId)
   */
  RegularStop resolveQuay(String stopPointRef) {
    var id = resolveId(stopPointRef);
    return transitService
      .findStopByScheduledStopPoint(id)
      .orElseGet(() -> transitService.getRegularStop(id));
  }

  /**
   * Resolve a {@link Route} from a line id.
   */
  Route resolveRoute(String lineRef) {
    return transitService.getRoute(resolveId(lineRef));
  }

  Operator resolveOperator(String operatorRef) {
    return transitService.getOperator(resolveId(operatorRef));
  }

  /**
   * Resolve the service date of a vehicle journey, trying in order:
   * <ol>
   *   <li>the service date given by the journey's FramedVehicleJourneyRef -> DataFrameRef,</li>
   *   <li>the service date of the DatedServiceJourney referenced by the journey's
   *       DatedVehicleJourneyRef or EstimatedVehicleJourneyCode,</li>
   *   <li>the date of the aimed departure time at the first call, shifted back by the number of
   *       days the scheduled trip's first departure lies after midnight (for trips running past
   *       midnight).</li>
   * </ol>
   * Return {@code null} if none of these strategies succeed.
   */
  @Nullable
  LocalDate resolveServiceDate(EstimatedVehicleJourneyWrapper journey) {
    var serviceDate = journey
      .vehicleJourneyIdAndServiceDate()
      .map(VehicleJourneyIdAndServiceDate::serviceDate);
    if (serviceDate.isPresent()) {
      return serviceDate.get();
    }

    FeedScopedId datedServiceJourneyId = resolveDatedServiceJourneyId(journey);
    if (datedServiceJourneyId != null) {
      var datedServiceJourney = resolveTripOnServiceDate(datedServiceJourneyId);
      if (datedServiceJourney != null) {
        return datedServiceJourney.getServiceDate();
      }
    }

    var calls = journey.calls();
    if (calls.isEmpty()) {
      return null;
    }

    var departureTime = calls.getFirst().getAimedDepartureTime();
    if (departureTime == null) {
      return null;
    }

    var daysOffset = calculateDayOffset(journey);

    return departureTime.toLocalDate().minusDays(daysOffset);
  }

  /**
   * Calculate the difference in days between the service date and the departure at the first stop.
   */
  private int calculateDayOffset(EstimatedVehicleJourneyWrapper journey) {
    Trip trip = resolveTrip(journey);
    if (trip == null) {
      return 0;
    }
    var pattern = transitService.findPattern(trip);
    if (pattern == null) {
      return 0;
    }
    var tripTimes = pattern.getScheduledTimetable().getTripTimes(trip);
    if (tripTimes == null) {
      return 0;
    }
    var departureTime = tripTimes.getDepartureTime(0);
    var days = (int) Duration.ofSeconds(departureTime).toDays();
    if (departureTime < 0) {
      return days - 1;
    } else {
      return days;
    }
  }
}

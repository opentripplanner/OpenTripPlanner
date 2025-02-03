package org.opentripplanner.updater.siri;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripIdAndServiceDate;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.service.TransitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.DatedVehicleJourneyRef;
import uk.org.siri.siri20.EstimatedVehicleJourney;
import uk.org.siri.siri20.FramedVehicleJourneyRefStructure;
import uk.org.siri.siri20.MonitoredVehicleJourneyStructure;

/**
 * This class is responsible for resolving references to various entities in the transit model for
 * the SIRI updaters
 */
public class EntityResolver {

  private static final Logger LOG = LoggerFactory.getLogger(EntityResolver.class);

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
      TripOnServiceDate tripOnServiceDate = transitService.getTripOnServiceDate(
        resolveId(datedServiceJourneyId)
      );

      if (tripOnServiceDate != null) {
        return tripOnServiceDate.getTrip();
      }
    }

    // It is possible that the trip has previously been added, resolve the added trip
    if (journey.getEstimatedVehicleJourneyCode() != null) {
      var addedTrip = transitService.getTrip(resolveId(journey.getEstimatedVehicleJourneyCode()));
      if (addedTrip != null) {
        return addedTrip;
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

  public TripOnServiceDate resolveTripOnServiceDate(
    EstimatedVehicleJourney estimatedVehicleJourney
  ) {
    FeedScopedId datedServiceJourneyId = resolveDatedServiceJourneyId(estimatedVehicleJourney);
    if (datedServiceJourneyId != null) {
      return resolveTripOnServiceDate(datedServiceJourneyId);
    }
    return resolveTripOnServiceDate(estimatedVehicleJourney.getFramedVehicleJourneyRef());
  }

  public TripOnServiceDate resolveTripOnServiceDate(String datedServiceJourneyId) {
    return resolveTripOnServiceDate(resolveId(datedServiceJourneyId));
  }

  public TripOnServiceDate resolveTripOnServiceDate(
    FramedVehicleJourneyRefStructure framedVehicleJourney
  ) {
    return resolveTripOnServiceDate(
      framedVehicleJourney.getDatedVehicleJourneyRef(),
      resolveServiceDate(framedVehicleJourney)
    );
  }

  @Nullable
  public TripOnServiceDate resolveTripOnServiceDate(
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

  public TripOnServiceDate resolveTripOnServiceDate(FeedScopedId datedServiceJourneyId) {
    return transitService.getTripOnServiceDate(datedServiceJourneyId);
  }

  public FeedScopedId resolveDatedServiceJourneyId(
    EstimatedVehicleJourney estimatedVehicleJourney
  ) {
    DatedVehicleJourneyRef datedVehicleJourneyRef = estimatedVehicleJourney.getDatedVehicleJourneyRef();
    if (datedVehicleJourneyRef != null) {
      return resolveId(datedVehicleJourneyRef.getValue());
    }

    if (estimatedVehicleJourney.getEstimatedVehicleJourneyCode() != null) {
      return resolveId(estimatedVehicleJourney.getEstimatedVehicleJourneyCode());
    }

    return null;
  }

  public LocalDate resolveServiceDate(FramedVehicleJourneyRefStructure vehicleJourneyRefStructure) {
    if (vehicleJourneyRefStructure.getDataFrameRef() != null) {
      var dataFrame = vehicleJourneyRefStructure.getDataFrameRef();
      if (dataFrame != null) {
        try {
          return LocalDate.parse(dataFrame.getValue());
        } catch (DateTimeParseException ignored) {
          LOG.warn("Invalid dataFrame format: {}", dataFrame.getValue());
        }
      }
    }
    return null;
  }

  /**
   * Resolve serviceDate. For legacy reasons this is provided in originAimedDepartureTime - in lack
   * of alternatives. Even though the field's name indicates that the timestamp represents the
   * departure from the first stop, only the Date-part is actually used, and is defined to
   * represent the actual serviceDate. The time and zone part is ignored.
   */
  @Nullable
  public LocalDate resolveServiceDate(@Nullable ZonedDateTime originAimedDepartureTime) {
    if (originAimedDepartureTime == null) {
      return null;
    }
    // This grabs the local-date from timestamp passed into OTP ignoring the time and zone
    // information. An alternative is to use the transit model zone:
    // 'originAimedDepartureTime.withZoneSameInstant(transitService.getTimeZone())'

    return originAimedDepartureTime.toLocalDate();
  }

  /**
   * Resolve a {@link Trip} by resolving a service journey id from FramedVehicleJourneyRef ->
   * DatedVehicleJourneyRef.
   */
  @Nullable
  public Trip resolveTrip(@Nullable FramedVehicleJourneyRefStructure journey) {
    if (journey != null) {
      return resolveTrip(journey.getDatedVehicleJourneyRef());
    }
    return null;
  }

  public Trip resolveTrip(String serviceJourneyId) {
    return transitService.getTrip(resolveId(serviceJourneyId));
  }

  /**
   * Resolve a {@link RegularStop} from a scheduled stop point or quay id.
   *
   * @see org.opentripplanner.transit.service.TimetableRepository#findStopByScheduledStopPoint(FeedScopedId)
   */
  public RegularStop resolveQuay(String stopPointRef) {
    var id = resolveId(stopPointRef);
    return transitService
      .findStopByScheduledStopPoint(id)
      .orElseGet(() -> transitService.getRegularStop(id));
  }

  /**
   * Resolve a {@link Route} from a line id.
   */
  public Route resolveRoute(String lineRef) {
    return transitService.getRoute(resolveId(lineRef));
  }

  public Operator resolveOperator(String operatorRef) {
    return transitService.getOperator(resolveId(operatorRef));
  }

  @Nullable
  public LocalDate resolveServiceDate(EstimatedVehicleJourney vehicleJourney) {
    if (vehicleJourney.getFramedVehicleJourneyRef() != null) {
      var dataFrame = vehicleJourney.getFramedVehicleJourneyRef().getDataFrameRef();
      if (dataFrame != null) {
        try {
          return LocalDate.parse(dataFrame.getValue());
        } catch (DateTimeParseException ignored) {
          LOG.warn("Invalid dataFrame format: {}", dataFrame.getValue());
        }
      }
    }

    FeedScopedId datedServiceJourneyId = resolveDatedServiceJourneyId(vehicleJourney);
    if (datedServiceJourneyId != null) {
      var datedServiceJourney = resolveTripOnServiceDate(datedServiceJourneyId);
      if (datedServiceJourney != null) {
        return datedServiceJourney.getServiceDate();
      }
    }

    var datetime = CallWrapper
      .of(vehicleJourney)
      .stream()
      .findFirst()
      .map(CallWrapper::getAimedDepartureTime);
    if (datetime.isEmpty()) {
      return null;
    }

    var daysOffset = calculateDayOffset(vehicleJourney);

    return datetime.get().toLocalDate().minusDays(daysOffset);
  }

  /**
   * Calculate the difference in days between the service date and the departure at the first stop.
   */
  private int calculateDayOffset(EstimatedVehicleJourney vehicleJourney) {
    Trip trip = resolveTrip(vehicleJourney);
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

package org.opentripplanner.ext.siri;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripIdAndServiceDate;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.model.timetable.TripOnServiceDateBuilder;
import org.opentripplanner.transit.service.TransitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.DatedVehicleJourneyRef;
import uk.org.siri.siri20.EstimatedVehicleJourney;
import uk.org.siri.siri20.FramedVehicleJourneyRefStructure;
import uk.org.siri.siri20.MonitoredVehicleJourneyStructure;
import uk.org.siri.siri20.VehicleJourneyRef;

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
      TripOnServiceDate tripOnServiceDate = transitService.getTripOnServiceDateById(
        resolveId(datedServiceJourneyId)
      );

      if (tripOnServiceDate != null) {
        return tripOnServiceDate.getTrip();
      }
    }

    // It is possible that the trip has previously been added, resolve the added trip
    if (journey.getEstimatedVehicleJourneyCode() != null) {
      var addedTrip = transitService.getTripForId(
        resolveId(journey.getEstimatedVehicleJourneyCode())
      );
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

  public TripOnServiceDate resolveTripOnServiceDate(
    String serviceJourneyId,
    LocalDate serviceDate
  ) {
    if (serviceDate == null) {
      return null;
    }

    return transitService.getTripOnServiceDateForTripAndDay(
      new TripIdAndServiceDate(resolveId(serviceJourneyId), serviceDate)
    );
  }

  public TripOnServiceDate resolveTripOnServiceDate(FeedScopedId datedServiceJourneyId) {
    return transitService.getTripOnServiceDateById(datedServiceJourneyId);
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
  public LocalDate resolveServiceDate(ZonedDateTime originAimedDepartureTime) {
    if (originAimedDepartureTime == null) {
      return null;
    }
    // This grab the local-date from timestamp passed into OTP ignoring the time and zone
    // information. An alternative is to use the transit model zone:
    // 'originAimedDepartureTime.withZoneSameInstant(transitService.getTimeZone())'

    return originAimedDepartureTime.toLocalDate();
  }

  /**
   * Resolve a {@link Trip} by resolving a service journey id from FramedVehicleJourneyRef ->
   * DatedVehicleJourneyRef.
   */
  public Trip resolveTrip(FramedVehicleJourneyRefStructure journey) {
    if (journey != null) {
      return resolveTrip(journey.getDatedVehicleJourneyRef());
    }
    return null;
  }

  public Trip resolveTrip(String serviceJourneyId) {
    return transitService.getTripForId(resolveId(serviceJourneyId));
  }

  /**
   * Resolve a {@link RegularStop} from a quay id.
   */
  public RegularStop resolveQuay(String quayRef) {
    return transitService.getRegularStop(resolveId(quayRef));
  }

  /**
   * Resolve a {@link Route} from a line id.
   */
  public Route resolveRoute(String lineRef) {
    return transitService.getRouteForId(resolveId(lineRef));
  }

  public Operator resolveOperator(String operatorRef) {
    return transitService.getOperatorForId(resolveId(operatorRef));
  }

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

    ZonedDateTime date = CallWrapper.of(vehicleJourney).get(0).getAimedDepartureTime();

    if (date == null) {
      return null;
    }

    var daysOffset = calculateDayOffset(vehicleJourney);

    return date.toLocalDate().minusDays(daysOffset);
  }

  /**
   * Calculate the difference in days between the service date and the departure at the first stop.
   */
  private int calculateDayOffset(EstimatedVehicleJourney vehicleJourney) {
    Trip trip = resolveTrip(vehicleJourney);
    if (trip == null) {
      return 0;
    }
    var pattern = transitService.getPatternForTrip(trip);
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

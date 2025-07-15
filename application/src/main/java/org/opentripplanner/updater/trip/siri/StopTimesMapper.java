package org.opentripplanner.updater.trip.siri;

import static org.opentripplanner.updater.trip.siri.support.NaturalLanguageStringHelper.getFirstStringFromList;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import javax.annotation.Nullable;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.updater.trip.siri.mapping.PickDropMapper;
import org.opentripplanner.utils.time.ServiceDateUtils;

class StopTimesMapper {

  private final EntityResolver entityResolver;
  private final ZoneId zoneId;

  public StopTimesMapper(EntityResolver entityResolver, ZoneId zoneId) {
    this.entityResolver = entityResolver;
    this.zoneId = zoneId;
  }

  /**
   * Map the call to the aimed StopTime or return null if the stop cannot be found in the site repository.
   */
  @Nullable
  StopTime createAimedStopTime(
    Trip trip,
    ZonedDateTime departureDate,
    int stopSequence,
    CallWrapper call,
    boolean isFirstStop,
    boolean isLastStop
  ) {
    RegularStop stop = entityResolver.resolveQuay(call.getStopPointRef());
    if (stop == null) {
      return null;
    }

    StopTime stopTime = new StopTime();
    stopTime.setStopSequence(stopSequence);
    stopTime.setTrip(trip);
    stopTime.setStop(stop);

    // Fallback to other time, if one doesn't exist
    var aimedArrivalTime = call.getAimedArrivalTime() != null
      ? call.getAimedArrivalTime()
      : call.getAimedDepartureTime();

    var aimedArrivalTimeSeconds = ServiceDateUtils.secondsSinceStartOfService(
      departureDate,
      aimedArrivalTime,
      zoneId
    );

    var aimedDepartureTime = call.getAimedDepartureTime() != null
      ? call.getAimedDepartureTime()
      : call.getAimedArrivalTime();

    var aimedDepartureTimeSeconds = ServiceDateUtils.secondsSinceStartOfService(
      departureDate,
      aimedDepartureTime,
      zoneId
    );

    // Use departure time for first stop, and arrival time for last stop, to avoid negative dwell times
    stopTime.setArrivalTime(isFirstStop ? aimedDepartureTimeSeconds : aimedArrivalTimeSeconds);
    stopTime.setDepartureTime(isLastStop ? aimedArrivalTimeSeconds : aimedDepartureTimeSeconds);

    // Update destination display
    var destinationDisplay = getFirstStringFromList(call.getDestinationDisplaies());
    if (!destinationDisplay.isEmpty()) {
      stopTime.setStopHeadsign(new NonLocalizedString(destinationDisplay));
    } else if (trip.getHeadsign() != null) {
      stopTime.setStopHeadsign(trip.getHeadsign());
    } else {
      // Fallback to empty string
      stopTime.setStopHeadsign(new NonLocalizedString(""));
    }

    // Update pickup / dropoff
    PickDropMapper.mapPickUpType(call, stopTime.getPickupType()).ifPresent(stopTime::setPickupType);
    PickDropMapper.mapDropOffType(call, stopTime.getDropOffType()).ifPresent(
      stopTime::setDropOffType
    );

    return stopTime;
  }
}

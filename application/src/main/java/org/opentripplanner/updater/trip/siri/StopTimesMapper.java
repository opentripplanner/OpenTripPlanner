package org.opentripplanner.updater.trip.siri;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.Trip;
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

    // Set default boarding/alighting rules for first and last stops.
    // Can't alight at first stop and can't board at last stop.
    if (isFirstStop) {
      stopTime.setDropOffType(PickDrop.NONE);
    }
    if (isLastStop) {
      stopTime.setPickupType(PickDrop.NONE);
    }

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
    var destinationDisplay = call.destinationDisplay();
    if (!destinationDisplay.isEmpty()) {
      stopTime.setStopHeadsign(new NonLocalizedString(destinationDisplay));
    } else if (trip.getHeadsign() != null) {
      stopTime.setStopHeadsign(trip.getHeadsign());
    }

    // Update pickup / dropoff
    call.pickUp().applyTo(stopTime.getPickupType()).ifPresent(stopTime::setPickupType);
    call.dropOff().applyTo(stopTime.getDropOffType()).ifPresent(stopTime::setDropOffType);

    return stopTime;
  }
}

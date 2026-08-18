package org.opentripplanner.updater.trip.siri;

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

  public StopTimesMapper(EntityResolver entityResolver) {
    this.entityResolver = entityResolver;
  }

  /**
   * Map the call to the aimed StopTime or return null if the stop cannot be found in the site repository.
   */
  @Nullable
  StopTime createAimedStopTime(
    Trip trip,
    ZonedDateTime startOfService,
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
      startOfService,
      aimedArrivalTime
    );

    var aimedDepartureTime = call.getAimedDepartureTime() != null
      ? call.getAimedDepartureTime()
      : call.getAimedArrivalTime();

    var aimedDepartureTimeSeconds = ServiceDateUtils.secondsSinceStartOfService(
      startOfService,
      aimedDepartureTime
    );

    stopTime.setArrivalTime(aimedArrivalTimeSeconds);
    stopTime.setDepartureTime(aimedDepartureTimeSeconds);

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

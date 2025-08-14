package org.opentripplanner.gtfs.mapping;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.onebusaway.gtfs.model.Location;
import org.onebusaway.gtfs.model.LocationGroup;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.utils.collection.MapUtils;

/**
 * Responsible for mapping GTFS StopTime into the OTP Transit model.
 */
class StopTimeMapper {

  private final StopMapper stopMapper;

  private final LocationMapper locationMapper;

  private final LocationGroupMapper locationGroupMapper;

  private final TripMapper tripMapper;
  private final BookingRuleMapper bookingRuleMapper;

  private final Map<org.onebusaway.gtfs.model.StopTime, StopTime> mappedStopTimes = new HashMap<>();

  private final TranslationHelper translationHelper;

  StopTimeMapper(
    StopMapper stopMapper,
    LocationMapper locationMapper,
    LocationGroupMapper locationGroupMapper,
    TripMapper tripMapper,
    BookingRuleMapper bookingRuleMapper,
    TranslationHelper translationHelper
  ) {
    this.stopMapper = stopMapper;
    this.locationMapper = locationMapper;
    this.locationGroupMapper = locationGroupMapper;
    this.tripMapper = tripMapper;
    this.bookingRuleMapper = bookingRuleMapper;
    this.translationHelper = translationHelper;
  }

  Collection<StopTime> map(Collection<org.onebusaway.gtfs.model.StopTime> times) {
    return MapUtils.mapToList(times, this::map);
  }

  /** Map from GTFS to OTP model, {@code null} safe. */
  StopTime map(org.onebusaway.gtfs.model.StopTime orginal) {
    return orginal == null ? null : mappedStopTimes.computeIfAbsent(orginal, this::doMap);
  }

  private StopTime doMap(org.onebusaway.gtfs.model.StopTime rhs) {
    StopTime lhs = new StopTime();

    lhs.setTrip(tripMapper.map(rhs.getTrip()));
    var stopLocation = rhs.getStopLocation();
    Objects.requireNonNull(
      stopLocation,
      "Trip %s contains stop_time with no stop, location or group.".formatted(rhs.getTrip())
    );
    switch (stopLocation) {
      case Stop stop -> lhs.setStop(stopMapper.map(stop));
      case Location location -> lhs.setStop(locationMapper.map(location));
      case LocationGroup locGroup -> lhs.setStop(locationGroupMapper.map(locGroup));
      default -> throw new IllegalArgumentException(
        "Unknown location type: %s".formatted(stopLocation)
      );
    }

    I18NString stopHeadsign = null;
    if (rhs.getStopHeadsign() != null) {
      stopHeadsign = translationHelper.getTranslation(
        org.onebusaway.gtfs.model.StopTime.class,
        "stopHeadsign",
        rhs.getTrip().getId().toString(),
        Integer.toString(rhs.getStopSequence()),
        rhs.getStopHeadsign()
      );
    }

    lhs.setArrivalTime(rhs.getArrivalTime());
    lhs.setDepartureTime(rhs.getDepartureTime());
    lhs.setTimepoint(rhs.getTimepoint());
    lhs.setStopSequence(rhs.getStopSequence());
    lhs.setStopHeadsign(stopHeadsign);
    lhs.setPickupType(PickDropMapper.map(rhs.getPickupType()));
    lhs.setDropOffType(PickDropMapper.map(rhs.getDropOffType()));
    lhs.setShapeDistTraveled(rhs.getShapeDistTraveled());
    lhs.setFlexWindowStart(rhs.getStartPickupDropOffWindow());
    lhs.setFlexWindowEnd(rhs.getEndPickupDropOffWindow());

    lhs.setFlexContinuousPickup(
      PickDropMapper.mapFlexContinuousPickDrop(rhs.getContinuousPickup())
    );
    lhs.setFlexContinuousDropOff(
      PickDropMapper.mapFlexContinuousPickDrop(rhs.getContinuousDropOff())
    );
    lhs.setPickupBookingInfo(bookingRuleMapper.map(rhs.getPickupBookingRule()));
    lhs.setDropOffBookingInfo(bookingRuleMapper.map(rhs.getDropOffBookingRule()));

    // Skip mapping of proxy
    // private transient StopTimeProxy proxy;
    if (rhs.getProxy() != null) {
      throw new IllegalStateException("Did not expect proxy to be set!");
    }

    return lhs;
  }
}

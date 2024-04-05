package org.opentripplanner.gtfs.mapping;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.opentripplanner.ext.flex.trip.FlexDurationModifier;
import org.opentripplanner.framework.collection.MapUtils;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model.timetable.Trip;

/** Responsible for mapping GTFS TripMapper into the OTP model. */
class TripMapper {

  private final RouteMapper routeMapper;
  private final DirectionMapper directionMapper;
  private final TranslationHelper translationHelper;

  private final Map<org.onebusaway.gtfs.model.Trip, Trip> mappedTrips = new HashMap<>();
  private final Map<Trip, FlexDurationModifier> flexSafeDurationFactors = new HashMap<>();

  TripMapper(
    RouteMapper routeMapper,
    DirectionMapper directionMapper,
    TranslationHelper translationHelper
  ) {
    this.routeMapper = routeMapper;
    this.directionMapper = directionMapper;
    this.translationHelper = translationHelper;
  }

  Collection<Trip> map(Collection<org.onebusaway.gtfs.model.Trip> trips) {
    return MapUtils.mapToList(trips, this::map);
  }

  Trip map(org.onebusaway.gtfs.model.Trip orginal) {
    return orginal == null ? null : mappedTrips.computeIfAbsent(orginal, this::doMap);
  }

  Collection<Trip> getMappedTrips() {
    return mappedTrips.values();
  }

  /**
   * The map of flex duration factors per flex trip.
   */
  Map<Trip, FlexDurationModifier> flexSafeDurationFactors() {
    return flexSafeDurationFactors;
  }

  private Trip doMap(org.onebusaway.gtfs.model.Trip rhs) {
    var lhs = Trip.of(AgencyAndIdMapper.mapAgencyAndId(rhs.getId()));

    lhs.withRoute(routeMapper.map(rhs.getRoute()));
    lhs.withServiceId(AgencyAndIdMapper.mapAgencyAndId(rhs.getServiceId()));
    lhs.withShortName(rhs.getTripShortName());
    I18NString tripHeadsign = null;
    if (rhs.getTripHeadsign() != null) {
      tripHeadsign =
        translationHelper.getTranslation(
          org.onebusaway.gtfs.model.Trip.class,
          "tripHeadsign",
          rhs.getId().getId(),
          rhs.getTripHeadsign()
        );
    }
    lhs.withHeadsign(tripHeadsign);
    lhs.withDirection(directionMapper.map(rhs.getDirectionId(), lhs.getId()));
    lhs.withGtfsBlockId(rhs.getBlockId());
    lhs.withShapeId(AgencyAndIdMapper.mapAgencyAndId(rhs.getShapeId()));
    lhs.withWheelchairBoarding(WheelchairAccessibilityMapper.map(rhs.getWheelchairAccessible()));
    lhs.withBikesAllowed(BikeAccessMapper.mapForTrip(rhs));
    lhs.withGtfsFareId(rhs.getFareId());

    var trip = lhs.build();
    mapSafeDurationFactors(rhs).ifPresent(f -> flexSafeDurationFactors.put(trip, f));
    return trip;
  }

  private Optional<FlexDurationModifier> mapSafeDurationFactors(
    org.onebusaway.gtfs.model.Trip rhs
  ) {
    if (rhs.getSafeDurationFactor() == null && rhs.getSafeDurationOffset() == null) {
      return Optional.empty();
    } else {
      var offset = Duration.ofSeconds(rhs.getSafeDurationOffset().longValue());
      return Optional.of(
        new FlexDurationModifier(offset, rhs.getSafeDurationFactor().floatValue())
      );
    }
  }
}

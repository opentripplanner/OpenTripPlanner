package org.opentripplanner.gtfs.mapping;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.util.MapUtils;

/** Responsible for mapping GTFS TripMapper into the OTP model. */
class TripMapper {

  private final RouteMapper routeMapper;
  private final DirectionMapper directionMapper;

  private final Map<org.onebusaway.gtfs.model.Trip, Trip> mappedTrips = new HashMap<>();

  TripMapper(RouteMapper routeMapper, DirectionMapper directionMapper) {
    this.routeMapper = routeMapper;
    this.directionMapper = directionMapper;
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

  private Trip doMap(org.onebusaway.gtfs.model.Trip rhs) {
    var lhs = Trip.of(AgencyAndIdMapper.mapAgencyAndId(rhs.getId()));

    lhs.withRoute(routeMapper.map(rhs.getRoute()));
    lhs.withServiceId(AgencyAndIdMapper.mapAgencyAndId(rhs.getServiceId()));
    lhs.withShortName(rhs.getTripShortName());
    lhs.withHeadsign(rhs.getTripHeadsign());
    lhs.withDirection(directionMapper.map(rhs.getDirectionId(), lhs.getId()));
    lhs.withGtfsBlockId(rhs.getBlockId());
    lhs.withShapeId(AgencyAndIdMapper.mapAgencyAndId(rhs.getShapeId()));
    lhs.withWheelchairBoarding(WheelchairAccessibilityMapper.map(rhs.getWheelchairAccessible()));
    lhs.withBikesAllowed(BikeAccessMapper.mapForTrip(rhs));
    lhs.withGtfsFareId(rhs.getFareId());

    return lhs.build();
  }
}

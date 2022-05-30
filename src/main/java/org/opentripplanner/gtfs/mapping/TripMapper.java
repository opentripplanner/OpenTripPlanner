package org.opentripplanner.gtfs.mapping;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.model.Direction;
import org.opentripplanner.model.WheelchairAccessibility;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.util.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Responsible for mapping GTFS TripMapper into the OTP model. */
class TripMapper {

  private static final Logger LOG = LoggerFactory.getLogger(TripMapper.class);

  private final RouteMapper routeMapper;

  private final Map<org.onebusaway.gtfs.model.Trip, Trip> mappedTrips = new HashMap<>();

  TripMapper(RouteMapper routeMapper) {
    this.routeMapper = routeMapper;
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

  private static int mapDirectionId(org.onebusaway.gtfs.model.Trip trip) {
    try {
      String directionId = trip.getDirectionId();
      if (directionId == null || directionId.isBlank()) {
        return -1;
      }
      return Integer.parseInt(directionId);
    } catch (NumberFormatException e) {
      LOG.debug("Trip {} does not have direction id, defaults to -1", trip);
    }
    return -1;
  }

  private Trip doMap(org.onebusaway.gtfs.model.Trip rhs) {
    var lhs = Trip.of(AgencyAndIdMapper.mapAgencyAndId(rhs.getId()));

    lhs.withRoute(routeMapper.map(rhs.getRoute()));
    lhs.withServiceId(AgencyAndIdMapper.mapAgencyAndId(rhs.getServiceId()));
    lhs.withShortName(rhs.getTripShortName());
    lhs.withHeadsign(rhs.getTripHeadsign());
    lhs.withDirection(Direction.valueOfGtfsCode(mapDirectionId(rhs)));
    lhs.withGtfsBlockId(rhs.getBlockId());
    lhs.withShapeId(AgencyAndIdMapper.mapAgencyAndId(rhs.getShapeId()));
    lhs.withWheelchairBoarding(
      WheelchairAccessibility.valueOfGtfsCode(rhs.getWheelchairAccessible())
    );
    lhs.withBikesAllowed(BikeAccessMapper.mapForTrip(rhs));
    lhs.withGtfsFareId(rhs.getFareId());

    return lhs.build();
  }
}

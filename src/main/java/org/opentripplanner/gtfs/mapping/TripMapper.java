package org.opentripplanner.gtfs.mapping;

import org.opentripplanner.model.Direction;
import org.opentripplanner.model.Trip;
import org.opentripplanner.util.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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

    private Trip doMap(org.onebusaway.gtfs.model.Trip rhs) {
        Trip lhs = new Trip(AgencyAndIdMapper.mapAgencyAndId(rhs.getId()));

        lhs.setRoute(routeMapper.map(rhs.getRoute()));
        lhs.setServiceId(AgencyAndIdMapper.mapAgencyAndId(rhs.getServiceId()));
        lhs.setTripShortName(rhs.getTripShortName());
        lhs.setTripHeadsign(rhs.getTripHeadsign());
        lhs.setRouteShortName(rhs.getRouteShortName());
        lhs.setDirection(Direction.valueOfGtfsCode(mapDirectionId(rhs)));
        lhs.setBlockId(rhs.getBlockId());
        lhs.setShapeId(AgencyAndIdMapper.mapAgencyAndId(rhs.getShapeId()));
        lhs.setWheelchairAccessible(rhs.getWheelchairAccessible());
        lhs.setBikesAllowed(BikeAccessMapper.mapForTrip(rhs));
        lhs.setFareId(rhs.getFareId());

        return lhs;
    }

    @Nullable
    private static int mapDirectionId(org.onebusaway.gtfs.model.Trip trip) {
        try {
            String directionId = trip.getDirectionId();
            if(directionId == null || directionId.isBlank()) {
                return -1;
            }
            return Integer.parseInt(directionId);
        } catch (NumberFormatException e) {
            LOG.debug("Trip {} does not have direction id, defaults to -1", trip);
        }
        return -1;
    }
}

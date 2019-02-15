package org.opentripplanner.gtfs.mapping;

import org.opentripplanner.model.Trip;
import org.opentripplanner.util.MapUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** Responsible for mapping GTFS TripMapper into the OTP model. */
class TripMapper {

    private final RouteMapper routeMapper;

    private Map<org.onebusaway.gtfs.model.Trip, Trip> mappedTrips = new HashMap<>();

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
        Trip lhs = new Trip();

        lhs.setId(AgencyAndIdMapper.mapAgencyAndId(rhs.getId()));
        lhs.setRoute(routeMapper.map(rhs.getRoute()));
        lhs.setServiceId(AgencyAndIdMapper.mapAgencyAndId(rhs.getServiceId()));
        lhs.setTripShortName(rhs.getTripShortName());
        lhs.setTripHeadsign(rhs.getTripHeadsign());
        lhs.setRouteShortName(rhs.getRouteShortName());
        lhs.setDirectionId(rhs.getDirectionId());
        lhs.setBlockId(rhs.getBlockId());
        lhs.setShapeId(AgencyAndIdMapper.mapAgencyAndId(rhs.getShapeId()));
        lhs.setWheelchairAccessible(rhs.getWheelchairAccessible());
        lhs.setTripBikesAllowed(rhs.getTripBikesAllowed());
        lhs.setBikesAllowed(rhs.getBikesAllowed());
        lhs.setFareId(rhs.getFareId());
        lhs.setDrtMaxTravelTime(rhs.getDrtMaxTravelTime());
        lhs.setDrtAvgTravelTime(rhs.getDrtAvgTravelTime());
        lhs.setDrtAdvanceBookMin(rhs.getDrtAdvanceBookMin());
        lhs.setDrtPickupMessage(rhs.getDrtPickupMessage());
        lhs.setDrtDropOffMessage(rhs.getDrtDropOffMessage());
        lhs.setContinuousPickupMessage(rhs.getContinuousPickupMessage());
        lhs.setContinuousDropOffMessage(rhs.getContinuousDropOffMessage());

        return lhs;
    }

}

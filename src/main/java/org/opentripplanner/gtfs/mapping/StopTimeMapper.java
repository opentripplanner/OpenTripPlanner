package org.opentripplanner.gtfs.mapping;

import org.opentripplanner.model.StopTime;
import org.opentripplanner.util.MapUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** Responsible for mapping GTFS StopTime into the OTP model. */
class StopTimeMapper {
    private final StopMapper stopMapper;

    private final TripMapper tripMapper;

    private Map<org.onebusaway.gtfs.model.StopTime, StopTime> mappedStopTimes = new HashMap<>();

    StopTimeMapper(StopMapper stopMapper, TripMapper tripMapper) {
        this.stopMapper = stopMapper;
        this.tripMapper = tripMapper;
    }

    Collection<StopTime> map(Collection<org.onebusaway.gtfs.model.StopTime> times) {
        return MapUtils.mapToList(times, this::map);
    }

    /** Map from GTFS to OTP model, {@code null} safe.  */
    StopTime map(org.onebusaway.gtfs.model.StopTime orginal) {
        return orginal == null ? null : mappedStopTimes.computeIfAbsent(orginal, this::doMap);
    }

    private StopTime doMap(org.onebusaway.gtfs.model.StopTime rhs) {
        StopTime lhs = new StopTime();

        lhs.setTrip(tripMapper.map(rhs.getTrip()));
        lhs.setStop(stopMapper.map(rhs.getStop()));
        lhs.setArrivalTime(rhs.getArrivalTime());
        lhs.setDepartureTime(rhs.getDepartureTime());
        lhs.setTimepoint(rhs.getTimepoint());
        lhs.setStopSequence(rhs.getStopSequence());
        lhs.setStopHeadsign(rhs.getStopHeadsign());
        lhs.setRouteShortName(rhs.getRouteShortName());
        lhs.setPickupType(rhs.getPickupType());
        lhs.setDropOffType(rhs.getDropOffType());
        lhs.setShapeDistTraveled(rhs.getShapeDistTraveled());
        lhs.setFarePeriodId(rhs.getFarePeriodId());
        lhs.setContinuousPickup(rhs.getContinuousPickup());
        lhs.setContinuousDropOff(rhs.getContinuousDropOff());
        lhs.setStartServiceArea(rhs.getStartServiceArea());
        lhs.setEndServiceArea(rhs.getEndServiceArea());
        lhs.setStartServiceAreaRadius(rhs.getStartServiceAreaRadius());
        lhs.setEndServiceAreaRadius(rhs.getEndServiceAreaRadius());

        // Skip mapping of proxy
        // private transient StopTimeProxy proxy;
        if (rhs.getProxy() != null) {
            throw new IllegalStateException("Did not expect proxy to be set!");
        }

        return lhs;
    }

}

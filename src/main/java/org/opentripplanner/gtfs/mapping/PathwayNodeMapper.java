package org.opentripplanner.gtfs.mapping;

import org.opentripplanner.model.PathwayNode;
import org.opentripplanner.model.WgsCoordinate;
import org.opentripplanner.model.WheelChairBoarding;
import org.opentripplanner.util.MapUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.opentripplanner.gtfs.mapping.AgencyAndIdMapper.mapAgencyAndId;

/** Responsible for mapping GTFS Node into the OTP model. */
class PathwayNodeMapper {
    private Map<org.onebusaway.gtfs.model.Stop, PathwayNode> mappedNodes = new HashMap<>();

    Collection<PathwayNode> map(Collection<org.onebusaway.gtfs.model.Stop> allNodes) {
        return MapUtils.mapToList(allNodes, this::map);
    }

    /** Map from GTFS to OTP model, {@code null} safe.  */
    PathwayNode map(org.onebusaway.gtfs.model.Stop orginal) {
        return orginal == null ? null : mappedNodes.computeIfAbsent(orginal, this::doMap);
    }

    private PathwayNode doMap(org.onebusaway.gtfs.model.Stop gtfsStop) {
        if (gtfsStop.getLocationType() != org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_NODE) {
            throw new IllegalArgumentException(
                "Expected type " + org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_NODE + ", but got "
                    + gtfsStop.getLocationType());
        }

        PathwayNode otpPathwayNode = new PathwayNode();

        otpPathwayNode.setId(mapAgencyAndId(gtfsStop.getId()));
        otpPathwayNode.setName(gtfsStop.getName());
        if (gtfsStop.isLatSet() && gtfsStop.isLonSet()) {
            otpPathwayNode.setCoordinate(new WgsCoordinate(gtfsStop.getLat(), gtfsStop.getLon()));
        }
        otpPathwayNode.setCode(gtfsStop.getCode());
        otpPathwayNode.setDescription(gtfsStop.getDesc());
        otpPathwayNode.setUrl(gtfsStop.getUrl());
        otpPathwayNode.setWheelchairBoarding(
            WheelChairBoarding.valueOfGtfsCode(gtfsStop.getWheelchairBoarding())
        );
        var level = gtfsStop.getLevel();
        if (level != null) {
            otpPathwayNode.setLevelIndex(level.getIndex());
            otpPathwayNode.setLevelName(level.getName());
        }

        return otpPathwayNode;
    }
}

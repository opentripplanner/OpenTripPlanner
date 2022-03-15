package org.opentripplanner.gtfs.mapping;

import static java.util.Objects.requireNonNullElse;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.model.PathwayNode;
import org.opentripplanner.util.MapUtils;

/** Responsible for mapping GTFS Node into the OTP model. */
class PathwayNodeMapper {
    static final String DEFAULT_NAME = "Pathway node";

    private final Map<org.onebusaway.gtfs.model.Stop, PathwayNode> mappedNodes = new HashMap<>();

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

        StopMappingWrapper base = new StopMappingWrapper(gtfsStop);

        return new PathwayNode(
            base.getId(),
            requireNonNullElse(base.getName(), DEFAULT_NAME),
            base.getCode(),
            base.getDescription(),
            base.getCoordinate(),
            base.getWheelchairBoarding(),
            base.getLevel()
        );
    }
}

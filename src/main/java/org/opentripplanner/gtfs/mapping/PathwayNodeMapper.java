package org.opentripplanner.gtfs.mapping;

import org.opentripplanner.model.PathwayNode;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.MapUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.util.NonLocalizedString;

/** Responsible for mapping GTFS Node into the OTP model. */
class PathwayNodeMapper {
    private Map<org.onebusaway.gtfs.model.Stop, PathwayNode> mappedNodes = new HashMap<>();

    Collection<PathwayNode> map(Collection<org.onebusaway.gtfs.model.Stop> allNodes) {
        return MapUtils.mapToList(allNodes, this::map);
    }

    /** Map from GTFS to OTP model, {@code null} safe.  */
    PathwayNode map(org.onebusaway.gtfs.model.Stop original) {
        return map(original, null);
    }
    PathwayNode map(org.onebusaway.gtfs.model.Stop original, I18NString nameTranslations) {
        return original == null ? null : mappedNodes.computeIfAbsent(original, k -> doMap(original, nameTranslations));
    }

    private PathwayNode doMap(org.onebusaway.gtfs.model.Stop gtfsStop, I18NString nameTranslations) {
        if (gtfsStop.getLocationType() != org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_NODE) {
            throw new IllegalArgumentException(
                "Expected type " + org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_NODE + ", but got "
                    + gtfsStop.getLocationType());
        }

        StopMappingWrapper base = new StopMappingWrapper(gtfsStop);

        return new PathwayNode(
            base.getId(),
            nameTranslations == null
                    ? new NonLocalizedString(base.getName())
                    : nameTranslations,
            base.getCode(),
            base.getDescription(),
            base.getCoordinate(),
            base.getWheelchairBoarding(),
            base.getLevel()
        );
    }
}

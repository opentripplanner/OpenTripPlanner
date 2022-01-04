package org.opentripplanner.gtfs.mapping;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.model.PathwayNode;
import org.opentripplanner.util.MapUtils;
import org.opentripplanner.util.TranslationHelper;

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
    PathwayNode map(org.onebusaway.gtfs.model.Stop original, TranslationHelper translationHelper) {
        return original == null ? null : mappedNodes.computeIfAbsent(original, k -> doMap(original, translationHelper));
    }

    private PathwayNode doMap(org.onebusaway.gtfs.model.Stop gtfsStop, TranslationHelper translationHelper) {
        if (gtfsStop.getLocationType() != org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_NODE) {
            throw new IllegalArgumentException(
                "Expected type " + org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_NODE + ", but got "
                    + gtfsStop.getLocationType());
        }

        StopMappingWrapper base = new StopMappingWrapper(gtfsStop);

        if (translationHelper != null) {
            return new PathwayNode(
                    base.getId(),
                    translationHelper.getTranslation(TranslationHelper.TABLE_STOPS,
                            TranslationHelper.STOP_NAME, base.getId().getId(),
                            null, base.getName()
                    ),
                    base.getCode(),
                    base.getDescription(),
                    base.getCoordinate(),
                    base.getWheelchairBoarding(),
                    base.getLevel()
            );
        }

        return new PathwayNode(
            base.getId(),
            base.getName(),
            base.getCode(),
            base.getDescription(),
            base.getCoordinate(),
            base.getWheelchairBoarding(),
            base.getLevel()
        );
    }
}

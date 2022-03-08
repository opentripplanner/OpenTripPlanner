package org.opentripplanner.gtfs.mapping;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.model.PathwayNode;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.MapUtils;
import org.opentripplanner.util.NonLocalizedString;
import org.opentripplanner.util.TranslationHelper;

/** Responsible for mapping GTFS Node into the OTP model. */
class PathwayNodeMapper {
    private final Map<org.onebusaway.gtfs.model.Stop, PathwayNode> mappedNodes = new HashMap<>();

    private final TranslationHelper translationHelper;

    PathwayNodeMapper(TranslationHelper translationHelper) {
        this.translationHelper = translationHelper;
    }

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

        Field nameField;
        try {
            nameField = org.onebusaway.gtfs.model.Stop.class.getDeclaredField("name");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }

        final I18NString name = translationHelper.getTranslation(
            nameField,
            base.getId().getId(),
            null,
            base.getName()
        );


        return new PathwayNode(
            base.getId(),
            name,
            base.getCode(),
            base.getDescription(),
            base.getCoordinate(),
            base.getWheelchairBoarding(),
            base.getLevel()
        );
    }
}

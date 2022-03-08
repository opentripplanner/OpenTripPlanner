package org.opentripplanner.gtfs.mapping;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.model.Entrance;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.MapUtils;
import org.opentripplanner.util.NonLocalizedString;
import org.opentripplanner.util.TranslationHelper;

/**
 * Responsible for mapping GTFS Entrance into the OTP model.
 */
class EntranceMapper {

    private final Map<org.onebusaway.gtfs.model.Stop, Entrance> mappedEntrances = new HashMap<>();

    private final TranslationHelper translationHelper;

    EntranceMapper(TranslationHelper translationHelper) {
        this.translationHelper = translationHelper;
    }

    Collection<Entrance> map(Collection<org.onebusaway.gtfs.model.Stop> allEntrances) {
        return MapUtils.mapToList(allEntrances, this::map);
    }

    /** Map from GTFS to OTP model, {@code null} safe. */
    Entrance map(org.onebusaway.gtfs.model.Stop orginal) {
        return orginal == null ? null : mappedEntrances.computeIfAbsent(orginal, this::doMap);
    }

    private Entrance doMap(org.onebusaway.gtfs.model.Stop gtfsStop) {
        if (gtfsStop.getLocationType()
                != org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_ENTRANCE_EXIT) {
            throw new IllegalArgumentException(
                    "Expected type " + org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_ENTRANCE_EXIT
                            + ", but got " + gtfsStop.getLocationType());
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
                base.getName());

        return new Entrance(
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

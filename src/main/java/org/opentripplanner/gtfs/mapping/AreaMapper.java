package org.opentripplanner.gtfs.mapping;

import org.opentripplanner.model.Area;
import org.opentripplanner.util.MapUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** Responsible for mapping GTFS Area into the OTP model. */
class AreaMapper {

    private final Map<org.onebusaway.gtfs.model.Area, Area> mappedAreas = new HashMap<>();

    Collection<Area> map(Collection<org.onebusaway.gtfs.model.Area> agencies) {
        return MapUtils.mapToList(agencies, this::map);
    }

    /** Map from GTFS to OTP model, {@code null} safe.  */
    Area map(org.onebusaway.gtfs.model.Area orginal) {
        return orginal == null ? null : mappedAreas.computeIfAbsent(orginal, this::doMap);
    }

    private Area doMap(org.onebusaway.gtfs.model.Area rhs) {
        Area lhs = new Area();

        lhs.setId(AgencyAndIdMapper.mapAgencyAndId(rhs.getId()));
        lhs.setWkt(rhs.getWkt());

        return lhs;
    }
}

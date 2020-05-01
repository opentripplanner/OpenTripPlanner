package org.opentripplanner.gtfs.mapping;

import org.opentripplanner.model.FlexArea;
import org.opentripplanner.util.MapUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** Map from the OBA model of GTFS-flex areas to the OTP internal model of areas. */
class AreaMapper {

    private final Map<org.onebusaway.gtfs.model.Area, FlexArea> mappedAreas = new HashMap<>();

    Collection<FlexArea> map(Collection<org.onebusaway.gtfs.model.Area> agencies) {
        return MapUtils.mapToList(agencies, this::map);
    }

    /** Map from the OBA model of GTFS-flex areas to the OTP internal model of areas.  */
    FlexArea map(org.onebusaway.gtfs.model.Area orginal) {
        return orginal == null ? null : mappedAreas.computeIfAbsent(orginal, this::doMap);
    }

    private FlexArea doMap(org.onebusaway.gtfs.model.Area rhs) {
        FlexArea lhs = new FlexArea();

        lhs.setId(AgencyAndIdMapper.mapAgencyAndId(rhs.getId()));
        lhs.setWkt(rhs.getWkt());

        return lhs;
    }
}

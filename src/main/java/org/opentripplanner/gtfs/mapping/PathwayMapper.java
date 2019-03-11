package org.opentripplanner.gtfs.mapping;

import org.opentripplanner.model.Pathway;
import org.opentripplanner.util.MapUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** Responsible for mapping GTFS Pathway into the OTP model. */
class PathwayMapper {

    private final StopMapper stopMapper;

    private Map<org.onebusaway.gtfs.model.Pathway, Pathway> mappedPathways = new HashMap<>();

    PathwayMapper(StopMapper stopMapper) {
        this.stopMapper = stopMapper;
    }

    Collection<Pathway> map(Collection<org.onebusaway.gtfs.model.Pathway> allPathways) {
        return MapUtils.mapToList(allPathways, this::map);
    }

    /** Map from GTFS to OTP model, {@code null} safe.  */
    Pathway map(org.onebusaway.gtfs.model.Pathway orginal) {
        return orginal == null ? null : mappedPathways.computeIfAbsent(orginal, this::doMap);
    }

    private Pathway doMap(org.onebusaway.gtfs.model.Pathway rhs) {
        Pathway lhs = new Pathway();

        lhs.setId(AgencyAndIdMapper.mapAgencyAndId(rhs.getId()));
        lhs.setPathwayType(rhs.getPathwayType());
        lhs.setFromStop(stopMapper.map(rhs.getFromStop()));
        lhs.setToStop(stopMapper.map(rhs.getToStop()));
        lhs.setTraversalTime(rhs.getTraversalTime());
        lhs.setWheelchairTraversalTime(rhs.getWheelchairTraversalTime());

        return lhs;
    }
}

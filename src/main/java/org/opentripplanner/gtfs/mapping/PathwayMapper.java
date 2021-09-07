package org.opentripplanner.gtfs.mapping;

import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.model.Pathway;
import org.opentripplanner.model.StationElement;
import org.opentripplanner.util.MapUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** Responsible for mapping GTFS Pathway into the OTP model. */
class PathwayMapper {

    private final StopMapper stopMapper;

    private EntranceMapper entranceMapper;

    private PathwayNodeMapper nodeMapper;

    private BoardingAreaMapper boardingAreaMapper;

    private Map<org.onebusaway.gtfs.model.Pathway, Pathway> mappedPathways = new HashMap<>();

    PathwayMapper(
        StopMapper stopMapper,
        EntranceMapper entranceMapper,
        PathwayNodeMapper nodeMapper,
        BoardingAreaMapper boardingAreaMapper
    ) {
        this.stopMapper = stopMapper;
        this.entranceMapper = entranceMapper;
        this.nodeMapper = nodeMapper;
        this.boardingAreaMapper = boardingAreaMapper;
    }

    Collection<Pathway> map(Collection<org.onebusaway.gtfs.model.Pathway> allPathways) {
        return MapUtils.mapToList(allPathways, this::map);
    }

    /** Map from GTFS to OTP model, {@code null} safe.  */
    Pathway map(org.onebusaway.gtfs.model.Pathway orginal) {
        return orginal == null ? null : mappedPathways.computeIfAbsent(orginal, this::doMap);
    }

    private Pathway doMap(org.onebusaway.gtfs.model.Pathway rhs) {
        Pathway lhs = new Pathway(AgencyAndIdMapper.mapAgencyAndId(rhs.getId()));

        lhs.setPathwayMode(rhs.getPathwayMode());
        if (rhs.isTraversalTimeSet()) { lhs.setTraversalTime(rhs.getTraversalTime()); }
        lhs.setName(rhs.getSignpostedAs());
        lhs.setReversedName(rhs.getReversedSignpostedAs());
        if (rhs.isLengthSet()) { lhs.setLength(rhs.getLength()); }
        if (rhs.isStairCountSet()) { lhs.setStairCount(rhs.getStairCount()); }
        if (rhs.isMaxSlopeSet()) { lhs.setSlope(rhs.getMaxSlope()); }
        lhs.setBidirectional(rhs.getIsBidirectional() == 1);

        lhs.setFromStop(mapStationElement(rhs.getFromStop()));
        lhs.setToStop(mapStationElement(rhs.getToStop()));
        return lhs;
    }

    private StationElement mapStationElement (Stop stop) {
        if (stop != null) {
            switch (stop.getLocationType()) {
                case org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_STOP:
                    return stopMapper.map(stop);
                case org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_ENTRANCE_EXIT:
                    return entranceMapper.map(stop);
                case org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_NODE:
                    return nodeMapper.map(stop);
                case org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_BOARDING_AREA:
                    return boardingAreaMapper.map(stop);
            }
        }
        // Stop was missing (null) or locationType was not valid (e.g. it was a station or missing)
        return null;
    }

}

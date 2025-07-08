package org.opentripplanner.gtfs.mapping;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.transit.model.site.Pathway;
import org.opentripplanner.transit.model.site.PathwayBuilder;
import org.opentripplanner.transit.model.site.StationElement;
import org.opentripplanner.utils.collection.MapUtils;

/** Responsible for mapping GTFS Pathway into the OTP model. */
class PathwayMapper {

  private final IdFactory idFactory;
  private final StopMapper stopMapper;

  private final EntranceMapper entranceMapper;

  private final PathwayNodeMapper nodeMapper;

  private final BoardingAreaMapper boardingAreaMapper;

  private final Map<org.onebusaway.gtfs.model.Pathway, Pathway> mappedPathways = new HashMap<>();

  PathwayMapper(
    IdFactory idFactory,
    StopMapper stopMapper,
    EntranceMapper entranceMapper,
    PathwayNodeMapper nodeMapper,
    BoardingAreaMapper boardingAreaMapper
  ) {
    this.idFactory = idFactory;
    this.stopMapper = stopMapper;
    this.entranceMapper = entranceMapper;
    this.nodeMapper = nodeMapper;
    this.boardingAreaMapper = boardingAreaMapper;
  }

  Collection<Pathway> map(Collection<org.onebusaway.gtfs.model.Pathway> allPathways) {
    return MapUtils.mapToList(allPathways, this::map);
  }

  /** Map from GTFS to OTP model, {@code null} safe. */
  Pathway map(org.onebusaway.gtfs.model.Pathway orginal) {
    return orginal == null ? null : mappedPathways.computeIfAbsent(orginal, this::doMap);
  }

  private Pathway doMap(org.onebusaway.gtfs.model.Pathway rhs) {
    PathwayBuilder pathway = Pathway.of(idFactory.createId(rhs.getId(), "pathway"))
      .withPathwayMode(PathwayModeMapper.map(rhs.getPathwayMode()))
      .withFromStop(mapStationElement(rhs.getFromStop()))
      .withToStop(mapStationElement(rhs.getToStop()))
      .withSignpostedAs(rhs.getSignpostedAs())
      .withReverseSignpostedAs(rhs.getReversedSignpostedAs())
      .withIsBidirectional(rhs.getIsBidirectional() == 1);

    if (rhs.isTraversalTimeSet()) {
      pathway.withTraversalTime(rhs.getTraversalTime());
    }
    if (rhs.isLengthSet()) {
      pathway.withLength(rhs.getLength());
    }
    if (rhs.isStairCountSet()) {
      pathway.withStairCount(rhs.getStairCount());
    }
    if (rhs.isMaxSlopeSet()) {
      pathway.withSlope(rhs.getMaxSlope());
    }

    return pathway.build();
  }

  private StationElement<?, ?> mapStationElement(Stop stop) {
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

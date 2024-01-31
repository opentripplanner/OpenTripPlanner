package org.opentripplanner.transit.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.framework.collection.CollectionsView;
import org.opentripplanner.framework.geometry.HashGridSpatialIndex;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.site.GroupStop;
import org.opentripplanner.transit.model.site.MultiModalStation;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StopLocation;

/**
 * Indexed access to Stop entities.
 * For performance reasons these indexes are not part of the serialized state of the graph.
 * They are rebuilt at runtime after graph deserialization.
 */
class StopModelIndex {

  private final HashGridSpatialIndex<RegularStop> regularStopSpatialIndex = new HashGridSpatialIndex<>();
  private final Map<Station, MultiModalStation> multiModalStationForStations = new HashMap<>();
  private final HashGridSpatialIndex<AreaStop> areaStopSpatialIndex = new HashGridSpatialIndex<>();
  private final HashGridSpatialIndex<GroupStop> groupStopSpatialIndex = new HashGridSpatialIndex<>();
  private final StopLocation[] stopsByIndex;

  /**
   * @param stops All stops including regular transit and flex
   */
  StopModelIndex(
    Collection<RegularStop> stops,
    Collection<AreaStop> flexStops,
    Collection<GroupStop> groupStops,
    Collection<MultiModalStation> multiModalStations,
    int indexSize
  ) {
    stopsByIndex = new StopLocation[indexSize];

    var allStops = new CollectionsView<StopLocation>(stops, flexStops, groupStops);
    for (StopLocation it : allStops) {
      stopsByIndex[it.getIndex()] = it;
    }

    for (MultiModalStation it : multiModalStations) {
      for (Station childStation : it.getChildStations()) {
        multiModalStationForStations.put(childStation, it);
      }
    }
    for (RegularStop regularStop : stops) {
      var envelope = new Envelope(regularStop.getCoordinate().asJtsCoordinate());
      regularStopSpatialIndex.insert(envelope, regularStop);
    }
    for (AreaStop areaStop : flexStops) {
      areaStopSpatialIndex.insert(areaStop.getGeometry().getEnvelopeInternal(), areaStop);
    }
    // We only want groupStops with areas in our spatial index. The ones that are defined as a list
    // of stops have already had the geometries of their child stops added to the regular stop index.
    Collection<GroupStop> groupStopsWithAreas = groupStops
      .stream()
      .filter(it -> it.getEncompassingAreaGeometry().isPresent())
      .toList();
    for (GroupStop groupStop : groupStopsWithAreas) {
      Envelope env = groupStop
        .getEncompassingAreaGeometry()
        .map(Geometry::getEnvelopeInternal)
        .orElseThrow(); // Because we filtered out the ones without areas, this should never throw.
      groupStopSpatialIndex.insert(env, groupStop);
    }

    // Trim the sizes of the indices
    regularStopSpatialIndex.compact();
    areaStopSpatialIndex.compact();
    groupStopSpatialIndex.compact();
  }

  /**
   * Find a regular stop in the spatial index
   */
  Collection<RegularStop> findRegularStops(Envelope envelope) {
    return regularStopSpatialIndex.query(envelope);
  }

  MultiModalStation getMultiModalStationForStation(Station station) {
    return multiModalStationForStations.get(station);
  }

  StopLocation stopByIndex(int index) {
    return stopsByIndex[index];
  }

  int stopIndexSize() {
    return stopsByIndex.length;
  }

  Collection<AreaStop> findAreaStops(Envelope envelope) {
    return areaStopSpatialIndex.query(envelope);
  }

  Collection<GroupStop> findGroupStops(Envelope envelope) {
    return groupStopSpatialIndex.query(envelope);
  }
}

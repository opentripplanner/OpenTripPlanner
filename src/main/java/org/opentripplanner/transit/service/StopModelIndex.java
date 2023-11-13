package org.opentripplanner.transit.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.locationtech.jts.geom.Envelope;
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
  private final HashGridSpatialIndex<AreaStop> locationIndex = new HashGridSpatialIndex<>();
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
      if (it instanceof RegularStop regularStop) {
        var envelope = new Envelope(it.getCoordinate().asJtsCoordinate());
        regularStopSpatialIndex.insert(envelope, regularStop);
      }
      stopsByIndex[it.getIndex()] = it;
    }

    for (MultiModalStation it : multiModalStations) {
      for (Station childStation : it.getChildStations()) {
        multiModalStationForStations.put(childStation, it);
      }
    }
    for (AreaStop it : flexStops) {
      locationIndex.insert(it.getGeometry().getEnvelopeInternal(), it);
    }

    // Trim the sizes of the indices
    regularStopSpatialIndex.compact();
    locationIndex.compact();
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
    return locationIndex.query(envelope);
  }
}

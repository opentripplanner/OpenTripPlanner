package org.opentripplanner.transit.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.common.geometry.HashGridSpatialIndex;
import org.opentripplanner.transit.model.site.FlexLocationGroup;
import org.opentripplanner.transit.model.site.FlexStopLocation;
import org.opentripplanner.transit.model.site.MultiModalStation;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.util.lang.CollectionsView;

/**
 * Indexed access to Stop entities.
 * For performance reasons these indexes are not part of the serialized state of the graph.
 * They are rebuilt at runtime after graph deserialization.
 */
class StopModelIndex {

  private final HashGridSpatialIndex<RegularStop> stopSpatialIndex = new HashGridSpatialIndex<>();
  private final Map<Station, MultiModalStation> multiModalStationForStations = new HashMap<>();
  private final HashGridSpatialIndex<FlexStopLocation> locationIndex = new HashGridSpatialIndex<>();
  private final StopLocation[] stopsByIndex;

  /**
   * @param stops All stops including regular transit and flex
   */
  StopModelIndex(
    Collection<RegularStop> stops,
    Collection<FlexStopLocation> flexStops,
    Collection<FlexLocationGroup> flexLocationGroups,
    Collection<MultiModalStation> multiModalStations
  ) {
    stopsByIndex = new StopLocation[StopLocation.indexCounter()];

    var allStops = new CollectionsView<StopLocation>(stops, flexStops, flexLocationGroups);
    for (StopLocation it : allStops) {
      Envelope envelope = new Envelope(it.getCoordinate().asJtsCoordinate());
      stopSpatialIndex.insert(envelope, it);
      stopsByIndex[it.getIndex()] = it;
    }

    for (MultiModalStation it : multiModalStations) {
      for (Station childStation : it.getChildStations()) {
        multiModalStationForStations.put(childStation, it);
      }
    }
    for (FlexStopLocation it : flexStops) {
      locationIndex.insert(it.getGeometry().getEnvelopeInternal(), it);
    }
  }

  Collection<RegularStop> queryStopSpatialIndex(Envelope envelope) {
    return stopSpatialIndex.query(envelope);
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

  Collection<FlexStopLocation> queryLocationIndex(Envelope envelope) {
    return locationIndex.query(envelope);
  }
}

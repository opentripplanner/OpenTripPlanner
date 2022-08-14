package org.opentripplanner.transit.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.common.geometry.HashGridSpatialIndex;
import org.opentripplanner.transit.model.site.FlexStopLocation;
import org.opentripplanner.transit.model.site.MultiModalStation;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.Stop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Indexed access to Stop entities.
 * For performance reasons these indexes are not part of the serialized state of the graph.
 * They are rebuilt at runtime after graph deserialization.
 */
class StopModelIndex {

  private static final Logger LOG = LoggerFactory.getLogger(StopModelIndex.class);

  // TODO: consistently key on model object or id string

  private final HashGridSpatialIndex<Stop> stopSpatialIndex = new HashGridSpatialIndex<>();
  private final Map<Station, MultiModalStation> multiModalStationForStations = new HashMap<>();
  private final HashGridSpatialIndex<FlexStopLocation> locationIndex = new HashGridSpatialIndex<>();
  private final StopLocation[] stopsByIndex;

  /**
   * @param stops All stops including regular transit and flex
   */
  public StopModelIndex(
    Collection<StopLocation> stops,
    Collection<MultiModalStation> multiModalStations,
    Collection<FlexStopLocation> flexStopLocations
  ) {
    stopsByIndex = new StopLocation[StopLocation.indexCounter()];

    for (StopLocation it : stops) {
      Envelope envelope = new Envelope(it.getCoordinate().asJtsCoordinate());
      stopSpatialIndex.insert(envelope, it);
      stopsByIndex[it.getIndex()] = it;
    }

    for (MultiModalStation it : multiModalStations) {
      for (Station childStation : it.getChildStations()) {
        multiModalStationForStations.put(childStation, it);
      }
    }
    for (FlexStopLocation it : flexStopLocations) {
      locationIndex.insert(it.getGeometry().getEnvelopeInternal(), it);
    }
  }

  public Collection<Stop> queryStopSpatialIndex(Envelope envelope) {
    return stopSpatialIndex.query(envelope);
  }

  public MultiModalStation getMultiModalStationForStation(Station station) {
    return multiModalStationForStations.get(station);
  }

  public StopLocation stopByIndex(int index) {
    return stopsByIndex[index];
  }

  public int stopIndexSize() {
    return stopsByIndex.length;
  }

  public Collection<FlexStopLocation> queryLocationIndex(Envelope envelope) {
    return locationIndex.query(envelope);
  }
}

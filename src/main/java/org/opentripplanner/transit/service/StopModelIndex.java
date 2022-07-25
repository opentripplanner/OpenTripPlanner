package org.opentripplanner.transit.service;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.common.geometry.HashGridSpatialIndex;
import org.opentripplanner.model.FlexLocationGroup;
import org.opentripplanner.model.FlexStopLocation;
import org.opentripplanner.model.MultiModalStation;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.StopIndexForRaptor;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.opentripplanner.transit.model.framework.FeedScopedId;
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
public class StopModelIndex implements StopIndexForRaptor {

  private static final Logger LOG = LoggerFactory.getLogger(StopModelIndex.class);

  // TODO: consistently key on model object or id string

  private final Map<Stop, TransitStopVertex> stopVertexForStop = Maps.newHashMap();
  private final HashGridSpatialIndex<TransitStopVertex> stopSpatialIndex = new HashGridSpatialIndex<>();

  private final Map<Station, MultiModalStation> multiModalStationForStations = Maps.newHashMap();

  public final Multimap<StopLocation, FlexLocationGroup> locationGroupsByStop = ArrayListMultimap.create();

  public final HashGridSpatialIndex<FlexStopLocation> locationIndex = new HashGridSpatialIndex<>();

  private final List<StopLocation> stopsByIndex;
  private final Map<StopLocation, Integer> indexByStop = new HashMap<>();

  public StopModelIndex(StopModel stopModel) {
    LOG.info("StopModelIndex init...");

    /* We will keep a separate set of all vertices in case some have the same label.
     * Maybe we should just guarantee unique labels. */
    for (TransitStopVertex stopVertex : stopModel.getAllStopVertices()) {
      Stop stop = stopVertex.getStop();
      stopVertexForStop.put(stop, stopVertex);
    }
    for (TransitStopVertex stopVertex : stopVertexForStop.values()) {
      Envelope envelope = new Envelope(stopVertex.getCoordinate());
      stopSpatialIndex.insert(envelope, stopVertex);
    }

    /* We will keep a separate set of all vertices in case some have the same label.
     * Maybe we should just guarantee unique labels. */
    for (TransitStopVertex stopVertex : stopModel.getAllStopVertices()) {
      Stop stop = stopVertex.getStop();
      stopForId.put(stop.getId(), stop);
      stopVertexForStop.put(stop, stopVertex);
    }
    for (TransitStopVertex stopVertex : stopVertexForStop.values()) {
      Envelope envelope = new Envelope(stopVertex.getCoordinate());
      stopSpatialIndex.insert(envelope, stopVertex);
    }

    for (MultiModalStation multiModalStation : stopModel.getAllMultiModalStations()) {
      for (Station childStation : multiModalStation.getChildStations()) {
        multiModalStationForStations.put(childStation, multiModalStation);
      }
    }
    for (FlexLocationGroup flexLocationGroup : stopModel.getAllFlexLocationGroups()) {
      for (StopLocation stop : flexLocationGroup.getLocations()) {
        locationGroupsByStop.put(stop, flexLocationGroup);
      }
      stopForId.put(flexLocationGroup.getId(), flexLocationGroup);
    }
    for (FlexStopLocation flexStopLocation : stopModel.getAllFlexLocations()) {
      locationIndex.insert(flexStopLocation.getGeometry().getEnvelopeInternal(), flexStopLocation);
      stopForId.put(flexStopLocation.getId(), flexStopLocation);
    }

    this.stopsByIndex = List.copyOf(stopForId.values());
    for (int i = 0; i < stopsByIndex.size(); ++i) {
      indexByStop.put(stopsByIndex.get(i), i);
    }

    LOG.info("StopModelIndex init complete.");
  }

  public Map<Stop, TransitStopVertex> getStopVertexForStop() {
    return stopVertexForStop;
  }

  public HashGridSpatialIndex<TransitStopVertex> getStopSpatialIndex() {
    return stopSpatialIndex;
  }

  private final Map<FeedScopedId, StopLocation> stopForId = Maps.newHashMap();

  public StopLocation getStopForId(FeedScopedId id) {
    return stopForId.get(id);
  }

  public Map<Station, MultiModalStation> getMultiModalStationForStations() {
    return multiModalStationForStations;
  }

  public Collection<StopLocation> getAllStops() {
    return stopForId.values();
  }

  @Override
  public StopLocation stopByIndex(int index) {
    return stopsByIndex.get(index);
  }

  @Override
  public int indexOf(StopLocation stop) {
    return indexByStop.get(stop);
  }

  @Override
  public int size() {
    return stopsByIndex.size();
  }
}

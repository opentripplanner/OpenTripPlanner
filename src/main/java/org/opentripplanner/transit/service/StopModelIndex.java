package org.opentripplanner.transit.service;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Map;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.common.geometry.HashGridSpatialIndex;
import org.opentripplanner.model.FlexLocationGroup;
import org.opentripplanner.model.FlexStopLocation;
import org.opentripplanner.model.MultiModalStation;
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
public class StopModelIndex {

  private static final Logger LOG = LoggerFactory.getLogger(StopModelIndex.class);

  // TODO: consistently key on model object or id string

  private final Map<Stop, TransitStopVertex> stopVertexForStop = Maps.newHashMap();
  private final HashGridSpatialIndex<TransitStopVertex> stopSpatialIndex = new HashGridSpatialIndex<>();

  private final Map<Station, MultiModalStation> multiModalStationForStations = Maps.newHashMap();

  public Multimap<StopLocation, FlexLocationGroup> locationGroupsByStop = ArrayListMultimap.create();

  public HashGridSpatialIndex<FlexStopLocation> locationIndex = new HashGridSpatialIndex<>();

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
    for (FlexLocationGroup flexLocationGroup : stopModel.locationGroupsById.values()) {
      for (StopLocation stop : flexLocationGroup.getLocations()) {
        locationGroupsByStop.put(stop, flexLocationGroup);
      }
    }
    for (FlexStopLocation flexStopLocation : stopModel.locationsById.values()) {
      locationIndex.insert(flexStopLocation.getGeometry().getEnvelopeInternal(), flexStopLocation);
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

  public void addStop(StopLocation stopLocation) {
    stopForId.put(stopLocation.getId(), stopLocation);
  }

  public Map<Station, MultiModalStation> getMultiModalStationForStations() {
    return multiModalStationForStations;
  }

  public Collection<StopLocation> getAllStops() {
    return stopForId.values();
  }
}

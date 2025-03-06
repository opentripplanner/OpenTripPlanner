package org.opentripplanner.transit.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.framework.geometry.HashGridSpatialIndex;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.site.GroupStop;
import org.opentripplanner.transit.model.site.MultiModalStation;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.utils.collection.CollectionsView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

/**
 * Indexed access to Stop entities.
 * For performance reasons these indexes are not part of the serialized state of the graph.
 * They are rebuilt at runtime after graph deserialization.
 */
class SiteRepositoryIndex {

  private static final Logger LOG = LoggerFactory.getLogger(SiteRepositoryIndex.class);

  private final HashGridSpatialIndex<RegularStop> regularStopSpatialIndex =
    new HashGridSpatialIndex<>();
  private final Map<Station, MultiModalStation> multiModalStationForStations = new HashMap<>();
  private final HashGridSpatialIndex<AreaStop> locationIndex = new HashGridSpatialIndex<>();
  private final StopLocation[] stopsByIndex;

  /**
   * @param stops All stops including regular transit and flex
   */
  SiteRepositoryIndex(
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

    logHolesInIndex();
  }

  /**
   * Find a regular stop in the spatial index, where the stop is inside of the passed Envelope.
   *
   * @param envelope - The {@link Envelope} to search for stops in.
   * @return A collection of {@link RegularStop}s that are inside of the passed envelope.
   */
  Collection<RegularStop> findRegularStops(Envelope envelope) {
    return regularStopSpatialIndex
      .query(envelope)
      .stream()
      .filter(stop -> envelope.contains(stop.getCoordinate().asJtsCoordinate()))
      .toList();
  }

  MultiModalStation getMultiModalStationForStation(Station station) {
    return multiModalStationForStations.get(station);
  }

  @Nullable
  StopLocation stopByIndex(int index) {
    return stopsByIndex[index];
  }

  int stopIndexSize() {
    return stopsByIndex.length;
  }

  Collection<AreaStop> findAreaStops(Envelope envelope) {
    return locationIndex.query(envelope);
  }

  /**
   * A small number of holes in the stop-index is ok, but if there are many, it will affect
   * the Raptor performance.
   */
  private void logHolesInIndex() {
    int c = (int) Arrays.stream(stopsByIndex).filter(Objects::isNull).count();
    if (c > 0) {
      double p = (100.0 * c) / stopsByIndex.length;
      // Log this as warning if more than 5% of the space is null
      LOG.atLevel(p >= 5.0 ? Level.WARN : Level.INFO).log(
        "The stop index contains holes in it. {} of {} is null.",
        c,
        stopsByIndex.length
      );
    }
  }
}

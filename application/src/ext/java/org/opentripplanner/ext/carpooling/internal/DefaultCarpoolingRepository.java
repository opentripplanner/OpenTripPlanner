package org.opentripplanner.ext.carpooling.internal;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import graphql.org.antlr.v4.runtime.misc.MultiMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.Nullable;
import org.locationtech.jts.geom.Point;
import org.opentripplanner.ext.carpooling.CarpoolingRepository;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.AreaStop;

public class DefaultCarpoolingRepository implements CarpoolingRepository {

  private final Graph graph;

  private final Map<FeedScopedId, CarpoolTrip> trips = new ConcurrentHashMap<>();

  private final Map<FeedScopedId, CarpoolTrip> boardingAreas = new ConcurrentHashMap<>();
  private final Map<FeedScopedId, CarpoolTrip> alightingAreas = new ConcurrentHashMap<>();

  private final ArrayListMultimap<StreetVertex, AreaStop> boardingAreasByVertex =
    ArrayListMultimap.create();
  private final ArrayListMultimap<StreetVertex, AreaStop> alightingAreasByVertex =
    ArrayListMultimap.create();

  public DefaultCarpoolingRepository(Graph graph) {
    this.graph = graph;
  }

  @Override
  public Collection<CarpoolTrip> getCarpoolTrips() {
    return trips.values();
  }

  @Override
  public void addCarpoolTrip(CarpoolTrip trip) {
    trips.put(trip.getId(), trip);

    var boardingArea = trip.getBoardingArea();
    var alightingArea = trip.getAlightingArea();

    boardingAreas.put(boardingArea.getId(), trip);
    alightingAreas.put(alightingArea.getId(), trip);

    streetVerticesWithinAreaStop(boardingArea).forEach(v -> {
      boardingAreasByVertex.put(v, boardingArea);
    });

    streetVerticesWithinAreaStop(alightingArea).forEach(v -> {
      alightingAreasByVertex.put(v, alightingArea);
    });
  }

  private List<StreetVertex> streetVerticesWithinAreaStop(AreaStop stop) {
    return graph
      .findVertices(stop.getGeometry().getEnvelopeInternal())
      .stream()
      .filter(StreetVertex.class::isInstance)
      .map(StreetVertex.class::cast)
      .filter(StreetVertex::isEligibleForCarPickupDropoff)
      .filter(vertx -> {
        // The street index overselects, so need to check for exact geometry inclusion
        Point p = GeometryUtils.getGeometryFactory().createPoint(vertx.getCoordinate());
        return stop.getGeometry().intersects(p);
      })
      .toList();
  }

  @Override
  public void removeCarpoolTrip(FeedScopedId tripId) {
    trips.remove(tripId);
  }

  @Override
  public CarpoolTrip getCarpoolTrip(FeedScopedId tripId) {
    return trips.get(tripId);
  }

  @Override
  public boolean isCarpoolBoardingArea(FeedScopedId areaId) {
    return boardingAreas.containsKey(areaId);
  }

  @Override
  public boolean isCarpoolAlightingArea(FeedScopedId areaId) {
    return alightingAreas.containsKey(areaId);
  }

  @Override
  public CarpoolTrip getCarpoolTripByBoardingArea(FeedScopedId boardingAreaId) {
    return boardingAreas.get(boardingAreaId);
  }

  @Override
  public CarpoolTrip getCarpoolTripByAlightingArea(FeedScopedId alightingAreaId) {
    return alightingAreas.get(alightingAreaId);
  }

  @Override
  public ArrayListMultimap<StreetVertex, AreaStop> getBoardingAreasForVertex() {
    return boardingAreasByVertex;
  }

  @Override
  public ArrayListMultimap<StreetVertex, AreaStop> getAlightingAreasForVertex() {
    return alightingAreasByVertex;
  }
}

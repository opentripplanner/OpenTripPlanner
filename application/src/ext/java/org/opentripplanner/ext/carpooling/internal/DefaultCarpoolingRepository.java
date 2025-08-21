package org.opentripplanner.ext.carpooling.internal;

import com.google.common.collect.ArrayListMultimap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.locationtech.jts.geom.Point;
import org.opentripplanner.ext.carpooling.CarpoolingRepository;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.AreaStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultCarpoolingRepository implements CarpoolingRepository {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultCarpoolingRepository.class);

  private final Graph graph;

  private final Map<FeedScopedId, CarpoolTrip> trips = new ConcurrentHashMap<>();

  private final Map<AreaStop, CarpoolTrip> boardingAreas = new ConcurrentHashMap<>();
  private final Map<AreaStop, CarpoolTrip> alightingAreas = new ConcurrentHashMap<>();

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

    boardingAreas.put(boardingArea, trip);
    alightingAreas.put(alightingArea, trip);

    streetVerticesWithinAreaStop(boardingArea).forEach(v -> {
      boardingAreasByVertex.put(v, boardingArea);
    });

    streetVerticesWithinAreaStop(alightingArea).forEach(v -> {
      alightingAreasByVertex.put(v, alightingArea);
    });
    LOG.info("Added carpooling trip for start time: {}", trip.getStartTime());
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
  public CarpoolTrip getCarpoolTripByBoardingArea(AreaStop boardingArea) {
    return boardingAreas.get(boardingArea);
  }

  @Override
  public CarpoolTrip getCarpoolTripByAlightingArea(AreaStop alightingArea) {
    return alightingAreas.get(alightingArea);
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

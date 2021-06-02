package org.opentripplanner.routing.graphfinder;

import com.beust.jcommander.internal.Lists;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.StreetVertexIndex;
import org.opentripplanner.routing.vertextype.TransitStopVertex;

/**
 * A Graph finder used in conjunction with a graph, which does not have a street network included.
 * Also usable if performance is more important or if the "as the crow flies" distance id required.
 */
public class DirectGraphFinder implements GraphFinder {

  private static GeometryFactory geometryFactory = GeometryUtils.getGeometryFactory();

  private StreetVertexIndex streetIndex;

  public DirectGraphFinder(Graph graph) {
    this.streetIndex = graph.getStreetIndex();
  }

  /**
   * Return all stops within a certain radius of the given vertex, using straight-line distance independent of streets.
   * If the origin vertex is a StopVertex, the result will include it.
   */
  @Override
  public List<NearbyStop> findClosestStops(double lat, double lon, double radiusMeters) {
    List<NearbyStop> stopsFound = Lists.newArrayList();
    Coordinate coordinate = new Coordinate(lon, lat);
    for (TransitStopVertex it : streetIndex.getNearbyTransitStops(coordinate, radiusMeters)) {
      double distance = Math.round(SphericalDistanceLibrary.distance(coordinate, it.getCoordinate()));
      if (distance < radiusMeters) {
        Coordinate coordinates[] = new Coordinate[] {coordinate, it.getCoordinate()};
        NearbyStop sd = new NearbyStop(
            it,
            distance,
            null,
            geometryFactory.createLineString(coordinates),
            null
        );
        stopsFound.add(sd);
      }
    }

    stopsFound.sort(NearbyStop::compareTo);

    return stopsFound;
  }

  @Override
  public List<PlaceAtDistance> findClosestPlaces(
      double lat, double lon, double maxDistance, int maxResults, List<TransitMode> filterByModes,
      List<PlaceType> filterByPlaceTypes, List<FeedScopedId> filterByStops,
      List<FeedScopedId> filterByRoutes, List<String> filterByBikeRentalStations,
      List<String> filterByBikeParks, List<String> filterByCarParks, RoutingService routingService
  ) {
    throw new UnsupportedOperationException("Not implemented");
  }
}

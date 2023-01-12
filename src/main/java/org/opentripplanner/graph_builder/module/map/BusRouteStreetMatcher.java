package org.opentripplanner.graph_builder.module.map;

import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.logging.ProgressTracker;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.service.TransitModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Uses the shapes from GTFS to determine which streets buses drive on. This is used to improve the
 * quality of the shapes shown for the user.
 * <p>
 * GTFS provides a mapping from trips→shapes. This module provides a mapping from stops→trips and
 * shapes→edges. Then transitively we get a mapping from stop→edges.
 */
public class BusRouteStreetMatcher implements GraphBuilderModule {

  private static final Logger log = LoggerFactory.getLogger(BusRouteStreetMatcher.class);

  private final Graph graph;
  private final TransitModel transitModel;

  @Inject
  public BusRouteStreetMatcher(Graph graph, TransitModel transitModel) {
    this.graph = graph;
    this.transitModel = transitModel;
  }

  public void buildGraph() {
    // Mapbuilder needs transit index
    transitModel.index();
    graph.index(transitModel.getStopModel());

    StreetMatcher matcher = new StreetMatcher(graph);
    log.info("Finding corresponding street edges for trip patterns...");
    // Why do we need to iterate over the routes? Why not just patterns?
    Collection<Route> allRoutes = transitModel.getTransitModelIndex().getAllRoutes();

    // Track progress
    ProgressTracker progress = ProgressTracker.track(
      "Match route to street edges",
      10,
      allRoutes.size()
    );
    log.info(progress.startMessage());

    for (Route route : allRoutes) {
      for (TripPattern pattern : transitModel
        .getTransitModelIndex()
        .getPatternsForRoute()
        .get(route)) {
        if (pattern.getMode().onStreet()) {
          /* we can only match geometry to streets on bus routes */
          log.debug("Matching {}", pattern);
          //If there are no shapes in GTFS pattern geometry is generated
          //generated geometry is useless for street matching
          //that is why pattern.geometry is null in that case
          if (pattern.getGeometry() == null) {
            continue;
          }

          for (int i = 0; i < pattern.numHopGeometries(); i++) {
            LineString hopGeometry = pattern.getHopGeometry(i);

            List<Edge> edges = matcher.match(hopGeometry);
            if (edges == null || edges.isEmpty()) {
              log.warn("Could not match to street network: {}", pattern);
              continue;
            }
            List<Coordinate> coordinates = new ArrayList<>();
            for (Edge e : edges) {
              coordinates.addAll(Arrays.asList(e.getGeometry().getCoordinates()));
            }
            Coordinate[] coordinateArray = new Coordinate[coordinates.size()];
            LineString ls = GeometryUtils
              .getGeometryFactory()
              .createLineString(coordinates.toArray(coordinateArray));
            // Replace the hop's geometry from GTFS with that of the equivalent OSM edges.
            pattern.setHopGeometry(i, ls);
          }
        }
      }
      progress.step(log::info);
    }
    log.info(progress.completeMessage());
  }

  @Override
  public void checkInputs() {
    //no file inputs
  }
}

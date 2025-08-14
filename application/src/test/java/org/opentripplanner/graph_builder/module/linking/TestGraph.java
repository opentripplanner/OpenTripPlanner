package org.opentripplanner.graph_builder.module.linking;

import java.util.List;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.linking.VertexLinker;
import org.opentripplanner.street.model.edge.LinkingDirection;
import org.opentripplanner.street.model.edge.StreetTransitStopLink;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.TraverseModeSet;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.TimetableRepository;

/**
 * Get graphs of Columbus Ohio with real OSM streets and a synthetic transit system for use in
 * testing.
 */
class TestGraph {

  private static final TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();

  /**
   * Add a regular grid of stops to the graph. Note! Not all of these stops
   * are within 100m of a street and will not be linked to the street graph.
   */
  public static void addRegularStopGrid(Graph graph) {
    int count = 0;
    for (double lat = 39.91733; lat < 40.02811; lat += 0.005) {
      for (double lon = -83.09040; lon < -82.88389; lon += 0.005) {
        String id = Integer.toString(count++);
        RegularStop stop = TEST_MODEL.stop(id).withCoordinate(lat, lon).build();
        graph.addVertex(TransitStopVertex.of().withStop(stop).build());
      }
    }
  }

  /** add some extra stops to the graph */
  public static void addExtraStops(Graph graph) {
    int count = 0;
    double lon = -83;
    for (double lat = 40; lat < 40.01; lat += 0.005) {
      String id = "EXTRA_" + count++;
      RegularStop stop = TEST_MODEL.stop(id).withCoordinate(lat, lon).build();
      graph.addVertex(TransitStopVertex.of().withStop(stop).build());
    }

    // add some duplicate stops, identical to the regular stop grid
    lon = -83.1341 + 0.1;
    for (double lat = 39.9058; lat < 40.0281; lat += 0.005) {
      String id = "DUPE_" + count++;
      RegularStop stop = TEST_MODEL.stop(id).withCoordinate(lat, lon).build();
      graph.addVertex(TransitStopVertex.of().withStop(stop).build());
    }

    // add some almost duplicate stops
    lon = -83.1341 + 0.15;
    for (double lat = 39.9059; lat < 40.0281; lat += 0.005) {
      String id = "ALMOST_" + count++;
      RegularStop stop = TEST_MODEL.stop(id).withCoordinate(lat, lon).build();
      graph.addVertex(TransitStopVertex.of().withStop(stop).build());
    }
  }

  /** link the stops in the graph */
  public static void link(Graph graph, TimetableRepository timetableRepository) {
    timetableRepository.index();
    graph.index();

    VertexLinker linker = graph.getLinker();

    for (TransitStopVertex tStop : graph.getVerticesOfType(TransitStopVertex.class)) {
      linker.linkVertexPermanently(
        tStop,
        new TraverseModeSet(TraverseMode.WALK),
        LinkingDirection.BIDIRECTIONAL,
        (vertex, streetVertex) ->
          List.of(
            StreetTransitStopLink.createStreetTransitStopLink(
              (TransitStopVertex) vertex,
              streetVertex
            ),
            StreetTransitStopLink.createStreetTransitStopLink(
              streetVertex,
              (TransitStopVertex) vertex
            )
          )
      );
    }
  }
}

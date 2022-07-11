package org.opentripplanner.graph_builder.module;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.opentripplanner.OtpModel;
import org.opentripplanner.graph_builder.linking.LinkingDirection;
import org.opentripplanner.graph_builder.linking.VertexLinker;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.graph_builder.module.osm.DefaultWayPropertySetSource;
import org.opentripplanner.graph_builder.module.osm.OpenStreetMapModule;
import org.opentripplanner.model.calendar.ServiceDateInterval;
import org.opentripplanner.openstreetmap.OpenStreetMapProvider;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.StreetTransitStopLink;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.trippattern.Deduplicator;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.opentripplanner.routing.vertextype.TransitStopVertexBuilder;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.site.Stop;
import org.opentripplanner.transit.service.StopModel;
import org.opentripplanner.transit.service.TransitModel;

/**
 * Get graphs of Columbus Ohio with real OSM streets and a synthetic transit system for use in
 * testing.
 */
public class FakeGraph {

  /** Build a graph in Columbus, OH with no transit */
  public static OtpModel buildGraphNoTransit() throws URISyntaxException {
    var deduplicator = new Deduplicator();
    var stopModel = new StopModel();
    var gg = new Graph(stopModel, deduplicator);
    var transitModel = new TransitModel(stopModel, deduplicator);

    File file = getFileForResource("columbus.osm.pbf");
    OpenStreetMapProvider provider = new OpenStreetMapProvider(file, false);

    OpenStreetMapModule osmModule = new OpenStreetMapModule(provider);
    osmModule.setDefaultWayPropertySetSource(new DefaultWayPropertySetSource());

    osmModule.buildGraph(gg, transitModel, new HashMap<>());
    return new OtpModel(gg, transitModel);
  }

  public static File getFileForResource(String resource) throws URISyntaxException {
    URL resourceUrl = FakeGraph.class.getResource(resource);
    return new File(resourceUrl.toURI());
  }

  /**
   * Add many transit lines to a lot of stops. This is only used by InitialStopsTest.
   */
  public static void addTransitMultipleLines(Graph g, TransitModel transitModel)
    throws URISyntaxException {
    GtfsModule gtfs = new GtfsModule(
      Arrays.asList(new GtfsBundle(getFileForResource("addTransitMultipleLines.gtfs.zip"))),
      ServiceDateInterval.unbounded()
    );
    gtfs.buildGraph(g, transitModel, new HashMap<>());
  }

  /**
   * This introduces a 1MB test resource but is only used by TestIntermediatePlaces.
   */
  public static void addPerpendicularRoutes(Graph graph, TransitModel transitModel)
    throws URISyntaxException {
    GtfsModule gtfs = new GtfsModule(
      Arrays.asList(new GtfsBundle(getFileForResource("addPerpendicularRoutes.gtfs.zip"))),
      ServiceDateInterval.unbounded()
    );
    gtfs.buildGraph(graph, transitModel, new HashMap<>());
  }

  /** Add a regular grid of stops to the graph */
  public static void addRegularStopGrid(Graph g, TransitModel transitModel) {
    int count = 0;
    for (double lat = 39.9058; lat < 40.0281; lat += 0.005) {
      for (double lon = -83.1341; lon < -82.8646; lon += 0.005) {
        String id = Integer.toString(count++);
        Stop stop = TransitModelForTest.stop(id).withCoordinate(lat, lon).build();
        new TransitStopVertexBuilder()
          .withGraph(g)
          .withStop(stop)
          .withTransitModel(transitModel)
          .build();
      }
    }
  }

  /** add some extra stops to the graph */
  public static void addExtraStops(Graph g, TransitModel transitModel) {
    int count = 0;
    double lon = -83;
    for (double lat = 40; lat < 40.01; lat += 0.005) {
      String id = "EXTRA_" + count++;
      Stop stop = TransitModelForTest.stop(id).withCoordinate(lat, lon).build();
      new TransitStopVertexBuilder()
        .withGraph(g)
        .withStop(stop)
        .withTransitModel(transitModel)
        .build();
    }

    // add some duplicate stops, identical to the regular stop grid
    lon = -83.1341 + 0.1;
    for (double lat = 39.9058; lat < 40.0281; lat += 0.005) {
      String id = "DUPE_" + count++;
      Stop stop = TransitModelForTest.stop(id).withCoordinate(lat, lon).build();
      new TransitStopVertexBuilder()
        .withGraph(g)
        .withStop(stop)
        .withTransitModel(transitModel)
        .build();
    }

    // add some almost duplicate stops
    lon = -83.1341 + 0.15;
    for (double lat = 39.9059; lat < 40.0281; lat += 0.005) {
      String id = "ALMOST_" + count++;
      Stop stop = TransitModelForTest.stop(id).withCoordinate(lat, lon).build();
      new TransitStopVertexBuilder()
        .withGraph(g)
        .withStop(stop)
        .withTransitModel(transitModel)
        .build();
    }
  }

  /** link the stops in the graph */
  public static void link(Graph graph, TransitModel transitModel) {
    transitModel.index();
    graph.index();

    VertexLinker linker = graph.getLinker();

    for (TransitStopVertex tStop : graph.getVerticesOfType(TransitStopVertex.class)) {
      linker.linkVertexPermanently(
        tStop,
        new TraverseModeSet(TraverseMode.WALK),
        LinkingDirection.BOTH_WAYS,
        (vertex, streetVertex) ->
          List.of(
            new StreetTransitStopLink((TransitStopVertex) vertex, streetVertex),
            new StreetTransitStopLink(streetVertex, (TransitStopVertex) vertex)
          )
      );
    }
  }
}

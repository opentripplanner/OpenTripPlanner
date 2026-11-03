package org.opentripplanner.graph_builder.module.boardinglocations;

import static org.opentripplanner.routing.linking.TransitStopVertexBuilderFactory.ofStop;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;
import org.opentripplanner.graph_builder.module.BoardingLocationCoordinateSource;
import org.opentripplanner.graph_builder.module.OsmBoardingLocationsModule;
import org.opentripplanner.graph_builder.module.osm.OsmModuleTestFactory;
import org.opentripplanner.osm.OsmProvider;
import org.opentripplanner.routing.linking.VertexLinkerTestFactory;
import org.opentripplanner.service.osminfo.internal.DefaultOsmInfoGraphBuildRepository;
import org.opentripplanner.service.osminfo.internal.DefaultOsmInfoGraphBuildService;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.streetadapter.VertexFactory;
import org.opentripplanner.transit.model.TransitTestEnvironment;
import org.opentripplanner.transit.model.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model.site.RegularStop;

/**
 * Runs the real pipeline a boarding location depends on: {@code OsmModule} over OSM input, then
 * {@link OsmBoardingLocationsModule} over the stops, and hands the result to the test.
 * <p>
 * Building the OSM input with {@code TestOsmProvider} rather than by hand keeps the platform
 * geometry, the area visibility vertices and the platform registration exactly as a real build
 * produces them.
 */
public class BoardingLocationsEnvironment {

  /** The OSM tags a platform's references are read from, matching what the tests set. */
  private static final Set<String> REF_TAGS = Set.of("ref");

  private final BoardingLocationCoordinateSource coordinateSource;
  private final OsmProvider provider;
  private final TransitTestEnvironmentBuilder transit = TransitTestEnvironment.of();
  private final List<RegularStop> stops = new ArrayList<>();

  private BoardingLocationsEnvironment(
    BoardingLocationCoordinateSource coordinateSource,
    OsmProvider provider
  ) {
    this.coordinateSource = coordinateSource;
    this.provider = provider;
  }

  public static BoardingLocationsEnvironment of(
    BoardingLocationCoordinateSource coordinateSource,
    OsmProvider provider
  ) {
    return new BoardingLocationsEnvironment(coordinateSource, provider);
  }

  public RegularStop stop(String id, WgsCoordinate coordinate) {
    var stop = transit.stop(id, builder -> builder.withCoordinate(coordinate));
    stops.add(stop);
    return stop;
  }

  public LinkedGraph build() {
    var graph = new Graph();
    var osmInfoRepository = new DefaultOsmInfoGraphBuildRepository();

    OsmModuleTestFactory.of(provider)
      .withGraph(graph)
      .withOsmInfoGraphBuildRepository(osmInfoRepository)
      .builder()
      .withBoardingAreaRefTags(REF_TAGS)
      .withAreaVisibility(true)
      .withMaxAreaNodes(100)
      .build()
      .buildGraph();

    var transitRepository = transit.build().transitRepository();
    var vertexFactory = new VertexFactory(graph);
    stops.forEach(stop -> vertexFactory.transitStop(ofStop(stop)));
    graph.index();

    var issueStore = new DefaultDataImportIssueStore();
    new OsmBoardingLocationsModule(
      graph,
      transitRepository,
      VertexLinkerTestFactory.of(graph),
      new DefaultOsmInfoGraphBuildService(osmInfoRepository),
      coordinateSource,
      issueStore
    ).buildGraph();

    return new LinkedGraph(graph, issueStore);
  }
}

package org.opentripplanner.graph_builder.module;

import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.linking.LinkingDirection;
import org.opentripplanner.graph_builder.linking.VertexLinker;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.StreetBikeParkLink;
import org.opentripplanner.routing.edgetype.StreetBikeRentalLink;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.edgetype.TransitEntranceLink;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.BikeParkVertex;
import org.opentripplanner.routing.vertextype.BikeRentalStationVertex;
import org.opentripplanner.routing.vertextype.TransitEntranceVertex;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * {@link org.opentripplanner.graph_builder.services.GraphBuilderModule} plugin that links various
 * objects in the graph to the street network. It should be run after both the transit network and
 * street network are loaded. It links four things: transit stops, transit entrances, bike rental
 * stations, and bike parks. Therefore it should be run even when there's no GTFS data present
 * to make bike rental services and bike parks usable.
 */
public class StreetLinkerModule implements GraphBuilderModule {

  private static final Logger LOG = LoggerFactory.getLogger(StreetLinkerModule.class);

  public void setAddExtraEdgesToAreas(Boolean addExtraEdgesToAreas) {
    this.addExtraEdgesToAreas = addExtraEdgesToAreas;
  }

  public Boolean getAddExtraEdgesToAreas() {
    return addExtraEdgesToAreas;
  }

  private Boolean addExtraEdgesToAreas = true;

  public List<String> provides() {
    return Arrays.asList("street to transit", "linking");
  }

  public List<String> getPrerequisites() {
    return Arrays.asList("streets"); // don't include transit, because we also link P+Rs and bike rental stations,
    // which you could have without transit. However, if you have transit, this module should be run after it
    // is loaded.
  }

  @Override
  public void buildGraph(
      Graph graph, HashMap<Class<?>, Object> extra, DataImportIssueStore issueStore
  ) {
    VertexLinker linker = graph.getLinker();

    if (graph.hasStreets) {
      linkTransitStops(graph, linker);
      linkTransitEntrances(graph, linker);
      linkBikeRentals(graph, linker);
      linkBikeParks(graph, linker);
    }

    // Calculates convex hull of a graph which is shown in routerInfo API point
    graph.calculateConvexHull();
  }

  private void linkTransitStops(Graph graph, VertexLinker linker) {
    LOG.info("Linking transit stops to graph...");
    for (TransitStopVertex tStop : graph.getVerticesOfType(TransitStopVertex.class)) {
      linker.linkVertexPermanently(
          tStop,
          TraverseMode.WALK,
          LinkingDirection.BOTH_WAYS,
          (vertex, streetVertex) -> List.of(
              new StreetTransitLink((TransitStopVertex) vertex, streetVertex),
              new StreetTransitLink(streetVertex, (TransitStopVertex) vertex)
          )
      );
    }
  }

  private void linkTransitEntrances(Graph graph, VertexLinker linker) {
    LOG.info("Linking transit entrances to graph...");
    for (TransitEntranceVertex tEntrance : graph.getVerticesOfType(TransitEntranceVertex.class)) {
      linker.linkVertexPermanently(
          tEntrance,
          TraverseMode.WALK,
          LinkingDirection.BOTH_WAYS,
          (vertex, streetVertex) -> List.of(
              new TransitEntranceLink((TransitEntranceVertex) vertex, streetVertex),
              new TransitEntranceLink(streetVertex, (TransitEntranceVertex) vertex)
          )
      );
    }
  }

  private void linkBikeRentals(Graph graph, VertexLinker linker) {
    LOG.info("Linking bike rental stations to graph...");
      // It is enough to have the edges traversable by foot, as you can walk with the bike if necessary
    for (BikeRentalStationVertex bikeRental : graph.getVerticesOfType(BikeRentalStationVertex.class)) {
      linker.linkVertexPermanently(
          bikeRental,
          TraverseMode.WALK,
          LinkingDirection.BOTH_WAYS,
          (vertex, streetVertex) -> List.of(
              new StreetBikeRentalLink((BikeRentalStationVertex) vertex, streetVertex),
              new StreetBikeRentalLink(streetVertex, (BikeRentalStationVertex) vertex)
          )
      );
    }
  }

  private void linkBikeParks(Graph graph, VertexLinker linker) {
    LOG.info("Linking bike parks to graph...");
    // It is enough to have the edges traversable by foot, as you can walk with the bike if necessary
    for (BikeParkVertex bikePark : graph.getVerticesOfType(BikeParkVertex.class)) {
      linker.linkVertexPermanently(
          bikePark,
          TraverseMode.WALK,
          LinkingDirection.BOTH_WAYS,
          (vertex, streetVertex) -> List.of(
              new StreetBikeParkLink((BikeParkVertex) vertex, streetVertex),
              new StreetBikeParkLink(streetVertex, (BikeParkVertex) vertex)
          )
      );
    }
  }

  @Override
  public void checkInputs() {
    //no inputs
  }
}

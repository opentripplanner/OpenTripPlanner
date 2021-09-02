package org.opentripplanner.graph_builder.module;

import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.linking.LinkingDirection;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.StreetBikeParkLink;
import org.opentripplanner.routing.edgetype.StreetBikeRentalLink;
import org.opentripplanner.routing.edgetype.StreetTransitStopLink;
import org.opentripplanner.routing.edgetype.StreetTransitEntranceLink;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.BikeParkVertex;
import org.opentripplanner.routing.vertextype.BikeRentalStationVertex;
import org.opentripplanner.routing.vertextype.TransitEntranceVertex;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.opentripplanner.util.OTPFeature;
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
    graph.getLinker().setAddExtraEdgesToAreas(this.addExtraEdgesToAreas);

    if (graph.hasStreets) {
      linkTransitStops(graph);
      linkTransitEntrances(graph);
      linkBikeRentals(graph);
      linkBikeParks(graph);
    }

    // Calculates convex hull of a graph which is shown in routerInfo API point
    graph.calculateConvexHull();
  }

  private void linkTransitStops(Graph graph) {
    LOG.info("Linking transit stops to graph...");
    for (TransitStopVertex tStop : graph.getVerticesOfType(TransitStopVertex.class)) {

      // Stops with pathways do not need to be connected to the street network, since there are explicit entraces defined for that
      if (tStop.hasPathways()) {
        continue;
      }

      TraverseModeSet modes = new TraverseModeSet(TraverseMode.WALK);

      if (OTPFeature.FlexRouting.isOn()) {
        // If regular stops are used for flex trips, they also need to be connected to car routable
        // street edges.
        if (graph.getAllFlexStopsFlat().contains(tStop.getStop())) {
          modes = new TraverseModeSet(TraverseMode.WALK, TraverseMode.CAR);
        }
      }

      graph.getLinker().linkVertexPermanently(
          tStop,
          modes,
          LinkingDirection.BOTH_WAYS,
          (vertex, streetVertex) -> List.of(
              new StreetTransitStopLink((TransitStopVertex) vertex, streetVertex),
              new StreetTransitStopLink(streetVertex, (TransitStopVertex) vertex)
          )
      );
    }
  }

  private void linkTransitEntrances(Graph graph) {
    LOG.info("Linking transit entrances to graph...");
    for (TransitEntranceVertex tEntrance : graph.getVerticesOfType(TransitEntranceVertex.class)) {
      graph.getLinker().linkVertexPermanently(
          tEntrance,
          new TraverseModeSet(TraverseMode.WALK),
          LinkingDirection.BOTH_WAYS,
          (vertex, streetVertex) -> List.of(
              new StreetTransitEntranceLink((TransitEntranceVertex) vertex, streetVertex),
              new StreetTransitEntranceLink(streetVertex, (TransitEntranceVertex) vertex)
          )
      );
    }
  }

  private void linkBikeRentals(Graph graph) {
    LOG.info("Linking bike rental stations to graph...");
      // It is enough to have the edges traversable by foot, as you can walk with the bike if necessary
    for (BikeRentalStationVertex bikeRental : graph.getVerticesOfType(BikeRentalStationVertex.class)) {
      graph.getLinker().linkVertexPermanently(
          bikeRental,
          new TraverseModeSet(TraverseMode.WALK),
          LinkingDirection.BOTH_WAYS,
          (vertex, streetVertex) -> List.of(
              new StreetBikeRentalLink((BikeRentalStationVertex) vertex, streetVertex),
              new StreetBikeRentalLink(streetVertex, (BikeRentalStationVertex) vertex)
          )
      );
    }
  }

  private void linkBikeParks(Graph graph) {
    LOG.info("Linking bike parks to graph...");
    // It is enough to have the edges traversable by foot, as you can walk with the bike if necessary
    for (BikeParkVertex bikePark : graph.getVerticesOfType(BikeParkVertex.class)) {
      graph.getLinker().linkVertexPermanently(
          bikePark,
          new TraverseModeSet(TraverseMode.WALK),
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

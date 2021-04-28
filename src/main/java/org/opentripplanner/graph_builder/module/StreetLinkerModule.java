package org.opentripplanner.graph_builder.module;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.linking.LinkingDirection;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.StreetTransitEntranceLink;
import org.opentripplanner.routing.edgetype.StreetTransitStopLink;
import org.opentripplanner.routing.edgetype.StreetVehicleParkingLink;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.TransitEntranceVertex;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.opentripplanner.routing.vertextype.VehicleParkingVertex;
import org.opentripplanner.util.OTPFeature;
import org.opentripplanner.util.ProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    return List.of("streets"); // don't include transit, because we also link P+Rs and bike rental stations,
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
      linkVehicleParks(graph);
    }

    // Calculates convex hull of a graph which is shown in routerInfo API point
    graph.calculateConvexHull();
  }


  public void linkTransitStops(Graph graph) {
    List<TransitStopVertex> vertices = graph.getVerticesOfType(TransitStopVertex.class);
    var progress = ProgressTracker.track("Linking transit stops to graph", 5000, vertices.size());
    LOG.info(progress.startMessage());

    for (TransitStopVertex tStop : vertices) {

      // Stops with pathways do not need to be connected to the street network, since there are explicit entraces defined for that
      if (tStop.hasPathways()) {
        continue;
      }
      // check if stop is already linked, to allow multiple linking cycles
      if (tStop.getDegreeOut() + tStop.getDegreeIn() > 0) {
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
      //noinspection Convert2MethodRef
      progress.step(m -> LOG.info(m));
    }
    LOG.info(progress.completeMessage());
  }

  public void linkTransitEntrances(Graph graph) {
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

  private void linkVehicleParks(Graph graph) {
    LOG.info("Linking vehicle parks to graph...");
    // It is enough to have the edges traversable by foot, as you can walk with the bike if necessary
    for (VehicleParkingVertex vehicleParkingVertex : graph.getVerticesOfType(VehicleParkingVertex.class)) {
      var traverseModes = vehicleParkingVertex.getVehicleParking().hasCarPlaces() ?
          new TraverseModeSet(TraverseMode.WALK, TraverseMode.CAR)
          : new TraverseModeSet(TraverseMode.WALK);

      graph.getLinker().linkVertexPermanently(
          vehicleParkingVertex,
          traverseModes,
          LinkingDirection.BOTH_WAYS,
          (vertex, streetVertex) -> List.of(
              new StreetVehicleParkingLink((VehicleParkingVertex) vertex, streetVertex),
              new StreetVehicleParkingLink(streetVertex, (VehicleParkingVertex) vertex)
          )
      );
    }
  }

  @Override
  public void checkInputs() {
    //no inputs
  }
}

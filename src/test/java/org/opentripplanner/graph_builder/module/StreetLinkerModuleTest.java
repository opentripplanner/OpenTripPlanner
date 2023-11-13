package org.opentripplanner.graph_builder.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner._support.geometry.Coordinates.KONGSBERG_PLATFORM_1;
import static org.opentripplanner.street.model.StreetTraversalPermission.CAR;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN;
import static org.opentripplanner.transit.model._data.TransitModelForTest.id;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.flex.trip.UnscheduledTrip;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model._data.StreetModelForTest;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetTransitStopLink;
import org.opentripplanner.street.model.vertex.SplitterVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertexBuilder;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.StopModel;
import org.opentripplanner.transit.service.TransitModel;

class StreetLinkerModuleTest {

  private static final double DELTA = 0.0001;

  @Test
  void linkingIsIdempotent() {
    var model = new TestModel();
    var module = model.streetLinkerModule();

    module.buildGraph();
    module.buildGraph();
    module.buildGraph();

    assertTrue(model.stopVertex().isConnectedToGraph());
    assertEquals(1, model.stopVertex().getOutgoing().size());
  }

  @Test
  void linkRegularStop() {
    var model = new TestModel();
    var module = model.streetLinkerModule();

    module.buildGraph();

    assertTrue(model.stopVertex().isConnectedToGraph());

    assertEquals(1, model.stopVertex().getOutgoing().size());
    var outgoing = model.outgoingLinks().getFirst();
    assertInstanceOf(StreetTransitStopLink.class, outgoing);

    SplitterVertex linkedTo = (SplitterVertex) outgoing.getToVertex();

    assertTrue(linkedTo.isConnectedToWalkingEdge());
    assertFalse(linkedTo.isConnectedToDriveableEdge());
  }

  @Test
  void linkFlexStop() {
    OTPFeature.FlexRouting.testOn(() -> {
      var model = new TestModel();
      var flexTrip = TransitModelForTest.of().unscheduledTrip(id("flex"), model.stop());
      model.withFlexTrip(flexTrip);

      var module = model.streetLinkerModule();

      module.buildGraph();

      assertTrue(model.stopVertex().isConnectedToGraph());

      // stop is used by a flex trip, needs to be linked to both the walk and car edge
      assertEquals(2, model.stopVertex().getOutgoing().size());
      var linkToWalk = model.outgoingLinks().getFirst();
      SplitterVertex walkSplit = (SplitterVertex) linkToWalk.getToVertex();

      assertTrue(walkSplit.isConnectedToWalkingEdge());
      assertFalse(walkSplit.isConnectedToDriveableEdge());

      var linkToCar = model.outgoingLinks().getLast();
      SplitterVertex carSplit = (SplitterVertex) linkToCar.getToVertex();

      assertFalse(carSplit.isConnectedToWalkingEdge());
      assertTrue(carSplit.isConnectedToDriveableEdge());
    });
  }

  private static class TestModel {

    private final TransitStopVertex stopVertex;
    private final StreetLinkerModule module;
    private final RegularStop stop;
    private final TransitModel transitModel;

    public TestModel() {
      var from = StreetModelForTest.intersectionVertex(
        KONGSBERG_PLATFORM_1.x - DELTA,
        KONGSBERG_PLATFORM_1.y - DELTA
      );
      var to = StreetModelForTest.intersectionVertex(
        KONGSBERG_PLATFORM_1.x + DELTA,
        KONGSBERG_PLATFORM_1.y + DELTA
      );

      Graph graph = new Graph();
      graph.addVertex(from);
      graph.addVertex(to);

      var walkableEdge = StreetModelForTest.streetEdge(from, to, PEDESTRIAN);
      var drivableEdge = StreetModelForTest.streetEdge(from, to, CAR);
      var builder = StopModel.of();
      stop =
        builder
          .regularStop(id("platform-1"))
          .withCoordinate(new WgsCoordinate(KONGSBERG_PLATFORM_1))
          .build();
      builder.withRegularStop(stop);

      transitModel = new TransitModel(builder.build(), new Deduplicator());

      stopVertex = new TransitStopVertexBuilder().withStop(stop).build();
      graph.addVertex(stopVertex);
      graph.hasStreets = true;

      module = new StreetLinkerModule(graph, transitModel, DataImportIssueStore.NOOP, false);

      assertFalse(stopVertex.isConnectedToGraph());
      assertTrue(stopVertex.getIncoming().isEmpty());
      assertTrue(stopVertex.getOutgoing().isEmpty());
    }

    public TransitStopVertex stopVertex() {
      return stopVertex;
    }

    public StreetLinkerModule streetLinkerModule() {
      return module;
    }

    public List<Edge> outgoingLinks() {
      return List.copyOf(stopVertex.getOutgoing());
    }

    public RegularStop stop() {
      return stop;
    }

    public void withFlexTrip(UnscheduledTrip flexTrip) {
      transitModel.addFlexTrip(flexTrip.getId(), flexTrip);
    }
  }
}

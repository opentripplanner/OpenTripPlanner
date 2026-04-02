package org.opentripplanner.graph_builder.module.stopconnectivity;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.street.model.StreetModelForTest.intersectionVertex;
import static org.opentripplanner.transit.model._data.FeedScopedIdForTestFactory.id;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;
import org.opentripplanner.graph_builder.issues.IsolatedStop;
import org.opentripplanner.routing.algorithm.GraphRoutingTest;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.StreetModelForTest;
import org.opentripplanner.street.model.edge.StreetTransitStopLink;
import org.opentripplanner.street.model.vertex.TransitStopVertex;

class StopConnectivityModuleTest extends GraphRoutingTest {

  @Test
  void stopConnectedToTinyIsland() {
    var issueStore = new DefaultDataImportIssueStore();

    var stop = TransitStopVertex.of().withId(id(1)).withCoordinate(60, 10).build();
    var i1 = intersectionVertex("v2", 60.0, 10.001);
    var i2 = intersectionVertex("v3", 60.0, 10.002);
    var i3 = intersectionVertex("v4", 60.0, 10.003);

    StreetTransitStopLink.createStreetTransitStopLink(stop, i1);
    StreetTransitStopLink.createStreetTransitStopLink(stop, i2);

    StreetModelForTest.streetEdge(i1, i3);

    var g = new Graph();
    g.addVertex(stop);
    g.addVertex(i1);
    g.addVertex(i2);
    g.addVertex(i3);

    var module = new StopConnectivityModule(g, issueStore);
    module.buildGraph();

    assertThat(issueStore.listIssues()).hasSize(1);
    IsolatedStop issue = (IsolatedStop) issueStore.listIssues().getFirst();
    assertThat(issue.maxWalk()).isAtLeast(Duration.ofMinutes(1));
  }
}

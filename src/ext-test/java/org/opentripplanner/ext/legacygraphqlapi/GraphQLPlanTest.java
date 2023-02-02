package org.opentripplanner.ext.legacygraphqlapi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.server.TestServerRequestContext;
import org.opentripplanner.transit.service.TransitModel;

class GraphQLPlanTest implements PlanTestConstants {

  static final TestServerRequestContext context;
  static final Graph graph = new Graph();
  static final LegacyGraphQLAPI resource;

  static {
    var transitModel = new TransitModel();
    transitModel.initTimeZone(ZoneIds.BERLIN);
    transitModel.index();

    context = new TestServerRequestContext(graph, transitModel);
    resource = new LegacyGraphQLAPI(context, "ignored");
  }

  @Test
  void preferredVehicleParkingTags() {
    var query =
      """
      query {
        plan(
          parking: {
            bannedTags: ["wheelbender"],
            requiredTags: ["covered"],
            preferredTags: ["roof", "locker", "cellar"],
            unpreferredTagCost: 555
          }
        ) {
          itineraries {
            duration
          }
        }
      }
      
      """;
    var response = resource.getGraphQL(query, 2000, 10000, new TestHeaders());
    assertEquals(200, response.getStatus());

    var routeRequest = context.lastRouteRequest();
    var parking = routeRequest.journey().parking();
    assertEquals(Set.of("cellar", "roof", "locker"), parking.preferredTags());
    assertEquals(555, parking.unpreferredTagCost());
    assertEquals(Set.of("wheelbender"), parking.bannedTags());
    assertEquals(Set.of("covered"), parking.requiredTags());
  }
}

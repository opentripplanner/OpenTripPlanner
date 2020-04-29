package org.opentripplanner.ext.examples.statistics.api.resource;

import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.graph_builder.module.AddTransitModelEntitiesToGraph;
import org.opentripplanner.graph_builder.module.geometry.GeometryAndBlockProcessor;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.graph.Graph;

import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.gtfs.GtfsContextBuilder.contextBuilder;

public class GraphStatisticsResourceTest {
    private static final String QUERY_STATISTICS = "query S {\n  graphStatistics {\n    stops\n  }\n}\n";

    private GraphStatisticsResource subject;
    private String expResult;

    @Before public void setUp() throws Exception {
        GtfsContext context = contextBuilder(ConstantsForTests.FAKE_GTFS).build();
        Graph graph = new Graph();
        AddTransitModelEntitiesToGraph.addToGraph(context, graph);
        new GeometryAndBlockProcessor(context).run(graph);
        graph.putService(
                CalendarServiceData.class, context.getCalendarServiceData()
        );
        graph.index();

        long expStops = graph.index.getAllStops().size();
        expResult = "{data={graphStatistics={stops=" + expStops + "}}}";

        subject = new GraphStatisticsResource(new RoutingService(graph));
    }

    @Test public void getGraphQLAsJson() {
        Map<String, Object> request = new HashMap<>();
        request.put("query", QUERY_STATISTICS);
        request.put("operationName", "S");

        Response res = subject.getGraphQLAsJson(request);

        assertEquals(res.toString(), res.getStatus(), 200);
        assertEquals(expResult, res.getEntity().toString());
    }

    @Test public void getGraphQL() {
        Response res = subject.getGraphQL(QUERY_STATISTICS);

        assertEquals(res.toString(), res.getStatus(), 200);
        assertEquals(expResult, res.getEntity().toString());
    }
}
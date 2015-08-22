package org.opentripplanner.api.resource;

import org.junit.Test;
import org.opentripplanner.api.model.RouterInfo;
import org.opentripplanner.api.model.RouterList;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.MemoryGraphSource;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.vertextype.ExitVertex;
import org.opentripplanner.standalone.CommandLineParameters;
import org.opentripplanner.standalone.OTPServer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RoutersTest {
    @Test
    public void testRouters() {
        OTPServer otpServer = new OTPServer(new CommandLineParameters(), new GraphService());
        otpServer.getGraphService().registerGraph("", new MemoryGraphSource(null, new Graph()));
        otpServer.getGraphService().registerGraph("A", new MemoryGraphSource("", new Graph()));
        otpServer.getGraphService().getRouter("A").graph.addVertex(new ExitVertex(null, "A", 0, 0, 0));
        otpServer.getGraphService().getRouter("A").graph.addVertex(new ExitVertex(null, "B", 0, 1, 0));
        otpServer.getGraphService().getRouter("A").graph.addVertex(new ExitVertex(null, "C", 1, 1, 0));
        //this needs to be added since convex hull isn't lazy loaded anymore
        otpServer.getGraphService().getRouter("A").graph.calculateConvexHull();
        otpServer.getGraphService().getRouter("").graph.calculateConvexHull();
        //this needs to be added since it is otherwise calculated during OSM/Transit loading
        // which doesn't happen in this test
        otpServer.getGraphService().getRouter("A").graph.calculateEnvelope();
        otpServer.getGraphService().getRouter("").graph.calculateEnvelope();

        Routers routerApi = new Routers();
        routerApi.otpServer = otpServer;
        RouterList routers = routerApi.getRouterIds();
        assertEquals(2, routers.routerInfo.size());
        RouterInfo router0 = routers.routerInfo.get(0);
        RouterInfo router1 = routers.routerInfo.get(1);
        RouterInfo otherRouter;
        RouterInfo defaultRouter;
        if (router0.routerId.equals("")) {
            defaultRouter = router0;
            otherRouter = router1;
        } else {
            defaultRouter = router1;
            otherRouter = router0;
        }
        assertEquals("", defaultRouter.routerId);
        assertEquals("A", otherRouter.routerId);
        assertTrue(otherRouter.polygon.getArea() > 0);
    }
}

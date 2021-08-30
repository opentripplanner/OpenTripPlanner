package org.opentripplanner.ext.interactivelauncher.client;

import java.time.Duration;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.standalone.server.Router;

public class TestClient implements Runnable {
    private final Router router;
    private final RoutingService routingService;

    public TestClient(Router router) {
        this.router = router;
        this.routingService = new RoutingService(router.graph);
    }

    @Override
    public void run() {
        if (sleepOnStartup()) { return; }
        System.out.println("\nTestClient.run ....ooooooo000000000000000!\n");


        var graph = router.graph;




        var req = router.defaultRoutingRequest.clone();

        req.from = new GenericLocation("", new FeedScopedId("EN", "NSR:StopPlace:58947"), null, null);
        req.to = new GenericLocation("", new FeedScopedId("EN", "NSR:StopPlace:58952"), null, null);
        req.dateTime = 1630825200;
        req.searchWindow = Duration.ofMinutes(121);


        var result = routingService.route(req, router);

        System.out.println("RES: " + result);
    }

    private boolean sleepOnStartup() {
        try {
            Thread.sleep(3000);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
            return true;
        }
        return false;
    }
}

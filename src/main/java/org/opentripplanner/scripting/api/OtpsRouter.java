package org.opentripplanner.scripting.api;

import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.error.VertexNotFoundException;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.standalone.Router;

/**
 * A router, as returned by the getRouter function of the OTP script entry point.
 * 
 * Example of code (python script):
 * <pre>
 *   # Get the default router
 *   defRouter = otp.getRouter()
 *   # Get the router of ID 'paris'
 *   parisRouter = otp.getRouter('paris')
 * </pre>
 * 
 * @author laurent
 */
public class OtpsRouter {

    private Router router;

    public OtpsRouter(Router router) {
        this.router = router;
    }

    /**
     * Plan a route on the router given the various options.
     * 
     * @param req The routing request options (date/time, modes, etc...)
     * @return A Shortest-path-tree (a time+various states for each vertices around the
     *         origin/destination).
     */
    public OtpsSPT plan(OtpsRoutingRequest req) {
        try {
            // TODO Is this correct?
            RoutingRequest req2 = req.req.clone();
            req2.setRoutingContext(router.graph);
            // TODO verify that this is indeed the intended behavior.
            ShortestPathTree spt = new AStar().getShortestPathTree(req2);
            return new OtpsSPT(spt, router.graph.getSampleFactory());
        } catch (VertexNotFoundException e) {
            // Can happen, not really an error
            return null;
        }
    }
}

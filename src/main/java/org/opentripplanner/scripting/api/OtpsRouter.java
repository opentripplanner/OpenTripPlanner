/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

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

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

package org.opentripplanner.api.ws.analyst;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.opentripplanner.analyst.core.TimeGrid;
import org.opentripplanner.analyst.request.SampleGridRenderer;
import org.opentripplanner.analyst.request.SampleGridRenderer.WTWD;
import org.opentripplanner.analyst.request.SampleGridRequest;
import org.opentripplanner.api.common.RoutingResource;
import org.opentripplanner.common.geometry.ZSampleGrid;
import org.opentripplanner.common.geometry.ZSampleGrid.TimeGridMapper;
import org.opentripplanner.routing.core.RoutingRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.core.InjectParam;

/**
 * Return a grid with time for a set of points.
 * 
 * Example of request:
 * 
 * <code>
 * http://localhost:8080/otp-rest-servlet/ws/timegrid?routerId=bordeaux&fromPlace=47.059,-0.880&date=2013/10/01&time=12:00:00&maxWalkDistance=1000&mode=WALK,TRANSIT
 * </code>
 * 
 * @author laurent
 */
@Path("/timegrid")
public class TimeGridWs extends RoutingResource {

    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(TimeGridWs.class);

    @InjectParam
    private SampleGridRenderer sampleGridRenderer;

    @QueryParam("maxTimeSec")
    private Integer maxTimeSec = null;

    @QueryParam("precisionMeters")
    private Integer precisionMeters = 200;

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getGeoJsonIsochrone() throws Exception {

        if (precisionMeters < 10)
            throw new IllegalArgumentException("Too small precisionMeters: " + precisionMeters);

        RoutingRequest sptRequest = buildRequest(0);
        SampleGridRequest tgRequest = new SampleGridRequest();
        tgRequest.setMaxTimeSec(maxTimeSec);
        tgRequest.setPrecisionMeters(precisionMeters);

        // Get a sample grid
        ZSampleGrid<WTWD> sampleGrid = sampleGridRenderer.getSampleGrid(tgRequest, sptRequest);

        TimeGrid timeGrid = sampleGrid.asTimeGrid(new TimeGridMapper<WTWD>() {
            @Override
            public int[] mapValues(WTWD z) {
                double t = z.tw / z.w;
                int it;
                if (Double.isInfinite(t))
                    it = -1;
                else
                    it = (int) Math.round(t);
                return new int[] { it, (int) Math.round(z.d) };
            }
        }, sampleGridRenderer.getOffRoadDistanceMeters(precisionMeters));

        return Response.ok().entity(timeGrid).build();

    }
}
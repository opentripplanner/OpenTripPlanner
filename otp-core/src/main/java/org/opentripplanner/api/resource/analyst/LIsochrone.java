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

package org.opentripplanner.api.resource.analyst;

import java.io.StringWriter;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.opentripplanner.analyst.core.IsochroneData;
import org.opentripplanner.analyst.request.IsoChroneRequest;
import org.opentripplanner.analyst.request.IsoChroneSPTRendererAccSampling;
import org.opentripplanner.analyst.request.IsoChroneSPTRendererRecursiveGrid;
import org.opentripplanner.api.common.RoutingResource;
import org.opentripplanner.api.model.error.PlannerError;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.util.GeoJSONBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.core.InjectParam;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Return isochrone geometry as a set of GeoJSON polygons.
 * 
 * Example of request:
 * http://localhost:8080/otp-rest-servlet/ws/isochrone?routerId=bordeaux&algorithm
 * =accSampling&fromPlace=47.059,-0.880&date=2013/10/01
 * &time=12:00:00&maxWalkDistance=1000&mode=WALK,TRANSIT&cutoffSec=1800&cutoffSec=3600
 * 
 * @author laurent
 */
@Path("/routers/{routerId}/isochrone")
public class LIsochrone extends RoutingResource {

    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(LIsochrone.class);

    @InjectParam
    private IsoChroneSPTRendererAccSampling accSamplingRenderer;

    @InjectParam
    private IsoChroneSPTRendererRecursiveGrid recursiveGridRenderer;

    @QueryParam("cutoffSec")
    private List<Integer> cutoffSecList;

    @QueryParam("maxTimeSec")
    private Integer maxTimeSec;

    @QueryParam("debug")
    private Boolean debug;

    @QueryParam("algorithm")
    private String algorithm;

    @QueryParam("precisionMeters")
    private Integer precisionMeters;

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getIsochrone() {

        try {
            if (debug == null)
                debug = false;
            if (precisionMeters == null)
                precisionMeters = 200;
            if (precisionMeters < 10)
                throw new IllegalArgumentException("Too small precisionMeters: " + precisionMeters);

            IsoChroneRequest isoChroneRequest = new IsoChroneRequest(cutoffSecList);
            isoChroneRequest.setIncludeDebugGeometry(debug);
            isoChroneRequest.setPrecisionMeters(precisionMeters);
            RoutingRequest sptRequest = buildRequest(0);

            if (maxTimeSec != null) {
                isoChroneRequest.setMaxTimeSec(maxTimeSec);
            } else {
                isoChroneRequest.setMaxTimeSec(isoChroneRequest.getMaxCutoffSec());
            }

            List<IsochroneData> isochrones;
            if (algorithm == null || "accSampling".equals(algorithm)) {
                isochrones = accSamplingRenderer.getIsochrones(isoChroneRequest, sptRequest);
            } else if ("recursiveGrid".equals(algorithm)) {
                isochrones = recursiveGridRenderer.getIsochrones(isoChroneRequest, sptRequest);
            } else {
                throw new IllegalArgumentException("Unknown algorithm: " + algorithm);
            }
            StringWriter geoJsonWriter = new StringWriter();
            GeoJSONBuilder geoJsonBuilder = new GeoJSONBuilder(geoJsonWriter);
            geoJsonBuilder.array();
            for (IsochroneData isochrone : isochrones) {
                geoJsonBuilder.object();
                geoJsonBuilder.key("type").value("Feature");
                geoJsonBuilder.key("properties").object();
                geoJsonBuilder.key("name").value("Isochrone " + isochrone.getCutoffSec() + " sec");
                geoJsonBuilder.endObject();
                geoJsonBuilder.key("geometry");
                geoJsonBuilder.writeGeom(isochrone.getGeometry());
                Geometry debugGeometry = isochrone.getDebugGeometry();
                if (debug && debugGeometry != null) {
                    geoJsonBuilder.key("debugGeometry");
                    geoJsonBuilder.writeGeom(debugGeometry);
                }
                geoJsonBuilder.endObject();
            }
            geoJsonBuilder.endArray();

            CacheControl cc = new CacheControl();
            cc.setMaxAge(3600);
            cc.setNoCache(false);
            return Response.ok(geoJsonWriter.toString()).cacheControl(cc).build();
        
        } catch (Exception e) {
            return Response.ok(new PlannerError(e)).build();
        }
    }

}
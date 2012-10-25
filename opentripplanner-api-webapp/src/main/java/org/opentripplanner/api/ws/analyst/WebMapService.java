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
 
import java.io.InputStream;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opentripplanner.analyst.request.RenderRequest;
import org.opentripplanner.analyst.request.Renderer;
import org.opentripplanner.analyst.request.TileRequest;
import org.opentripplanner.api.common.RoutingResource;
import org.opentripplanner.analyst.parameter.Layer;
import org.opentripplanner.analyst.parameter.LayerList;
import org.opentripplanner.analyst.parameter.MIMEImageFormat;
import org.opentripplanner.analyst.parameter.Style;
import org.opentripplanner.analyst.parameter.StyleList;
import org.opentripplanner.analyst.parameter.WMSVersion;
import org.opentripplanner.routing.core.RoutingRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sun.jersey.api.core.InjectParam;
import com.sun.jersey.api.spring.Autowire;

@Path("wms")
@Autowire
public class WebMapService extends RoutingResource {
    
    private static final Logger LOG = LoggerFactory.getLogger(WebMapService.class);

    @InjectParam
    private Renderer renderer;
    
    // use of string array in annotation dodges bug in Enunciate, which will be fixed in v1.26
    @GET @Produces( {"image/*", "text/*"} )
    public Response wmsGet(
           // Mandatory parameters
           @QueryParam("version") @DefaultValue("1.1.1")         WMSVersion version,
           @QueryParam("request") @DefaultValue("GetMap")        String request,
           @QueryParam("layers")  @DefaultValue("traveltime")    LayerList layers, 
           @QueryParam("styles")  @DefaultValue("gray")          StyleList styles,
           // SRS is called CRS in 1.3.0
           @QueryParam("srs")     @DefaultValue("EPSG:4326")     CoordinateReferenceSystem srs,
           @QueryParam("crs")     CoordinateReferenceSystem crs,
           @QueryParam("bbox")    Envelope2D bbox, 
           @QueryParam("width")   @DefaultValue("256")           int width, 
           @QueryParam("height")  @DefaultValue("256")           int height, 
           @QueryParam("format")  @DefaultValue("image/png")     MIMEImageFormat format,
           // Optional parameters
           @QueryParam("transparent") @DefaultValue("false")     Boolean transparent,
           @QueryParam("bgcolor")     @DefaultValue("0xFFFFFF")  String bgcolor,
           @QueryParam("exceptions")  @DefaultValue("XML")       String exceptions,
           // @QueryParam("time") GregorianCalendar time, // SearchResource will parse time without date 
           // non-WMS parameters
           @QueryParam("resolution")     Double resolution,
           @QueryParam("reproject")   @DefaultValue("true")       Boolean reproject,
           @QueryParam("timestamp")   @DefaultValue("false")      Boolean timestamp,
           @Context UriInfo uriInfo ) throws Exception { 
        
        if (request.equals("getCapabilities")) 
            return getCapabilitiesResponse();
            
        if (version == new WMSVersion("1.3.0") && crs != null)
            srs = crs;

        //bbox.setCoordinateReferenceSystem(srs);
        if (reproject) {
            LOG.info("reprojecting envelope from WGS84 to {}", srs);
            ReferencedEnvelope renv = new ReferencedEnvelope(bbox, DefaultGeographicCRS.WGS84);
            LOG.debug("WGS84 = {}", renv);
            bbox = new Envelope2D(renv.transform(srs, false));
            LOG.debug("reprojected envelope is {}", bbox);
        }

        if (resolution != null) {
            width  = (int) Math.ceil(bbox.width  / resolution);
            height = (int) Math.ceil(bbox.height / resolution);
            LOG.debug("resolution (pixel size) set to {} map units", resolution);
            LOG.debug("resulting raster dimensions are {}w x {}h", width, height);
        }

        RoutingRequest reqA = this.buildRequest(0);
        RoutingRequest reqB = this.buildRequest(1);
        
        LOG.debug("params {}", uriInfo.getQueryParameters());
        LOG.debug("layers = {}", layers);
        LOG.debug("styles = {}", styles);
        LOG.debug("version = {}", version);
        LOG.debug("srs = {}", srs.getName());
        LOG.debug("bbox = {}", bbox);
        LOG.debug("search time = {}", reqA.getDateTime());
        
//        SPTRequest sptRequestA, sptRequestB = null;
//        if (originLat == null && originLon == null) {
//            LOG.warn("no origin (sample dimension) specified.");
//            return Response.noContent().build();
//        }
//        sptRequestA = new SPTRequest(originLon, originLat, time);
//
//        if (originLatB != null && originLonB != null) {
//            sptRequestB = new SPTRequest(originLonB, originLatB, timeB);
//        } 
//        
        TileRequest tileRequest = new TileRequest(bbox, width, height);
        Layer layer = layers.get(0);
        Style style = styles.get(0);
        RenderRequest renderRequest = new RenderRequest(format, layer, style, transparent, timestamp);
        
        if (layer != Layer.DIFFERENCE) {
//            noPurple = req.clone();
//            noPurple.setBannedRoutes("Test_Purple");
            reqB = null;
        }
        
        
        return renderer.getResponse(tileRequest, reqA, reqB, renderRequest);
    }

    /** Yes, this is loading a static capabilities response from a file 
     * on the classpath. */
    private Response getCapabilitiesResponse() throws Exception {
        InputStream xml = getClass().getResourceAsStream("wms-capabilities.xml");
        return Response.ok().entity(xml).type("text/xml").build();
    }
    
}
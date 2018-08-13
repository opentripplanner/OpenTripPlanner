package org.opentripplanner.api.resource;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.geotools.geometry.Envelope2D;
import org.opentripplanner.analyst.core.SlippyTile;
import org.opentripplanner.analyst.request.RenderRequest;
import org.opentripplanner.analyst.request.TileRequest;
import org.opentripplanner.api.common.RoutingResource;
import org.opentripplanner.api.parameter.Layer;
import org.opentripplanner.api.parameter.LayerList;
import org.opentripplanner.api.parameter.MIMEImageFormat;
import org.opentripplanner.api.parameter.Style;
import org.opentripplanner.api.parameter.StyleList;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.standalone.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NOTE This way of fetching travel time tiles does not currently work.
 * It relied on an SPT cache which would find existing SPTs based on search request parameters.
 * Server side stored search results are now done explicitly with TimeSurfaces.
 * See org.opentripplanner.api.resource.SurfaceResource, functions createSurface and tileGet
 * The basic idea is to create a surface for a "batch" or one-to-many OTP search, then using the returned ID
 * fetch the tiles from a URL under that surface ID.
 */
@Path("/routers/{routerId}/analyst/tile/{z}/{x}/{y}.png")
public class TileService extends RoutingResource {
    
    private static final Logger LOG = LoggerFactory.getLogger(TileService.class);

    @PathParam("x") int x; 
    @PathParam("y") int y;
    @PathParam("z") int z;
    
    @QueryParam("layers")  @DefaultValue("traveltime") LayerList layers; 
    @QueryParam("styles")  @DefaultValue("mask")       StyleList styles;
    @QueryParam("format")  @DefaultValue("image/png")  MIMEImageFormat format;

    @GET @Produces("image/*")
    public Response tileGet() throws Exception { 
        
        Envelope2D env = SlippyTile.tile2Envelope(x, y, z);
        TileRequest tileRequest = new TileRequest(env, 256, 256);
        RoutingRequest sptRequestA = buildRequest();

        Layer layer = layers.get(0);
        Style style = styles.get(0);
        RenderRequest renderRequest = new RenderRequest(format, layer, style, true, false);
        Router router = otpServer.getRouter(routerId);

        // The method below that takes sptRequests directly is deprecated, see javadoc above on this class and
        // org.opentripplanner.api.resource.SurfaceResource, functions createSurface and tileGet
        return null; // router.renderer.getResponse(tileRequest, sptRequestA, sptRequestB, renderRequest);
    }

}
package org.opentripplanner.api.resource;

import com.google.common.collect.Maps;
import org.geotools.feature.FeatureCollection;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geometry.Envelope2D;
import org.opentripplanner.analyst.PointSet;
import org.opentripplanner.analyst.ResultSet;
import org.opentripplanner.analyst.SampleSet;
import org.opentripplanner.analyst.TimeSurface;
import org.opentripplanner.analyst.core.IsochroneData;
import org.opentripplanner.analyst.core.SlippyTile;
import org.opentripplanner.analyst.request.RenderRequest;
import org.opentripplanner.analyst.request.SampleGridRenderer.WTWD;
import org.opentripplanner.analyst.request.TileRequest;
import org.opentripplanner.api.common.ParameterException;
import org.opentripplanner.api.common.RoutingResource;
import org.opentripplanner.api.model.TimeSurfaceShort;
import org.opentripplanner.api.parameter.CRSParameter;
import org.opentripplanner.api.parameter.IsoTimeParameter;
import org.opentripplanner.api.parameter.Layer;
import org.opentripplanner.api.parameter.MIMEImageFormat;
import org.opentripplanner.api.parameter.Style;
import org.opentripplanner.common.geometry.DelaunayIsolineBuilder;
import org.opentripplanner.routing.algorithm.EarliestArrivalSearch;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.standalone.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Surfaces cannot be isolated per-router because sometimes you want to compare two surfaces from different router IDs.
 * Though one could question whether that really makes sense (perhaps alternative scenarios should be "within" the same router)
 */
@Path("/surfaces")
@Produces({ MediaType.APPLICATION_JSON })
public class SurfaceResource extends RoutingResource {

    private static final Logger LOG = LoggerFactory.getLogger(TimeSurface.class);

    @Context
    UriInfo uriInfo;

    @POST
    public Response createSurface(@QueryParam("cutoffMinutes") 
    @DefaultValue("90") int cutoffMinutes,
    @QueryParam("routerId") String routerId) {

        // Build the request
        try {
            RoutingRequest req = buildRequest(); // batch must be true
           
            // routerId is optional -- select default graph if not set
            Router router = otpServer.getRouter(routerId);
            req.setRoutingContext(router.graph);
        	
            EarliestArrivalSearch sptService = new EarliestArrivalSearch();
            sptService.maxDuration = (60 * cutoffMinutes);
            ShortestPathTree spt = sptService.getShortestPathTree(req);
            req.cleanup();
            if (spt != null) {
                TimeSurface surface = new TimeSurface(spt);
                surface.params = Maps.newHashMap();
                for (Map.Entry<String, List<String>> e : uriInfo.getQueryParameters().entrySet()) {
                    // include only the first instance of each query parameter
                    surface.params.put(e.getKey(), e.getValue().get(0));
                }
                surface.cutoffMinutes = cutoffMinutes;
                otpServer.surfaceCache.add(surface);
                return Response.ok().entity(new TimeSurfaceShort(surface)).build(); // .created(URI)
            } else {
                return Response.noContent().entity("NO SPT").build();
            }
        } catch (ParameterException pex) {
            return Response.status(Response.Status.BAD_REQUEST).entity("BAD USER").build();
        }

    }

    /** List all the available surfaces. */
    @GET
    public Response getTimeSurfaceList () {
        return Response.ok().entity(TimeSurfaceShort.list(otpServer.surfaceCache.cache.asMap().values())).build();
    }

    /** Describe a specific surface. */
    @GET @Path("/{surfaceId}")
    public Response getTimeSurfaceList (@PathParam("surfaceId") Integer surfaceId) {
        TimeSurface surface = otpServer.surfaceCache.get(surfaceId);
        if (surface == null) return Response.status(Response.Status.NOT_FOUND).entity("Invalid surface ID.").build();
        return Response.ok().entity(new TimeSurfaceShort(surface)).build();
        // DEBUG return Response.ok().entity(surface).build();
    }

    /** Evaluate a surface at all the points in a PointSet. */
    @GET @Path("/{surfaceId}/indicator")
    public Response getIndicator (@PathParam("surfaceId") Integer surfaceId,
                                  @QueryParam("targets")  String  targetPointSetId,
                                  @QueryParam("origins")  String  originPointSetId,
                                  @QueryParam("detail")   boolean detail) {

        final TimeSurface surf = otpServer.surfaceCache.get(surfaceId);
        if (surf == null) return badRequest("Invalid TimeSurface ID.");
        final PointSet pset = otpServer.pointSetCache.get(targetPointSetId);
        if (pset == null) return badRequest("Missing or invalid target PointSet ID.");

        Router router = otpServer.getRouter(surf.routerId);
        // TODO cache this sampleset
        SampleSet samples = pset.getSampleSet(router.graph);
        final ResultSet indicator = new ResultSet(samples, surf, detail, detail);
        if (indicator == null) return badServer("Could not compute indicator as requested.");

        return Response.ok().entity(new StreamingOutput() {
            @Override
            public void write(OutputStream output) throws IOException, WebApplicationException {
                indicator.writeJson(output);
            }
        }).build();

    }

    /** Create vector isochrones for a surface. */
    @GET @Path("/{surfaceId}/isochrone")
    public Response getIsochrone (
            @PathParam("surfaceId") Integer surfaceId,
            @QueryParam("spacing") int spacing,
            @QueryParam("nMax") @DefaultValue("1") int nMax) {
        final TimeSurface surf = otpServer.surfaceCache.get(surfaceId);
        if (surf == null) return badRequest("Invalid TimeSurface ID.");
        if (spacing < 1) spacing = 30;
        List<IsochroneData> isochrones = getIsochronesAccumulative(surf, spacing, nMax);
        // NOTE that cutoffMinutes in the surface must be properly set for the following call to work
        final FeatureCollection fc = LIsochrone.makeContourFeatures(isochrones);
        return Response.ok().entity(new StreamingOutput() {
            @Override
            public void write(OutputStream output) throws IOException {
                FeatureJSON fj = new FeatureJSON();
                fj.writeFeatureCollection(fc, output);
            }
        }).build();
    }

    @Path("/{surfaceId}/isotiles/{z}/{x}/{y}.png")
    @GET @Produces("image/png")
    public Response tileGet(@PathParam("surfaceId") Integer surfaceId,
                            @PathParam("x") int x,
                            @PathParam("y") int y,
                            @PathParam("z") int z) throws Exception {

        Envelope2D env = SlippyTile.tile2Envelope(x, y, z);
        TimeSurface surfA = otpServer.surfaceCache.get(surfaceId);
        if (surfA == null) return badRequest("Unrecognized surface ID.");
        	
        TileRequest tileRequest = new TileRequest(env, 256, 256);
       
        MIMEImageFormat imageFormat = new MIMEImageFormat("image/png");
        RenderRequest renderRequest =
                new RenderRequest(imageFormat, Layer.TRAVELTIME, Style.COLOR30, true, false);
        // TODO why can't the renderer be static?
        Router router = otpServer.getRouter(surfA.routerId);
        return router.renderer.getResponse(tileRequest, surfA, null, renderRequest);
    }
    /**
     * Renders a raster tile for showing the difference between two TimeSurfaces.
     * This service is included as a way to provide difference tiles using existing mechanisms in OTP.
     * TODO However, there is some room for debate around how differences are expressed in URLs.
     * We may want a more general purpose mechanism for combining time surfaces.
     * For example you could make a web service request to create a time surface A-B or A+B, and the server would give
     * you an ID for that surface, and then you could use that ID anywhere a surface ID is required. Perhaps internally
     * there would be some sort of DifferenceTimeSurface subclass that could just drop in anywhere TimeSurface is used.
     * This approach would be more stateful but more flexible.
     *
     * @author hannesj
     * 
     * @param surfaceId The id of the first surface
     * @param compareToSurfaceId The id of of the surface, which is compared to the first surface
    */
    @Path("/{surfaceId}/differencetiles/{compareToSurfaceId}/{z}/{x}/{y}.png")
    @GET @Produces("image/png")
    public Response differenceTileGet(@PathParam("surfaceId") Integer surfaceId,
                            @PathParam("compareToSurfaceId") Integer compareToSurfaceId,
                            @PathParam("x") int x,
                            @PathParam("y") int y,
                            @PathParam("z") int z) throws Exception {

        Envelope2D env = SlippyTile.tile2Envelope(x, y, z);
        TimeSurface surfA = otpServer.surfaceCache.get(surfaceId);
        if (surfA == null) return badRequest("Unrecognized surface ID.");

        TimeSurface surfB = otpServer.surfaceCache.get(compareToSurfaceId);
        if (surfB == null) return badRequest("Unrecognized surface ID.");

        if ( ! surfA.routerId.equals(surfB.routerId)) {
            return badRequest("Both surfaces must be from the same router to perform subtraction.");
        }

        TileRequest tileRequest = new TileRequest(env, 256, 256);
        MIMEImageFormat imageFormat = new MIMEImageFormat("image/png");
        RenderRequest renderRequest = new RenderRequest(imageFormat, Layer.DIFFERENCE, Style.DIFFERENCE, true, false);
        // TODO why can't the renderer be static?
        Router router = otpServer.getRouter(surfA.routerId);
        return router.renderer.getResponse(tileRequest, surfA, surfB, renderRequest);
    }

    private Response badRequest(String message) {
        return Response.status(Response.Status.BAD_REQUEST).entity("Bad request: " + message).build();
    }

    private Response badServer(String message) {
        return Response.status(Response.Status.BAD_REQUEST).entity("Server fail: " + message).build();
    }

    /**
     * Use Laurent's accumulative grid sampler. Cutoffs in minutes.
     * The grid and Delaunay triangulation are cached, so subsequent requests are very fast.
     *
     * @param spacing the number of minutes between isochrones
     * @return a list of evenly-spaced isochrones up to the timesurface's cutoff point
     */
    public static List<IsochroneData> getIsochronesAccumulative(TimeSurface surf, int spacing, int nMax) {

        long t0 = System.currentTimeMillis();
        if (surf.sampleGrid == null) {
            // The sample grid was not built from the SPT; make a minimal one including only time from the vertices in this timesurface
            surf.makeSampleGridWithoutSPT();
        }
        DelaunayIsolineBuilder<WTWD> isolineBuilder = new DelaunayIsolineBuilder<WTWD>(
                surf.sampleGrid.delaunayTriangulate(), new WTWD.IsolineMetric());

        List<IsochroneData> isochrones = new ArrayList<IsochroneData>();
        for (int minutes = spacing, n = 0; minutes <= surf.cutoffMinutes && n < nMax; minutes += spacing, n++) {
            int seconds = minutes * 60;
            WTWD z0 = new WTWD();
            z0.w = 1.0;
            z0.wTime = seconds;
            z0.d = 300; // meters. TODO set dynamically / properly, make sure it matches grid cell size?
            IsochroneData isochrone = new IsochroneData(seconds, isolineBuilder.computeIsoline(z0));
            isochrones.add(isochrone);
         }

        long t1 = System.currentTimeMillis();
        LOG.debug("Computed {} isochrones in {} msec", isochrones.size(), (int) (t1 - t0));

        return isochrones;
    }

    /**
     * Produce a single grayscale raster of travel time, like travel time tiles but not broken into tiles.
     */
    @Path("/{surfaceId}/raster")
    @GET @Produces("image/*")
    public Response getRaster(
            @PathParam("surfaceId") Integer surfaceId,
            @QueryParam("width") @DefaultValue("1024") Integer width,
            @QueryParam("height") @DefaultValue("768") Integer height,
            @QueryParam("resolution") Double resolution,
            @QueryParam("time") IsoTimeParameter time,
            @QueryParam("format") @DefaultValue("image/geotiff") MIMEImageFormat format,
            @QueryParam("crs") @DefaultValue("EPSG:4326") CRSParameter crs) throws Exception {

        TimeSurface surface = otpServer.surfaceCache.get(surfaceId);
        Router router = otpServer.getRouter(surface.routerId);
        // BoundingBox is a subclass of Envelope, an Envelope2D constructor parameter
        Envelope2D bbox = new Envelope2D(router.graph.getGeomIndex().getBoundingBox(crs.crs));
        if (resolution != null) {
            width  = (int) Math.ceil(bbox.width  / resolution);
            height = (int) Math.ceil(bbox.height / resolution);
        }

        TileRequest tileRequest = new TileRequest(bbox, width, height);
        RenderRequest renderRequest = new RenderRequest(format, Layer.TRAVELTIME, Style.GRAY, false, false);
        return router.renderer.getResponse(tileRequest, surface, null, renderRequest);
    }


}

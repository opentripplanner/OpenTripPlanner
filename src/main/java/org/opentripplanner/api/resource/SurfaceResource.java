package org.opentripplanner.api.resource;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import org.apache.commons.math3.util.FastMath;
import org.geotools.feature.FeatureCollection;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.geometry.Envelope2D;
import org.opentripplanner.analyst.ResultSet;
import org.opentripplanner.analyst.PointSet;
import org.opentripplanner.analyst.SampleSet;
import org.opentripplanner.analyst.TimeSurface;
import org.opentripplanner.analyst.core.IsochroneData;
import org.opentripplanner.analyst.core.Sample;
import org.opentripplanner.analyst.core.SlippyTile;
import org.opentripplanner.analyst.request.IsoChroneRequest;
import org.opentripplanner.analyst.request.RenderRequest;
import org.opentripplanner.analyst.request.Renderer;
import org.opentripplanner.analyst.request.SampleGridRenderer;
import org.opentripplanner.analyst.request.SampleGridRequest;
import org.opentripplanner.analyst.request.TileRequest;
import org.opentripplanner.api.common.ParameterException;
import org.opentripplanner.api.common.RoutingResource;
import org.opentripplanner.api.model.TimeSurfaceShort;
import org.opentripplanner.api.parameter.Layer;
import org.opentripplanner.api.parameter.LayerList;
import org.opentripplanner.api.parameter.MIMEImageFormat;
import org.opentripplanner.api.parameter.Style;
import org.opentripplanner.api.parameter.StyleList;
import org.opentripplanner.common.geometry.AccumulativeGridSampler;
import org.opentripplanner.common.geometry.DelaunayIsolineBuilder;
import org.opentripplanner.common.geometry.DistanceLibrary;
import org.opentripplanner.common.geometry.IsolineBuilder;
import org.opentripplanner.common.geometry.RecursiveGridIsolineBuilder;
import org.opentripplanner.common.geometry.SparseMatrixZSampleGrid;
import org.opentripplanner.common.geometry.ZSampleGrid;
import org.opentripplanner.routing.algorithm.EarliestArrivalSPTService;
import org.opentripplanner.routing.algorithm.GenericAStar;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.services.SPTService;
import org.opentripplanner.routing.spt.SPTWalker;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.standalone.OTPServer;
import org.opentripplanner.standalone.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opentripplanner.analyst.request.SampleGridRenderer.WTWD;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
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
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.apache.commons.math3.util.FastMath.toRadians;

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
            RoutingRequest req = buildRequest(0); // batch must be true
           
            // routerId is optional -- select default graph if not set
            Router router = otpServer.getRouter(routerId);
            req.setRoutingContext(router.graph);
        	
            EarliestArrivalSPTService sptService = new EarliestArrivalSPTService();
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
        final ResultSet indicator = new ResultSet(samples, surf);
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
            @QueryParam("spacing") int spacing) {
        final TimeSurface surf = otpServer.surfaceCache.get(surfaceId);
        if (surf == null) return badRequest("Invalid TimeSurface ID.");
        if (spacing < 1) spacing = 5;
        List<IsochroneData> isochrones = getIsochronesAccumulative(surf, spacing);
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
     */
    public List<IsochroneData> getIsochronesAccumulative(TimeSurface surf, int spacing) {

        long t0 = System.currentTimeMillis();
        DelaunayIsolineBuilder<WTWD> isolineBuilder = new DelaunayIsolineBuilder<WTWD>(
                surf.sampleGrid.delaunayTriangulate(), new WTWD.IsolineMetric());

        List<IsochroneData> isochrones = new ArrayList<IsochroneData>();
        for (int minutes = spacing; minutes <= surf.cutoffMinutes; minutes += spacing) {
            int seconds = minutes * 60;
            WTWD z0 = new WTWD();
            z0.w = 1.0;
            z0.wTime = seconds;
            z0.d = 300; // meters. TODO set dynamically / properly, make sure it matches grid cell size?
            IsochroneData isochrone = new IsochroneData(seconds, isolineBuilder.computeIsoline(z0));
            isochrones.add(isochrone);
        }

        long t1 = System.currentTimeMillis();
        LOG.debug("Computed {} isochrones in {}msec", isochrones.size(), (int) (t1 - t0));

        return isochrones;
    }

}

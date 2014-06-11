package org.opentripplanner.api.resource;

import com.bedatadriven.geojson.GeometrySerializer;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import org.geotools.feature.FeatureCollection;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.geometry.Envelope2D;
import org.opentripplanner.analyst.Indicator;
import org.opentripplanner.analyst.PointSet;
import org.opentripplanner.analyst.TimeSurface;
import org.opentripplanner.analyst.core.IsochroneData;
import org.opentripplanner.analyst.core.Sample;
import org.opentripplanner.analyst.core.SlippyTile;
import org.opentripplanner.analyst.request.RenderRequest;
import org.opentripplanner.analyst.request.Renderer;
import org.opentripplanner.analyst.request.TileRequest;
import org.opentripplanner.api.common.ParameterException;
import org.opentripplanner.api.common.RoutingResource;
import org.opentripplanner.api.model.TimeSurfaceShort;
import org.opentripplanner.api.parameter.Layer;
import org.opentripplanner.api.parameter.LayerList;
import org.opentripplanner.api.parameter.MIMEImageFormat;
import org.opentripplanner.api.parameter.Style;
import org.opentripplanner.api.parameter.StyleList;
import org.opentripplanner.common.geometry.RecursiveGridIsolineBuilder;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.algorithm.EarliestArrivalSPTService;
import org.opentripplanner.routing.algorithm.GenericAStar;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.services.SPTService;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.standalone.OTPServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

@Path("/surfaces")
@Produces({ MediaType.APPLICATION_JSON })
public class SurfaceResource extends RoutingResource {

    @Context
    OTPServer server;

    @Context
    UriInfo uriInfo;

    @POST
    public Response createSurface(@QueryParam("cutoffMinutes") @DefaultValue("90") int cutoffMinutes) {

        // Build the request
        try {
            RoutingRequest req = buildRequest(0); // batch must be true
            Graph graph = server.graphService.getGraph();
            req.setRoutingContext(graph);
            EarliestArrivalSPTService sptService = new EarliestArrivalSPTService();
            sptService.setMaxDuration(60 * cutoffMinutes);
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
                server.surfaceCache.add(surface);
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
        return Response.ok().entity(TimeSurfaceShort.list(server.surfaceCache.cache)).build();
    }

    /** Describe a specific surface. */
    @GET @Path("/{surfaceId}")
    public Response getTimeSurfaceList (@PathParam("surfaceId") Integer surfaceId) {
        TimeSurface surface = server.surfaceCache.get(surfaceId);
        if (surface == null) return Response.status(Response.Status.NOT_FOUND).entity("Invalid surface ID.").build();
        return Response.ok().entity(new TimeSurfaceShort(surface)).build();
    }

    /** Evaluate a surface at all the points in a PointSet. */
    @GET @Path("/{surfaceId}/indicator")
    public Response getIndicator (@PathParam("surfaceId") Integer surfaceId,
                                  @QueryParam("targets")  String  targetPointSetId,
                                  @QueryParam("origins")  String  originPointSetId,
                                  @QueryParam("detail")   boolean detail) {

        final TimeSurface surf = server.surfaceCache.get(surfaceId);
        if (surf == null) return badRequest("Invalid TimeSurface ID.");
        final PointSet pset = server.pointSetCache.get(targetPointSetId);
        if (pset == null) return badRequest("Missing or invalid target PointSet ID.");
        final Indicator indicator = new Indicator(pset, surf, detail);
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
            @QueryParam("cutoffMinutes") List<Integer> cutoffs) {
        final TimeSurface surf = server.surfaceCache.get(surfaceId);
        if (surf == null) return badRequest("Invalid TimeSurface ID.");
        if (cutoffs == null || cutoffs.isEmpty()) {
            cutoffs.add(surf.cutoffMinutes);
            cutoffs.add(surf.cutoffMinutes / 2);
        }
        List<IsochroneData> isochrones = getIsochrones(surf, cutoffs);
        final FeatureCollection fc = LIsochrone.makeContourFeatures(isochrones);
        return Response.ok().entity(new StreamingOutput() {
            @Override
            public void write(OutputStream output) throws IOException, WebApplicationException {
                FeatureJSON fj = new FeatureJSON();
                fj.writeFeatureCollection(fc, output);
            }
        }).build();
    }

    private Response badRequest(String message) {
        return Response.status(Response.Status.BAD_REQUEST).entity("Bad request: " + message).build();
    }

    private Response badServer(String message) {
        return Response.status(Response.Status.BAD_REQUEST).entity("Server fail: " + message).build();
    }

    /**
     * Use Laurent's recursive grid sampler. Cutoffs in minutes.
     * Based on IsoChroneSPTRendererRecursiveGrid.getIsochrones()
     * We could also do accumulative sampling into a grid and store that grid.
     */
    private List<IsochroneData> getIsochrones(final TimeSurface surf, List<Integer> cutoffs) {
        List<Coordinate> initialPoints = Lists.newArrayList();
        Graph graph = server.graphService.getGraph();
        for (StreetVertex sv : Iterables.filter(graph.getVertices(), StreetVertex.class)) {
            if (surf.getTime(sv) != TimeSurface.UNREACHABLE) initialPoints.add(sv.getCoordinate());
        }
        RecursiveGridIsolineBuilder.ZFunc timeFunc = new RecursiveGridIsolineBuilder.ZFunc() {
            @Override
            public long z(Coordinate c) {
                // TODO not multi-graph compatible
                Sample sample = server.sampleFactory.getSample(c.x, c.y);
                if (sample == null) return Long.MAX_VALUE;
                Long z = sample.eval(surf);
                return z;
            }
        };
        Coordinate center = new Coordinate(surf.lon, surf.lat);
        double gridSizeMeters = 400;
        double dY = Math.toDegrees(gridSizeMeters / SphericalDistanceLibrary.RADIUS_OF_EARTH_IN_M);
        double dX = dY / Math.cos(Math.toRadians(center.x));
        RecursiveGridIsolineBuilder isolineBuilder =
                new RecursiveGridIsolineBuilder(dX, dY, center, timeFunc, initialPoints);
        List<IsochroneData> isochrones = new ArrayList<IsochroneData>();
        for (int cutoff : cutoffs) {
            int cutoffSec = cutoff * 60;
            Geometry isoline = isolineBuilder.computeIsoline(cutoffSec);
            IsochroneData isochrone = new IsochroneData(cutoffSec, isoline);
            isochrones.add(isochrone);
        }
        return isochrones;
    }

    @Path("/{surfaceId}/isotiles/{z}/{x}/{y}.png")
    @GET @Produces("image/png")
    public Response tileGet(@PathParam("surfaceId") Integer surfaceId,
                            @PathParam("x") int x,
                            @PathParam("y") int y,
                            @PathParam("z") int z) throws Exception {

            Envelope2D env = SlippyTile.tile2Envelope(x, y, z);
            TileRequest tileRequest = new TileRequest(env, 256, 256);
            TimeSurface surfA = server.surfaceCache.get(surfaceId);
            if (surfA == null) return badRequest("Unrecognized surface ID.");
            MIMEImageFormat imageFormat = new MIMEImageFormat("image/png");
            RenderRequest renderRequest =
                new RenderRequest(imageFormat, Layer.TRAVELTIME, Style.COLOR30, true, false);
            // TODO why can't the renderer be static?
            return server.renderer.getResponse(tileRequest, surfA, null, renderRequest);
        }


}

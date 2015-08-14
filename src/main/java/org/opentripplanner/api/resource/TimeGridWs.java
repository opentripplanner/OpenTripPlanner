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

package org.opentripplanner.api.resource;

import java.io.ByteArrayOutputStream;
import java.util.Date;
import java.util.Locale;

import javax.ws.rs.*;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;

import org.apache.commons.codec.binary.Base64;
import org.opentripplanner.analyst.request.SampleGridRenderer.WTWD;
import org.opentripplanner.analyst.request.SampleGridRequest;
import org.opentripplanner.api.common.RoutingResource;
import org.opentripplanner.common.geometry.ZSampleGrid;
import org.opentripplanner.common.geometry.ZSampleGrid.ZSamplePoint;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.standalone.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.ImageLineHelper;
import ar.com.hjg.pngj.ImageLineInt;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.chunks.PngChunkTEXT;
import ar.com.hjg.pngj.chunks.PngChunkTextVar;

/**
 * A Jersey web service resource class that returns a grid with time for a set of points.
 * 
 * Example of request:
 * 
 * <code>
 * http://localhost:8080/otp/routers/bordeaux/timegrid?fromPlace=47.059,-0.880&date=2013/10/01&time=12:00:00&maxWalkDistance=1000&maxTimeSec=3600&mode=WALK,TRANSIT
 * </code>
 * 
 * @author laurent
 */
@Path("/routers/{routerId}/timegrid")
public class TimeGridWs extends RoutingResource {

    public enum DataChannel {
        TIME, /* Clock time */
        BOARDINGS, /* Number of boardings */
        WALK_DISTANCE, /* Total walk distance */
    }

    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(TimeGridWs.class);

    @QueryParam("maxTimeSec")
    private Integer maxTimeSec;

    @QueryParam("precisionMeters")
    @DefaultValue("200")
    private Integer precisionMeters;

    @QueryParam("offRoadDistanceMeters")
    @DefaultValue("150")
    private Integer offRoadDistanceMeters;

    @QueryParam("coordinateOrigin")
    private String coordinateOrigin;

    @QueryParam("zDataType")
    @DefaultValue("TIME")
    private DataChannel zDataType;

    private static final String OTPA_GRID_CORNER = "OTPA-Grid-Corner";

    private static final String OTPA_GRID_CELL_SIZE = "OTPA-Grid-Cell-Size";

    private static final String OTPA_OFFROAD_DIST = "OTPA-OffRoad-Dist";

    @GET
    @Produces({ "image/png" })
    public Response getTimeGridPng(@QueryParam("base64") @DefaultValue("false") boolean base64) throws Exception {

        /* Fetch the Router for this request using server and routerId fields from superclass. */
        Router router = otpServer.getRouter(routerId);

        if (precisionMeters < 10)
            throw new IllegalArgumentException("Too small precisionMeters: " + precisionMeters);
        if (offRoadDistanceMeters < 10)
            throw new IllegalArgumentException("Too small offRoadDistanceMeters: " + offRoadDistanceMeters);

        // Build the request
        RoutingRequest sptRequest = buildRequest();
        SampleGridRequest tgRequest = new SampleGridRequest();
        tgRequest.maxTimeSec = maxTimeSec;
        tgRequest.precisionMeters = precisionMeters;
        tgRequest.offRoadDistanceMeters = offRoadDistanceMeters;
        if (coordinateOrigin != null)
            tgRequest.coordinateOrigin = new GenericLocation(null, coordinateOrigin).getCoordinate();

        // Get a sample grid
		ZSampleGrid<WTWD> sampleGrid = router.sampleGridRenderer.getSampleGrid(tgRequest, sptRequest);

        int cols = sampleGrid.getXMax() - sampleGrid.getXMin() + 1;
        int rows = sampleGrid.getYMax() - sampleGrid.getYMin() + 1;
        int channels = 4; // Hard-coded to RGBA

        // We force to 8 bits channel depth, some clients won't support more than 8
        // (namely, HTML5 canvas...)
        ImageInfo imgInfo = new ImageInfo(cols, rows, 8, true, false, false);
        /**
         * TODO: PNGJ allow for progressive (ie line-by-line) writing. Thus we could theorically
         * prevent having to allocate a bit pixel array in the first place, but we would need a
         * line-by-line iterator on the sample grid, which is currently not the case.
         */
        int[][] rgba = new int[rows][cols * channels];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PngWriter pw = new PngWriter(baos, imgInfo);

        pw.getMetadata().setText(PngChunkTextVar.KEY_Software, "OTPA");
        pw.getMetadata().setText(PngChunkTextVar.KEY_Creation_Time, new Date().toString());
        pw.getMetadata().setText(PngChunkTextVar.KEY_Description, "Sample grid bitmap");

        String gridCornerStr = String.format(Locale.US, "%.8f,%.8f", sampleGrid.getCenter().y
                + sampleGrid.getYMin() * sampleGrid.getCellSize().y, sampleGrid.getCenter().x
                + sampleGrid.getXMin() * sampleGrid.getCellSize().x);
        String gridCellSzStr = String.format(Locale.US, "%.12f,%.12f", sampleGrid.getCellSize().y,
                sampleGrid.getCellSize().x);
        String offRoadDistStr = String.format(Locale.US, "%d", offRoadDistanceMeters);

        PngChunkTEXT gridCornerChunk = new PngChunkTEXT(imgInfo);
        gridCornerChunk.setKeyVal(OTPA_GRID_CORNER, gridCornerStr);
        pw.getChunksList().queue(gridCornerChunk);
        PngChunkTEXT gridCellSzChunk = new PngChunkTEXT(imgInfo);
        gridCellSzChunk.setKeyVal(OTPA_GRID_CELL_SIZE, gridCellSzStr);
        pw.getChunksList().queue(gridCellSzChunk);
        PngChunkTEXT offRoadDistChunk = new PngChunkTEXT(imgInfo);
        offRoadDistChunk.setKeyVal(OTPA_OFFROAD_DIST, offRoadDistStr);
        pw.getChunksList().queue(offRoadDistChunk);

        double unit;
        switch (zDataType) {
        case TIME:
            unit = 1.0; // 1:1sec, max 18h
            break;
        case BOARDINGS:
            unit = 1000.0; // 1:0.001 boarding, max 65.5
            break;
        case WALK_DISTANCE:
            unit = 10.0; // 1:0.1m, max 6.55km
            break;
        default:
            throw new IllegalArgumentException("Unsupported Z DataType.");
        }

        for (ZSamplePoint<WTWD> p : sampleGrid) {
            WTWD z = p.getZ();
            int row = p.getY() - sampleGrid.getYMin();
            int col = p.getX() - sampleGrid.getXMin();
            double zz;
            switch (zDataType) {
            case TIME:
                zz = z.wTime / z.w;
                break;
            case BOARDINGS:
                zz = z.wBoardings / z.w;
                break;
            case WALK_DISTANCE:
                zz = z.wWalkDist / z.w;
                break;
            default:
                throw new IllegalArgumentException("Unsupported Z DataType.");
            }
            int iz;
            if (Double.isInfinite(zz)) {
                iz = 65535;
            } else {
                iz = ImageLineHelper.clampTo_0_65535((int) Math.round(zz * unit));
                if (iz == 65535)
                    iz = 65534; // Clamp
            }
            // d is expressed as a percentage of grid size, max 255%.
            // Sometimes d will be bigger than 2.55 x grid size,
            // but this should not be too much important as we are off-bounds.
            int id = ImageLineHelper.clampTo_0_255((int) Math.round(z.d / precisionMeters * 100));
            int offset = col * channels;
            rgba[row][offset + 0] = (iz & 0xFF); // z low 8 bits
            rgba[row][offset + 1] = (iz >> 8); // z high 8 bits
            rgba[row][offset + 2] = id; // d
            /*
             * Keep the alpha channel at 255, otherwise the RGB channel will be downsampled on some
             * rendering clients (namely, JS canvas).
             */
            rgba[row][offset + 3] = 255;
        }
        for (int row = 0; row < rgba.length; row++) {
            ImageLineInt iline = new ImageLineInt(imgInfo, rgba[row]);
            pw.writeRow(iline, row);
        }
        pw.end();

        // Disallow caching on client side
        CacheControl cc = new CacheControl();
        cc.setNoCache(true);
        // Also put the meta-data in the HTML header (easier to read from JS)
        byte[] data = baos.toByteArray();
        if (base64) {
            data = Base64.encodeBase64(data);
        }
        return Response.ok().cacheControl(cc).entity(data).header(OTPA_GRID_CORNER, gridCornerStr)
                .header(OTPA_GRID_CELL_SIZE, gridCellSzStr)
                .header(OTPA_OFFROAD_DIST, offRoadDistStr).build();
    }
}
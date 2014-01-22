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

import java.io.ByteArrayOutputStream;
import java.util.Date;
import java.util.Locale;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.opentripplanner.analyst.core.TimeGrid;
import org.opentripplanner.analyst.request.SampleGridRenderer;
import org.opentripplanner.analyst.request.SampleGridRenderer.WTWD;
import org.opentripplanner.analyst.request.SampleGridRequest;
import org.opentripplanner.api.common.RoutingResource;
import org.opentripplanner.common.geometry.ZSampleGrid;
import org.opentripplanner.common.geometry.ZSampleGrid.TimeGridMapper;
import org.opentripplanner.common.geometry.ZSampleGrid.ZSamplePoint;
import org.opentripplanner.routing.core.RoutingRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.ImageLineHelper;
import ar.com.hjg.pngj.ImageLineInt;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.chunks.PngChunkPHYS;
import ar.com.hjg.pngj.chunks.PngChunkTEXT;
import ar.com.hjg.pngj.chunks.PngChunkTextVar;

import com.sun.jersey.api.core.InjectParam;
import com.sun.jersey.core.util.Base64;

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

    private static final String OTPA_GRID_CORNER = "OTPA-Grid-Corner";

    private static final String OTPA_GRID_CELL_SIZE = "OTPA-Grid-Cell-Size";

    private static final String OTPA_OFFROAD_DIST = "OTPA-OffRoad-Dist";

    /** 
     * TODO Remove this method (unused).
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Path("/{filename}.json")
    public Response getTimeGridJson() throws Exception {

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

    @GET
    @Produces({ "image/png" })
    @Path("/{filename}.png")
    public Response getTimeGridPng(@QueryParam("base64") @DefaultValue("false") boolean base64)
            throws Exception {

        if (precisionMeters < 10)
            throw new IllegalArgumentException("Too small precisionMeters: " + precisionMeters);

        // Build the request
        RoutingRequest sptRequest = buildRequest(0);
        SampleGridRequest tgRequest = new SampleGridRequest();
        tgRequest.setMaxTimeSec(maxTimeSec);
        tgRequest.setPrecisionMeters(precisionMeters);

        // Get a sample grid
        ZSampleGrid<WTWD> sampleGrid = sampleGridRenderer.getSampleGrid(tgRequest, sptRequest);
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

        /*
         * PNG specs: pHYSs are in pixel per unit, that is pixel per METERS, and are defined as
         * integers. Here we are having 0.something pixel per meters...
         */
        PngChunkPHYS pHYS = new PngChunkPHYS(imgInfo);
        pHYS.setPixelsxUnitX(0); // ???
        pHYS.setPixelsxUnitY(0); // ???
        pHYS.setUnits(1); // Meters
        pw.getChunksList().queue(pHYS);

        String gridCornerStr = String.format(Locale.US, "%f,%f", sampleGrid.getCenter().x
                + sampleGrid.getXMin() * sampleGrid.getCellSize().x, sampleGrid.getCenter().y
                + sampleGrid.getYMin() * sampleGrid.getCellSize().y);
        String gridCellSzStr = String.format(Locale.US, "%f,%f", sampleGrid.getCellSize().x,
                sampleGrid.getCellSize().y);
        String offRoadDistStr = String.format(Locale.US, "%f",
                sampleGridRenderer.getOffRoadDistanceMeters(precisionMeters));

        PngChunkTEXT gridCornerChunk = new PngChunkTEXT(imgInfo);
        gridCornerChunk.setKeyVal(OTPA_GRID_CORNER, gridCornerStr);
        pw.getChunksList().queue(gridCornerChunk);
        PngChunkTEXT gridCellSzChunk = new PngChunkTEXT(imgInfo);
        gridCellSzChunk.setKeyVal(OTPA_GRID_CELL_SIZE, gridCellSzStr);
        pw.getChunksList().queue(gridCellSzChunk);
        PngChunkTEXT offRoadDistChunk = new PngChunkTEXT(imgInfo);
        offRoadDistChunk.setKeyVal(OTPA_OFFROAD_DIST, offRoadDistStr);
        pw.getChunksList().queue(offRoadDistChunk);

        for (ZSamplePoint<WTWD> p : sampleGrid) {
            WTWD z = p.getZ();
            int row = p.getY() - sampleGrid.getYMin();
            int col = p.getX() - sampleGrid.getXMin();
            double t = z.tw / z.w;
            int it;
            if (Double.isInfinite(t)) {
                it = 65535;
            } else {
                it = ImageLineHelper.clampTo_0_65535((int) Math.round(t));
                if (it == 65535)
                    it = 65534;
            }
            int id = ImageLineHelper.clampTo_0_255((int) Math.round(z.d / 2));
            int offset = col * channels;
            rgba[row][offset + 0] = it & 0xFF; // t low 8 bits
            rgba[row][offset + 1] = it >> 8; // t high 8 bits
            rgba[row][offset + 2] = id; // d
            rgba[row][offset + 3] = 255; // TODO USE IT
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
            data = Base64.encode(data);
        }
        return Response.ok().cacheControl(cc).entity(data).header(OTPA_GRID_CORNER, gridCornerStr)
                .header(OTPA_GRID_CELL_SIZE, gridCellSzStr)
                .header(OTPA_OFFROAD_DIST, offRoadDistStr).build();
    }
}
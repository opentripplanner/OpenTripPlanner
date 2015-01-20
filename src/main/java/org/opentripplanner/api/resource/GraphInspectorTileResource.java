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

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.geotools.geometry.Envelope2D;
import org.opentripplanner.analyst.core.SlippyTile;
import org.opentripplanner.analyst.request.TileRequest;
import org.opentripplanner.api.common.RoutingResource;
import org.opentripplanner.api.parameter.MIMEImageFormat;

import static org.opentripplanner.api.resource.ServerInfo.Q;

import org.opentripplanner.inspector.TileRenderer;
import org.opentripplanner.standalone.OTPServer;
import org.opentripplanner.standalone.Router;

/**
 * Slippy map tile API for rendering various graph information for inspection/debugging purpose
 * (bike safety factor, connectivity...).
 * 
 * One can easily add a new layer by adding the following kind of code to a leaflet map:
 * 
 * <pre>
 *   var bikesafety = new L.TileLayer(
 *      'http://localhost:8080/otp/routers/default/inspector/tile/bike-safety/{z}/{x}/{y}.png',
 *      { maxZoom : 22 });
 *   var map = L.map(...);
 *   L.control.layers(null, { "Bike safety": bikesafety }).addTo(map);
 * </pre>
 * 
 * Tile rendering goes through TileRendererManager which select the appropriate renderer for the
 * given layer.
 * 
 * @see TileRendererManager
 * @see TileRenderer
 * 
 * @author laurent
 * 
 */
@Path("/routers/{routerId}/inspector")
public class GraphInspectorTileResource extends RoutingResource {

    @Context
    private OTPServer otpServer;

    @PathParam("x")
    int x;

    @PathParam("y")
    int y;

    @PathParam("z")
    int z;

    @PathParam("routerId")
    String routerId;

    @PathParam("layer")
    String layer;

    @PathParam("ext")
    String ext;

    @GET @Path("/tile/{layer}/{z}/{x}/{y}.{ext}")
    @Produces("image/*")
    public Response tileGet() throws Exception {

        // Re-use analyst
        Envelope2D env = SlippyTile.tile2Envelope(x, y, z);
        TileRequest tileRequest = new TileRequest(env, 256, 256);

        Router router = otpServer.getRouter(routerId);
        BufferedImage image = router.tileRendererManager.renderTile(tileRequest, layer);

        MIMEImageFormat format = new MIMEImageFormat("image/" + ext);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(image.getWidth() * image.getHeight() / 4);
        ImageIO.write(image, format.type, baos);
        CacheControl cc = new CacheControl();
        cc.setMaxAge(3600);
        cc.setNoCache(false);
        return Response.ok(baos.toByteArray()).type(format.toString()).cacheControl(cc).build();
    }

    /**
     * Gets all layer names
     * 
     * Used in fronted to create layer chooser
     * @return 
     */
    @GET @Path("layers")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML + Q, MediaType.TEXT_XML + Q })
    public InspectorLayersList getLayers() {

        Router router = otpServer.getRouter(routerId);
        InspectorLayersList layersList = new InspectorLayersList(router.tileRendererManager.getRenderers());
        return layersList;
    }

}
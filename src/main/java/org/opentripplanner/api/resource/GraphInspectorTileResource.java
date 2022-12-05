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
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.api.parameter.MIMEImageFormat;
import org.opentripplanner.inspector.raster.MapTile;
import org.opentripplanner.inspector.raster.TileRenderer;
import org.opentripplanner.inspector.raster.TileRendererManager;
import org.opentripplanner.standalone.api.OtpServerRequestContext;

/**
 * Slippy map tile API for rendering various graph information for inspection/debugging purpose
 * (bike safety factor, connectivity...).
 * <p>
 * One can easily add a new layer by adding the following kind of code to a leaflet map:
 *
 * <pre>
 *   var bikesafety = new L.TileLayer(
 *      'http://localhost:8080/otp/routers/default/inspector/tile/bike-safety/{z}/{x}/{y}.png',
 *      { maxZoom : 22 });
 *   var map = L.map(...);
 *   L.control.layers(null, { "Bike safety": bikesafety }).addTo(map);
 * </pre>
 * <p>
 * Tile rendering goes through TileRendererManager which select the appropriate renderer for the
 * given layer.
 *
 * @author laurent
 * @see TileRendererManager
 * @see TileRenderer
 */
@Path("/routers/{ignoreRouterId}/inspector")
public class GraphInspectorTileResource {

  private final OtpServerRequestContext serverContext;

  public GraphInspectorTileResource(
    @Context OtpServerRequestContext serverContext,
    /**
     * @deprecated The support for multiple routers are removed from OTP2.
     * See https://github.com/opentripplanner/OpenTripPlanner/issues/2760
     */
    @Deprecated @PathParam("ignoreRouterId") String ignoreRouterId
  ) {
    this.serverContext = serverContext;
  }

  @GET
  @Path("/tile/{layer}/{z}/{x}/{y}.{ext}")
  @Produces("image/*")
  public Response tileGet(
    @PathParam("x") int x,
    @PathParam("y") int y,
    @PathParam("z") int z,
    @PathParam("layer") String layer,
    @PathParam("ext") String ext
  ) throws Exception {
    // Re-use analyst
    Envelope env = WebMercatorTile.tile2Envelope(x, y, z);
    MapTile mapTile = new MapTile(env, 256, 256);

    OtpServerRequestContext serverContext = this.serverContext;
    BufferedImage image = serverContext.tileRendererManager().renderTile(mapTile, layer);

    MIMEImageFormat format = new MIMEImageFormat("image/" + ext);
    ByteArrayOutputStream baos = new ByteArrayOutputStream(
      image.getWidth() * image.getHeight() / 4
    );
    ImageIO.write(image, format.type, baos);
    CacheControl cc = new CacheControl();
    cc.setMaxAge(3600);
    cc.setNoCache(false);
    return Response.ok(baos.toByteArray()).type(format.toString()).cacheControl(cc).build();
  }

  /**
   * Gets all layer names
   * <p>
   * Used in fronted to create layer chooser
   */
  @GET
  @Path("layers")
  @Produces(MediaType.APPLICATION_JSON)
  public InspectorLayersList getLayers() {
    OtpServerRequestContext serverContext = this.serverContext;
    return new InspectorLayersList(serverContext.tileRendererManager().getRenderers());
  }
}

package org.opentripplanner.ext.debugrastertiles.api.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.api.resource.WebMercatorTile;
import org.opentripplanner.ext.debugrastertiles.MapTile;
import org.opentripplanner.ext.debugrastertiles.TileRenderer;
import org.opentripplanner.ext.debugrastertiles.TileRendererManager;
import org.opentripplanner.standalone.api.OtpServerRequestContext;

/**
 * Slippy raster map tile API for rendering various graph information for inspection/debugging
 * purpose (bike safety factor, connectivity...). Vector tile alternatives should be preferably used
 * instead.
 * <p>
 * Tile rendering goes through TileRendererManager which select the appropriate renderer for the
 * given layer.
 *
 * @author laurent
 * @see TileRendererManager
 * @see TileRenderer
 */
@Path("/debugrastertiles")
public class DebugRasterTileResource {

  private final TileRendererManager tileRendererManager;

  public DebugRasterTileResource(@Context OtpServerRequestContext serverContext) {
    this.tileRendererManager = new TileRendererManager(
      serverContext.graph(),
      serverContext.defaultRouteRequest().preferences().wheelchair()
    );
  }

  @GET
  @Path("/{layer}/{z}/{x}/{y}.{ext}")
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

    BufferedImage image = tileRendererManager.renderTile(mapTile, layer);

    MIMEImageFormat format = new MIMEImageFormat("image/" + ext);
    ByteArrayOutputStream baos = new ByteArrayOutputStream(
      (image.getWidth() * image.getHeight()) / 4
    );
    ImageIO.write(image, format.type, baos);
    CacheControl cc = new CacheControl();
    cc.setMaxAge(3600);
    cc.setNoCache(false);
    return Response.ok(baos.toByteArray()).type(format.toString()).cacheControl(cc).build();
  }
}

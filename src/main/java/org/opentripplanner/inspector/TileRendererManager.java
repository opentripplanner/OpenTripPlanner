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

package org.opentripplanner.inspector;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import org.geotools.geometry.Envelope2D;
import org.opentripplanner.analyst.request.TileRequest;
import org.opentripplanner.api.resource.GraphInspectorTileResource;
import org.opentripplanner.inspector.TileRenderer.TileRenderContext;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.GraphService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.util.AffineTransformation;

/**
 * Process slippy map tile rendering requests. Get the tile renderer for the given layer, setup a
 * tile rendering context (bounding box, image graphic context, affine transform, etc...) and call
 * the renderer to paint the tile.
 * 
 * @see GraphInspectorTileResource
 * @see TileRenderer
 * 
 * @author laurent
 * 
 */
public class TileRendererManager {

    private static final Logger LOG = LoggerFactory.getLogger(TileRendererManager.class);

    private Map<String, TileRenderer> renderers = new HashMap<String, TileRenderer>();

    private Graph graph;

    public TileRendererManager(Graph graph) {
        this.graph = graph;

        // Register layers.
        renderers.put("bike-safety", new EdgeVertexTileRenderer(new BikeSafetyEdgeRenderer()));
        renderers.put("traversal", new EdgeVertexTileRenderer(
                new TraversalPermissionsEdgeRenderer()));
        renderers.put("wheelchair", new EdgeVertexTileRenderer(new WheelchairEdgeRenderer()));
    }

    public void registerRenderer(String layer, TileRenderer tileRenderer) {
        renderers.put(layer, tileRenderer);
    }

    public BufferedImage renderTile(final TileRequest tileRequest, String layer) {

        TileRenderContext context = new TileRenderContext() {
            @Override
            public Envelope expandPixels(double marginXPixels, double marginYPixels) {
                Envelope retval = new Envelope(bbox);
                retval.expandBy(
                        marginXPixels / tileRequest.width * (bbox.getMaxX() - bbox.getMinX()),
                        marginYPixels / tileRequest.height * (bbox.getMaxY() - bbox.getMinY()));
                return retval;
            }
        };

        context.graph = graph;

        TileRenderer renderer = renderers.get(layer);
        if (renderer == null)
            throw new IllegalArgumentException("Unknown layer: " + layer);

        // The best place for caching tiles may be here
        BufferedImage image = new BufferedImage(tileRequest.width, tileRequest.height,
                renderer.getColorModel());
        context.graphics = image.createGraphics();
        Envelope2D trbb = tileRequest.bbox;
        context.bbox = new Envelope(trbb.x, trbb.x + trbb.width, trbb.y, trbb.y + trbb.height);
        context.transform = new AffineTransformation();
        double xScale = tileRequest.width / trbb.width;
        double yScale = tileRequest.height / trbb.height;

        context.transform.translate(-trbb.x, -trbb.y - trbb.height);
        context.transform.scale(xScale, -yScale);
        context.metersPerPixel = Math.toRadians(trbb.height) * 6371000 / tileRequest.height;
        context.tileWidth = tileRequest.width;
        context.tileHeight = tileRequest.height;

        long start = System.currentTimeMillis();
        renderer.renderTile(context);
        LOG.debug("Rendered tile at {},{} in {} ms", tileRequest.bbox.y, tileRequest.bbox.x,
                System.currentTimeMillis() - start);
        return image;
    }

    /**
     * Gets all renderers
     * 
     * Used to return list of renderers to client.
     * Could be also used to show legend.
     * @return 
     */
    public Map<String, TileRenderer> getRenderers() {
        return renderers;
    }
}
package org.opentripplanner.inspector;

import java.awt.Graphics2D;

import org.opentripplanner.routing.graph.Graph;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.util.AffineTransformation;

/**
 * Interface for a slippy map tile renderer.
 * 
 * @author laurent
 */
public interface TileRenderer {

    /**
     * Context used for rendering a tile.
     * 
     */
    public abstract class TileRenderContext {

        /** Graphics where to paint tile to, in pixel CRS (no transform set) */
        public Graphics2D graphics;

        /** The JTS transform that convert from WGS84 CRS to pixel CRS */
        public AffineTransformation transform;

        /** The graph being processed */
        public Graph graph;

        /** Bounding box of the rendered tile in WGS84 CRS, w/o margins */
        public Envelope bbox;

        /** Ground pixel density inverse */
        public double metersPerPixel;

        /** Tile size in pixels */
        public int tileWidth, tileHeight;

        /** Expand the bounding box to add some margins, in pixel size. */
        public abstract Envelope expandPixels(double marginXPixels, double marginYPixels);
    }

    /** Return the BufferedImage color model the renderer would like to use */
    public abstract int getColorModel();

    /** Implementation of the tile rendering */
    public abstract void renderTile(TileRenderContext context);

    /** Gets descriptive name of this Tile Render */
    public abstract String getName();

}
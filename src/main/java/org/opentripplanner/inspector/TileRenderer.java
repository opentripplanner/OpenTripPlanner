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

import java.awt.Graphics2D;

import org.opentripplanner.routing.graph.Graph;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.util.AffineTransformation;

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
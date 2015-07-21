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

package org.opentripplanner.analyst.core;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opentripplanner.analyst.request.TileRequest;
import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateTile extends Tile {

    private static final Logger LOG = LoggerFactory.getLogger(TemplateTile.class);
    Sample[] samples;
    
    public TemplateTile(TileRequest req, Graph graph) {
        super(req);
        this.samples = new Sample[width * height];
        CoordinateReferenceSystem crs = gg.getCoordinateReferenceSystem2D(); 
        int i = 0;
        try {
            MathTransform tr = CRS.findMathTransform(crs, DefaultGeographicCRS.WGS84);
            // grid coordinate object to be reused for examining each cell 
            GridCoordinates2D coord = new GridCoordinates2D();
            for (int gy = 0; gy < height; gy++) {
                if (gy % 100 == 0)
                    LOG.trace("raster line {} / {}", gy, height);
                for (int gx = 0; gx < width; gx++) {
                    coord.x = gx;
                    coord.y = gy;
                    // find coordinates for current raster cell in tile CRS
                    DirectPosition sourcePos = gg.gridToWorld(coord);
                    // convert coordinates in tile CRS to WGS84
                    //LOG.debug("world : {}", sourcePos);
                    tr.transform(sourcePos, sourcePos);
                    //LOG.debug("wgs84 : {}", sourcePos);
                    // axis order can vary
                    double lon = sourcePos.getOrdinate(0);
                    double lat = sourcePos.getOrdinate(1);
                    // TODO: axes are reversed in the default mathtransform
                    Sample s = graph.getSampleFactory().getSample(lon, lat);
                    samples[i++] = s;
                }
            }
        } catch (Exception e) {
            LOG.error("Could not create template tile", e);
        }
    }
    
    public Sample[] getSamples() {
        return this.samples;
    }

}

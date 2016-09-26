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

import java.util.HashMap;
import java.util.Map;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opentripplanner.analyst.request.TileRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateTile extends Tile {

    private static final Logger LOG = LoggerFactory.getLogger(TemplateTile.class);
    private Map<String, Sample[]> samples = new HashMap<String, Sample[]>();
    
    public TemplateTile(TileRequest req, SampleSource sampleSource) {
        super(req);
        LOG.debug("In TemplateTile constructor: about to create samples (rId = {})",
            req.routerId);
        this.samples.put(req.routerId, createSamples(sampleSource, req.routerId));
        if (req.routerId2 != null) {
            LOG.debug("In TemplateTile constructor: about to create samples (rId = {})",
                    req.routerId2);
        	this.samples.put(req.routerId2, createSamples(sampleSource, req.routerId2));
        }
    }
    
    private Sample[] createSamples(SampleSource sampleSource, String routerIdArg) {
        Sample[] sampleSet = new Sample[width * height];
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
                    Sample s = sampleSource.getSample(lon, lat, routerIdArg);
                    sampleSet[i++] = s;
                }
            }
        } catch (Exception e) {
            LOG.error(e.toString());
            e.printStackTrace();
        }
        return sampleSet;
    }
    
    public Sample[] getSamples(String routerIdArg) {
    	if (routerIdArg == null) {
    		routerIdArg = this.routerId;
    	}
    	else if (!(routerIdArg.equals(this.routerId) ||
    			(this.routerId2 != null && routerIdArg.equals(this.routerId2)))) {
            LOG.error("bad routerId passed in to Tile.getSamples() ({}), wasn't "+
                    "one of two allowed routerIds passed to constructor ({}, {}).",
                    routerIdArg, this.routerId, this.routerId2);
            return null;
    	}
    	return this.samples.get(routerIdArg);
    }
}

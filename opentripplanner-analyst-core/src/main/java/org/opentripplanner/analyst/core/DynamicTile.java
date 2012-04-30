package org.opentripplanner.analyst.core;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opentripplanner.analyst.request.TileRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynamicTile extends Tile {

    private static final Logger LOG = LoggerFactory.getLogger(DynamicTile.class);
    final SampleSource ss;
    
    public DynamicTile(TileRequest req, SampleSource sampleSource) {
        super(req);
        this.ss = sampleSource;
    }
    
    public Sample[] getSamples() {
        Sample[] ret = new Sample[width * height];
        long t0 = System.currentTimeMillis();
        CoordinateReferenceSystem crs = gg.getCoordinateReferenceSystem2D();
        try {
            MathTransform tr = CRS.findMathTransform(crs, DefaultGeographicCRS.WGS84);
            // grid coordinate object to be reused for examining each cell 
            GridCoordinates2D coord = new GridCoordinates2D();
            int i = 0, ns = 0;
            for (int gy = 0; gy < height; gy++) {
                for (int gx = 0; gx < width; gx++) {
                    coord.x = gx;
                    coord.y = gy;
                    // find coordinates for current raster cell in tile CRS
                    DirectPosition sourcePos = gg.gridToWorld(coord);
                    // convert coordinates in tile CRS to WGS84
                    tr.transform(sourcePos, sourcePos);
                    // axis order can vary
                    double lon = sourcePos.getOrdinate(0);
                    double lat = sourcePos.getOrdinate(1);
                    Sample s = ss.getSample(lon, lat);
                    if (s != null)
                        ns++;
                    ret[i] = s;
                    i++;
                }
            }
            LOG.debug("finished preparing tile. number of samples: {}", ns); 
        } catch (Exception e) {
            LOG.error(e.getMessage());
            return null;
        }
        long t1 = System.currentTimeMillis();
        LOG.debug("filled in tile image from SPT in {}msec", t1 - t0);
        return ret;
    }

}

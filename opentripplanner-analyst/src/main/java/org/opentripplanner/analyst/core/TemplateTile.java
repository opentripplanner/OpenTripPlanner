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

public class TemplateTile extends Tile {

    private static final Logger LOG = LoggerFactory.getLogger(TemplateTile.class);
    Sample[] samples;
    
    public TemplateTile(TileRequest req, SampleSource sampleSource) {
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
                    Sample s = sampleSource.getSample(lon, lat);
                    samples[i++] = s;
                }
            }
        } catch (Exception e) {
            LOG.error(e.toString());
            e.printStackTrace();
        }
    }
    
    public Sample[] getSamples() {
        return this.samples;
    }

}

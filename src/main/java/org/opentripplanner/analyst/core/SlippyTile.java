package org.opentripplanner.analyst.core;

import org.geotools.geometry.Envelope2D;
import org.geotools.referencing.crs.DefaultGeographicCRS;

/* http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames */
public class SlippyTile {

    public static String getTileNumber(final double lat, final double lon, final int zoom) {
        int xtile = (int)Math.floor( (lon + 180) / 360 * (1<<zoom) ) ;
        int ytile = (int)Math.floor( (1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * (1<<zoom) ) ;
        return("" + zoom + "/" + xtile + "/" + ytile);
    }


    public static double tile2lon(int x, int z) {
        return x / Math.pow(2.0, z) * 360.0 - 180;
    }
    
    public static double tile2lat(int y, int z) {
        double n = Math.PI - (2.0 * Math.PI * y) / Math.pow(2.0, z);
        return Math.toDegrees(Math.atan(Math.sinh(n)));
    }
    
    public static Envelope2D tile2Envelope(final int x, final int y, final int zoom) {
        double maxLat = tile2lat(y, zoom);
        double minLat = tile2lat(y + 1, zoom);
        double minLon = tile2lon(x, zoom);
        double maxLon = tile2lon(x + 1, zoom);
        return new Envelope2D(DefaultGeographicCRS.WGS84, minLon, minLat, maxLon-minLon, maxLat-minLat);
    }

}

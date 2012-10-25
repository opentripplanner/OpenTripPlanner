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

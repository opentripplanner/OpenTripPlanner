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

package org.opentripplanner.graph_builder.module.ned;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A Vertical datum specified as a grid of offsets from NAD83
 * http://vdatum.noaa.gov/dev/gtx_info.html
 * 
 * @author novalis
 * 
 */
public class VerticalDatum {
    
    double lowerLeftLatitude;

    double lowerLeftLongitude;

    double deltaLatitude;

    double deltaLongitude;

    float[][] datum;

    public VerticalDatum(double lowerLeftLongitude, double lowerLeftLatitude, double width,
            double height, float[][] datum) {
        this.lowerLeftLongitude = lowerLeftLongitude; 
        this.lowerLeftLatitude = lowerLeftLatitude;
        this.deltaLongitude = width;
        this.deltaLatitude = height;
        this.datum = datum;
    }

    double interpolatedHeight(double longitude, double latitude) {
        // because VDatums can cross -180 longitude, there's some complication in interpolating
        // longitude

        double lowerLeftAdjusted = lowerLeftLongitude;
        if (lowerLeftLongitude + deltaLongitude > 180) {
            // then lowerLeftLongitude must be left of 180
            if (longitude < lowerLeftLongitude) {
                // lowerLeftLongitude is right of 180 (probablY)
                lowerLeftAdjusted -= 360; // this transforms lowerLeft so that interpolation will
                                          // work
                if (longitude < lowerLeftLongitude) {
                    throw new RuntimeException("longitude out of range");
                }
            }
        }

        if (longitude > lowerLeftAdjusted + deltaLongitude || longitude < lowerLeftAdjusted) {
            throw new RuntimeException("longitude out of range");
        }
        if (latitude < lowerLeftLatitude || latitude > lowerLeftLatitude + deltaLatitude) {
            throw new RuntimeException("latitude out of range");
        }

        double longitudeNormalized = (longitude - lowerLeftAdjusted) / deltaLongitude;
        double latitudeNormalized = (latitude - lowerLeftLatitude) / deltaLatitude;

        int rows = datum.length;
        int columns = datum[0].length;

        int x1 = (int) Math.floor(longitudeNormalized * columns);
        int y1 = (int) Math.floor(latitudeNormalized * rows);

        double gridXFraction = longitudeNormalized * columns - x1;
        double gridYFraction = latitudeNormalized * rows - y1;
        return datum[y1][x1] * gridXFraction * gridYFraction + datum[y1][x1 + 1]
                * (1 - gridXFraction) * gridYFraction + datum[y1 + 1][x1] * gridXFraction
                * (1 - gridYFraction) + datum[y1 + 1][x1 + 1] * (1 - gridXFraction)
                * (1 - gridYFraction);
    }

    boolean covers(double longitude, double latitude) {
        double lowerLeftAdjusted = lowerLeftLongitude;
        if (lowerLeftLongitude + deltaLongitude > 180) {
            // then lowerLeftLongitude must be left of 180
            if (longitude < lowerLeftLongitude) {
                // lowerLeftLongitude is right of 180 (probablY)
                lowerLeftAdjusted -= 360; // this transforms lowerLeft so that interpolation will
                                          // work
                if (longitude < lowerLeftLongitude) {
                    return false;
                }
            }
        }

        if (longitude > lowerLeftAdjusted + deltaLongitude || longitude < lowerLeftAdjusted) {
            return false;
        }
        if (latitude < lowerLeftLatitude || latitude > lowerLeftLatitude + deltaLatitude) {
            return false;
        }
        return true;
    }
        
    public static VerticalDatum fromGTX (InputStream inputStream) throws IOException {
        DataInputStream stream = new DataInputStream(new BufferedInputStream(inputStream));
        double lowerLeftLatitude = stream.readDouble();
        double lowerLeftLongitude = stream.readDouble();
        if (lowerLeftLongitude > 180) {
            lowerLeftLongitude -= 360; //convert to standard coordinates
        }
        double deltaLatitude = stream.readDouble();
        double deltaLongitude = stream.readDouble();
        int nRows = stream.readInt();
        int nColumns = stream.readInt();
        float[][] data = new float[nRows][nColumns];
        for (int y = 0; y < nRows; ++y) {
            for (int x = 0; x < nColumns; ++x) {
                data[y][x] = stream.readFloat();
            }
        }
        return new VerticalDatum(lowerLeftLongitude, lowerLeftLatitude, deltaLatitude * nColumns, deltaLongitude * nRows, data);
    }

}

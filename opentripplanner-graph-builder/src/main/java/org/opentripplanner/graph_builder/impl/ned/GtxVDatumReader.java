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

package org.opentripplanner.graph_builder.impl.ned;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class GtxVDatumReader {
    VerticalDatum read(InputStream inputStream) throws IOException {
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

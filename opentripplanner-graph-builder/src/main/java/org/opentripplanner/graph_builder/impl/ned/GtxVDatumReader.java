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

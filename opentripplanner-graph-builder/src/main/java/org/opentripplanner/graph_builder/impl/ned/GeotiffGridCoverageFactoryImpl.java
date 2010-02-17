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

import java.io.File;
import java.io.IOException;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.opentripplanner.graph_builder.services.ned.NEDGridCoverageFactory;

/**
 * Implementation of NEDGridCoverageFactory for Geotiff data (one of the default NED formats).
 * 
 * @author demory
 * 
 */

public class GeotiffGridCoverageFactoryImpl implements NEDGridCoverageFactory {

    private File path;

    public GeotiffGridCoverageFactoryImpl() {

    }

    public GeotiffGridCoverageFactoryImpl(File path) {
        this.path = path;
    }

    public void setPath(File path) {
        this.path = path;
    }

    @Override
    public GridCoverage2D getGridCoverage() {
        GeoTiffFormat format = new GeoTiffFormat();
        GeoTiffReader reader = null;
        GridCoverage2D coverage = null;

        try {
            reader = (GeoTiffReader) format.getReader(path);
            coverage = (GridCoverage2D) reader.read(null);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }

        return coverage;
    }

}
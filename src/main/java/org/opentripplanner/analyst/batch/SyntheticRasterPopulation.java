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

package org.opentripplanner.analyst.batch;

import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SyntheticRasterPopulation extends RasterPopulation {

    private static Logger LOG = LoggerFactory.getLogger(SyntheticRasterPopulation.class); 

    public String name = "synthetic grid coverage";
    public double resolutionMeters = 250; // deprecated
    public String crsCode = "EPSG:4326";
    public boolean boundsFromGraph = false; // use graph envelope, overriding any specified bounds
    
    @Override
    public void createIndividuals() {
        try {
            coverageCRS = CRS.decode(crsCode, true);
        } catch (Exception e) {
            LOG.error("error decoding coordinate reference system code.", e);
            return;
        }
        if (boundsFromGraph) {
            // autowire graph service or pass in
        }
        gridEnvelope = new GridEnvelope2D(0, 0, cols, rows);
        refEnvelope = new ReferencedEnvelope(left, right, bottom, top, coverageCRS);
        gridGeometry = new GridGeometry2D(gridEnvelope, refEnvelope);
        super.createIndividuals0();
    }

}

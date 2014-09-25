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

import static org.junit.Assert.*;

import java.io.File;
import java.nio.charset.Charset;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.io.Files;

public class CSVPopulationTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    /**
     * Test that coordinate transforms are applied correctly
     */
    @Test
    public void testCoordinateTransform() throws Exception {
        File csvFile = temporaryFolder.newFile("coordinateTransform.csv");
        // coordinates via OpenStreetMap
        Files.write("Santa Barbara Botanical Gardens,1,6046688.23,1992920.46\n",
                csvFile, Charset.forName("utf-8"));

        CSVPopulation pop = new CSVPopulation();
        pop.sourceFilename = csvFile.getAbsolutePath();
        pop.skipHeaders = false;
        pop.xCol = 2;
        pop.yCol = 3;
        pop.inputCol = 1;
        pop.labelCol = 0;
        pop.crs = "EPSG:2229"; // State Plane CA Zone 5, US Survey Feet

        pop.createIndividuals();

        Individual sbbg = pop.individuals.get(0);
        assertEquals(sbbg.lat, 34.45659, 0.00001);
        assertEquals(sbbg.lon, -119.70843, 0.00001);
    }

    /** Test that untransformed coordinate systems work */
    @Test
    public void testNoCoordinateTransform() throws Exception {
        File csvFile = temporaryFolder.newFile("noCoordinateTransform.csv");
        // coordinates via OpenStreetMap
        Files.write("Marine Science,1,-119.84330,34.40783\n",
                csvFile, Charset.forName("utf-8"));

        CSVPopulation pop = new CSVPopulation();
        pop.sourceFilename = csvFile.getAbsolutePath();
        pop.skipHeaders = false;
        pop.setLonCol(2);
        pop.setLatCol(3);
        pop.inputCol = 1;
        pop.labelCol = 0;

        pop.createIndividuals();

        Individual marsci = pop.individuals.get(0);
        assertEquals(marsci.lat, 34.40783, 0.00001);
        assertEquals(marsci.lon, -119.84330, 0.00001);

        pop = new CSVPopulation();
        pop.sourceFilename = csvFile.getAbsolutePath();
        pop.skipHeaders = false;
        pop.setLonCol(2);
        pop.setLatCol(3);
        pop.inputCol = 1;
        pop.labelCol = 0;
        pop.crs = "EPSG:4326";

        pop.createIndividuals();

        marsci = pop.individuals.get(0);
        assertEquals(marsci.lat, 34.40783, 0.00001);
        assertEquals(marsci.lon, -119.84330, 0.00001);
    }

}

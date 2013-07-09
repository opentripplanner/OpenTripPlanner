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

import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.csvreader.CsvReader;

import lombok.Setter;

public class CSVPopulation extends BasicPopulation {

    private static final Logger LOG = LoggerFactory.getLogger(BasicPopulation.class);

    @Setter
    public int latCol = 0;

    @Setter
    public int lonCol = 1;

    @Setter
    public int labelCol = 2;

    @Setter
    public int inputCol = 3;

    @Setter
    public boolean skipHeaders = true;

    @Override
    public void createIndividuals() {
        try {
            CsvReader reader = new CsvReader(sourceFilename, ',', Charset.forName("UTF8"));
            if (skipHeaders) {
                reader.readHeaders();
            }
            while (reader.readRecord()) {
                double lat = Double.parseDouble(reader.get(latCol));
                double lon = Double.parseDouble(reader.get(lonCol));
                String label = reader.get(labelCol);
                Double input = Double.parseDouble(reader.get(inputCol));
                Individual individual = new Individual(label, lon, lat, input);
                this.addIndividual(individual);
            }
            reader.close();
        } catch (Exception e) {
            LOG.error("exception while loading individuals from CSV file:");
            e.printStackTrace();
        }
    }

}

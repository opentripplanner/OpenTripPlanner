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

package org.opentripplanner.scripting.api;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.opentripplanner.analyst.batch.Individual;
import org.opentripplanner.analyst.batch.Population;

import com.csvreader.CsvReader;

/**
 * A population is a collection of individuals.
 * 
 * Example of use (python script loading a CSV file):
 * <pre>
 *   spt = ...
 *   pop = otp.loadCSVPopulation('pop.csv', 'lat', 'lon')
 *   result = spt.eval(pop)
 *   for r in result:
 *       print r.getTime()
 * </pre>
 * 
 * @author laurent
 */
public class OtpsPopulation implements Iterable<OtpsIndividual> {

    private List<OtpsIndividual> individuals;

    private Map<String, Integer> dataIndex;

    protected OtpsPopulation() {
        individuals = new ArrayList<>();
        dataIndex = null;
    }

    protected OtpsPopulation(Population population) {
        individuals = new ArrayList<>();
        for (Individual ind : population) {
            addIndividual(ind.lat, ind.lon);
        }
    }

    /**
     * Set data header keys. Only useful for population you create yourself programmatically. For
     * CSV population this will be set automatically according to the CSV headers.
     * 
     * @param headers The data header, in order. Later used as a key to retrieve individual data
     *        value.
     */
    public void setHeaders(String[] headers) {
        dataIndex = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            dataIndex.put(headers[i], i);
        }
    }

    protected int getDataIndex(String dataName) {
        if (dataIndex == null)
            return -1;
        Integer ret = dataIndex.get(dataName);
        if (ret == null)
            return -1;
        return ret;
    }

    /**
     * Add a new data-less individual to the collection.
     * 
     * @param lat Latitude of the individual location
     * @param lon Longitude of the individual location
     */
    public void addIndividual(double lat, double lon) {
        this.addIndividual(lat, lon, null);
    }

    /**
     * Add a new individual with some data attached to it.
     * 
     * @param lat Latitude of the individual location
     * @param lon Longitude of the individual location
     * @param data An array of data to store in the individual
     */
    public void addIndividual(double lat, double lon, String[] data) {
        OtpsIndividual individual = new OtpsIndividual(lat, lon, data, this);
        individuals.add(individual);
    }

    @Override
    public Iterator<OtpsIndividual> iterator() {
        // This seems to work. What the use for Guava ForwardingIterator then?
        return individuals.iterator();
    }

    // TODO Specify the CRS to use
    // TODO Use a Map<String, Object> for optional parameters?
    protected static OtpsPopulation loadFromCSV(String filename, String latColName,
            String lonColName) throws IOException {
        OtpsPopulation ret = new OtpsPopulation();
        CsvReader reader = new CsvReader(filename, ',', Charset.forName("UTF8"));
        reader.readHeaders();

        // Read headers
        List<String> dataHeaders = new ArrayList<>(reader.getHeaderCount());
        for (String header : reader.getHeaders()) {
            if (header.equals(latColName) || header.equals(lonColName))
                continue;
            dataHeaders.add(header);
        }
        ret.setHeaders(dataHeaders.toArray(new String[dataHeaders.size()]));

        // Read records
        while (reader.readRecord()) {
            double lat = Double.parseDouble(reader.get(latColName));
            double lon = Double.parseDouble(reader.get(lonColName));
            List<String> data = new ArrayList<String>();
            for (String header : dataHeaders) {
                data.add(reader.get(header));
            }
            OtpsIndividual individual = new OtpsIndividual(lat, lon, data.toArray(new String[data
                    .size()]), ret);
            ret.individuals.add(individual);
        }
        reader.close();
        return ret;
    }
}

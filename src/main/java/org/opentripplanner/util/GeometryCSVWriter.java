/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.*/
package org.opentripplanner.util;

import com.csvreader.CsvWriter;
import com.google.common.annotations.VisibleForTesting;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opentripplanner.routing.impl.CandidateEdge;
import org.opentripplanner.routing.impl.CandidateEdgeBundle;

/**
 * This class is used primary for debugging puproses.
 *
 * It makes possible to write to CSV files some information together with WKT
 * serialized geometries. Those files are very easy to open and visualize with
 * QGIS.
 *
 * @author mabu
 */
public class GeometryCSVWriter {

    /* Two dimensional WKT serializer   */
    private static final WKTWriter wktWriter = new WKTWriter(2);

    private List<String> csvColumns;
    private Integer geoFieldIndex;
    private CsvWriter csvWriter;
    @VisibleForTesting
    Writer testWriter = null;

    /**
     * Finds geo field index and writes header into CSV file
     *
     * With help of {@link #constructor(java.util.List, String, String)}
     *
     * @param csvColumns Name of csv columns together with geo field name
     * @param geo_field Name of geo field
     * @param file Path to csv file where this will be saved
     */
    public GeometryCSVWriter(List<String> csvColumns, String geo_field, String file) {
        constructor(csvColumns, geo_field, file);
    }

    /**
     * Initializes writer, finds out index of geometry and writes header
     *
     * It writes to file, or to testWriter if it isn't null
     *
     * @param csvColumns Name of csv columns together with geo field name
     * @param geo_field_name Name of geo field
     * @param file Path to csv file where this will be saved
     */
    private void constructor(List<String> csvColumns, String geo_field_name, String file) {
        int index = 0;
        for (String key : csvColumns) {
            if (key.equals(geo_field_name)) {
                geoFieldIndex = index;
            }
            index++;
        }
        this.csvColumns = csvColumns;

        if (testWriter != null) {
            csvWriter = new CsvWriter(testWriter, ',');
        } else {
            csvWriter = new CsvWriter(file, ',', Charset.forName("UTF8"));
        }
        try {
            csvWriter.writeRecord(csvColumns.toArray(new String[csvColumns.size()]));
        } catch (IOException ex) {
            Logger.getLogger(GeometryCSVWriter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Finds geo field index and writes header into writer
     *
     * With help of {@link #constructor(java.util.List, String, String)}
     *
     * @param csvColumns Name of csv columns together with geo field name
     * @param geo_field Name of geo field
     * @param test_writer Writer which is used for writing csv file (Usually
     * used only in testing)
     */
    @VisibleForTesting
    public GeometryCSVWriter(List<String> csvColumns, String geo_field, Writer test_writer) {
        this.testWriter = test_writer;
        constructor(csvColumns, geo_field, "");
    }

    /**
     * Writes a line into CSV file
     *
     * First values up to geo field are written, then geo field then other
     * values.
     *
     * @param values of csv columns in same order as in constructor with skipped
     * geo field
     * @param geo Geometry data
     */
    public void add(List<String> values, Geometry geo) {
        String geometry = wktWriter.write(geo);
        String[] csv_values = new String[csvColumns.size()];
        for (int cur_index = 0; cur_index < geoFieldIndex; cur_index++) {
            csv_values[cur_index] = values.get(cur_index);
        }
        csv_values[geoFieldIndex] = geometry;

        //Geometry isn't in last column
        if ((csvColumns.size() - 1) != geoFieldIndex) {
            for (int cur_index = geoFieldIndex; cur_index < values.size(); cur_index++) {
                csv_values[cur_index + 1] = values.get(cur_index);
            }
        }

        try {
            csvWriter.writeRecord(csv_values);
        } catch (IOException ex) {
            Logger.getLogger(GeometryCSVWriter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Writes CSV with edges in bundle information.
     *
     * It is useful to see which edges are in which bundles.
     *
     * For each edge in bundle:
     * - edge name
     * - bundle index
     * - index of edge in bundle
     * - distance from stop to edge
     * - {@link org.opentripplanner.routing.impl.CandidateEdge} score
     * - {@link org.opentripplanner.routing.impl.CandidateEdge} endwise (1 - true, 0 - false)
     * - {@link org.opentripplanner.routing.impl.CandidateEdge} geometry (WKT)
     *
     * @param bundles Collection of {@link CandidateEdgeBundle} to be written
     * @param filename of csv file
     */
    public static void writeCandidateBundle(Collection <CandidateEdgeBundle> bundles,
            String filename) {
        GeometryCSVWriter myWriter = new GeometryCSVWriter(
                Arrays.asList("edge_name", "bundle_index", "index", "distance", "score",
                        "endwise", "geo"), "geo", filename);
        int bundle_index=0;
        for (CandidateEdgeBundle bundle: bundles) {
            int index = 0;
            for (CandidateEdge ce : bundle) {
                String endwise;
                if (ce.endwise()) {
                    endwise = "1";
                } else {
                    endwise = "0";
                }
                myWriter.add(Arrays.asList(ce.edge.getName(),
                        Integer.toString(bundle_index), Integer.toString(index),
                        Double.toString(ce.distance), Double.toString(ce.score),
                        endwise), ce.edge.getGeometry());
                index++;
            }
            bundle_index++;
        }
        myWriter.close();
    }

    /**
     * Closes writer
     */
    public void close() {
        csvWriter.close();
    }
}

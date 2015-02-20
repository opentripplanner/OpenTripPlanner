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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.csvreader.CsvWriter;
import com.google.common.base.Charsets;

/**
 * This class allow one to generate easily tabular data and save it as a CSV file.
 * 
 * For example, in python:
 * <pre>
 *   csv = otp.createCSVOutput()
 *   csv.setHeader( [ 'lat', 'lon', 'total' ] )
 *   csv.addRow( [ 45.123, 5.789, 42 ] )
 *   csv.addRow( [ 45.124, 5.792, 34 ] )
 *   csv.save('mydata.csv')
 * </pre>
 * 
 * TODO Rename this class to "TabularOutput" and allow for saving in various format.
 * 
 * @author laurent
 */
public class OtpsCsvOutput {

    private List<List<String>> data = new ArrayList<>();

    private String[] headers;

    protected OtpsCsvOutput() {
    }

    /**
     * Set the (optional) column header names. If this is not set, no header will be generated.
     * 
     * @param headers An array of string, each entry is the name of the corresponding column header,
     *        in order.
     */
    public void setHeader(Object[] headers) {
        this.headers = new String[headers.length];
        for (int i = 0; i < headers.length; i++) {
            this.headers[i] = headers[i].toString();
        }
    }

    /**
     * Add a new row to the data.
     * 
     * @param row An array of objects. The order and size of the array should correspond to the
     *        header. The default toString method of each object will be called to get the actual
     *        data to output; so any type of object can be provided (string, numbers...)
     */
    public void addRow(Object[] row) {
        List<String> strs = new ArrayList<>(row.length);
        for (int i = 0; i < row.length; i++) {
            strs.add(row[i] == null ? "" : row[i].toString());
        }
        data.add(strs);
    }

    /**
     * Save the data to a file.
     * @param file The name of the file to save the data to.
     * @throws IOException In case something bad happens (IO exception)
     */
    public void save(String file) throws IOException {
        CsvWriter csvWriter = new CsvWriter(file, ',', Charsets.UTF_8);
        if (headers != null)
            csvWriter.writeRecord(headers);
        for (List<String> row : data) {
            csvWriter.writeRecord(row.toArray(new String[row.size()]));
        }
        csvWriter.close();
    }

    /**
     * @return The CSV data as a string. It can be used for example as the script return value.
     * @see OtpsEntryPoint.setRetval()
     * @throws IOException
     */
    public String asText() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CsvWriter csvWriter = new CsvWriter(baos, ',', Charsets.UTF_8);
        if (headers != null)
            csvWriter.writeRecord(headers);
        for (List<String> row : data) {
            csvWriter.writeRecord(row.toArray(new String[row.size()]));
        }
        csvWriter.close();
        return new String(baos.toByteArray(), Charsets.UTF_8);
    }

}

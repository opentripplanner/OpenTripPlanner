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
 * 
 */
public class OtpsCsvOutput {

    private List<List<String>> data = new ArrayList<>();

    private String[] headers;

    protected OtpsCsvOutput() {
    }

    public void setHeader(Object[] headers) {
        this.headers = new String[headers.length];
        for (int i = 0; i < headers.length; i++) {
            this.headers[i] = headers[i].toString();
        }
    }

    public void addRow(Object[] row) {
        List<String> strs = new ArrayList<>(row.length);
        for (int i = 0; i < row.length; i++) {
            strs.add(row[i] == null ? "" : row[i].toString());
        }
        data.add(strs);
    }

    public void save(String file) throws IOException {
        CsvWriter csvWriter = new CsvWriter(file, ',', Charsets.UTF_8);
        if (headers != null)
            csvWriter.writeRecord(headers);
        for (List<String> row : data) {
            csvWriter.writeRecord(row.toArray(new String[row.size()]));
        }
        csvWriter.close();
    }

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

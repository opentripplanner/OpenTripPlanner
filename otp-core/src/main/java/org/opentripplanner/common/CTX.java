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

package org.opentripplanner.common;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * CTX parser for receiving Dutch realtime updates
 * 
 * @author skywave
 */
public class CTX {

    public ArrayList<HashMap<String, String>> rows = new ArrayList<HashMap<String, String>>();

    public String subscription;

    public String timestamp;

    public String type;

    public CTX(String ctx) {
        String[] lines = ctx.split("\r\n");
        String[] labels = null;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.length() > 0) {
                if (line.charAt(0) == '\\') {
                    if (line.charAt(1) == 'G') {
                        String[] header = line.split("\\|");
                        subscription = header[2];
                    } else if (line.charAt(1) == 'T') {
                        String[] table = line.split("\\|");
                        type = table[1];
                    } else if (line.charAt(1) == 'L') {
                        labels = line.substring(2).split("\\|");
                    }
                } else {
                    HashMap<String, String> row = new HashMap<String, String>();
                    String[] values = line.split("\\|");
                    for (int j = 0; j < values.length; j++) {
                        if (values[j] == "\0") {
                            values[j] = null;
                        }
                        row.put(labels[j], values[j]);
                    }
                    rows.add(row);
                }
            }
        }
    }
}

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

package org.opentripplanner.gtfs;

import com.csvreader.CsvWriter;
import org.opentripplanner.gtfs.format.Feed;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class RoundTrip {
    final static private Charset UTF8 = Charset.forName("UTF-8");

    public static void main (String[] args) {
        if (args.length < 1) {
            System.err.println("Please specify a GTFS feed input file for parsing and extraction.");
            System.exit(1);
        }

        try (Feed feed = new Feed(args[0])) {
            for (Entry<String, Iterable<Map<String, String>>> entry : feed.entrySet()) {
                CsvWriter csvWriter = new CsvWriter(entry.getKey(), ',', UTF8);
                Iterable<Map<String, String>> iterable = entry.getValue();
                Set<String> set = iterable.iterator().next().keySet();

                try {
                    csvWriter.writeRecord(set.toArray(new String[set.size()]));

                    for (Map<String, String> map : iterable) {
                        Collection<String> collection = map.values();
                        csvWriter.writeRecord(collection.toArray(new String[collection.size()]));
                    }
                } catch (Exception e) {
                    e.printStackTrace();

                    System.exit(2);
                } finally {
                    csvWriter.close();
                }
            }
        }
    }
}

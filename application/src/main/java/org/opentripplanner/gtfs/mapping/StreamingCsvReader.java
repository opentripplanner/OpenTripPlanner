/**
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org> Copyright (C) 2012 Google, Inc.
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opentripplanner.gtfs.mapping;

import java.io.*;
import java.util.*;
import java.util.stream.Stream;
import org.onebusaway.csv_entities.DelimitedTextParser;

public class StreamingCsvReader {

  public Stream<Map<String, String>> read(Reader reader) throws IOException {

    BufferedReader lineReader = new BufferedReader(reader);
    // Skip the initial UTF BOM, if present
    lineReader.mark(1);
    int c = lineReader.read();

    if (c != 0xFEFF) {
      lineReader.reset();
    }
    var fields = DelimitedTextParser.parse(lineReader.readLine()).stream().toList();

    return lineReader
        .lines()
        .map(
            line -> {
              var elements = DelimitedTextParser.parse(line);
              var values = new HashMap<String, String>();

              for (int i = 0; i < fields.size(); i++) {
                String csvFieldName = fields.get(i);
                String value = elements.get(i);
                values.put(csvFieldName, value);
              }

              return values;
            });
  }
}

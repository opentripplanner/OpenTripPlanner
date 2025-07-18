package org.opentripplanner.gtfs.mapping;

import java.io.*;
import java.util.*;
import java.util.stream.Stream;
import org.onebusaway.csv_entities.CsvInputSource;
import org.onebusaway.csv_entities.DelimitedTextParser;
import org.opentripplanner.utils.lang.StringUtils;

public class StreamingCsvReader {

  private final CsvInputSource inputSource;

  public StreamingCsvReader(CsvInputSource inputSource) {
    this.inputSource = Objects.requireNonNull(inputSource);
  }

  public Stream<GtfsRow> rows(String fileName) throws IOException {
    if (inputSource.hasResource(fileName)) {
      return stream(fileName);
    } else {
      return Stream.empty();
    }
  }

  private Stream<GtfsRow> stream(String fileName) throws IOException {
    var source = inputSource.getResource(fileName);
    var streamReader = new InputStreamReader(source);
    BufferedReader lineReader = new BufferedReader(streamReader);

    // Skip the initial UTF BOM, if present
    lineReader.mark(1);
    int c = lineReader.read();

    if (c != 0xFEFF) {
      lineReader.reset();
    }
    var fields = DelimitedTextParser.parse(lineReader.readLine()).stream().toList();

    return lineReader
      .lines()
      .filter(StringUtils::hasValue)
      .map(line -> {
        var elements = DelimitedTextParser.parse(line);
        var values = new HashMap<String, String>(fields.size());

        for (int i = 0; i < fields.size() && i < elements.size(); i++) {
          var fieldName = fields.get(i);
          var value = elements.get(i);
          if (StringUtils.hasValue(value)) {
            values.put(fieldName, value);
          }
        }

        return new GtfsRow(values);
      });
  }
}

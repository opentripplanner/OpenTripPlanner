package org.opentripplanner.gtfs.mapping;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.onebusaway.csv_entities.CsvInputSource;

record TestCsvSource(String csv) implements CsvInputSource {
  @Override
  public boolean hasResource(String name) {
    return true;
  }

  @Override
  public InputStream getResource(String name) {
    return new ByteArrayInputStream(csv.stripIndent().getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public void close() {}
}

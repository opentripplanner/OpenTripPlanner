package org.opentripplanner.datastore.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.datastore.api.FileType.GTFS;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.datastore.api.DataSource;

class ZipStreamDataSourceDecoratorTest {

  private static final long TIME = 30 * 365 * 24 * 60 * 60 * 1000L;
  static final List<String> EXPECTED_ZIP_ENTRIES = List.of(
    "trips.txt",
    "agency.txt",
    "calendar.txt",
    "calendar_dates.txt",
    "fare_attributes.txt",
    "fare_rules.txt",
    "routes.txt",
    "shapes.txt",
    "stop_times.txt",
    "stops.txt"
  );

  static final Map<String, Long> EXPECTED_FILE_SIZES = Map.of(
    "trips.txt",
    19406L,
    "agency.txt",
    113L,
    "calendar.txt",
    351L,
    "calendar_dates.txt",
    170L,
    "fare_attributes.txt",
    199L,
    "fare_rules.txt",
    487L,
    "routes.txt",
    228L,
    "shapes.txt",
    145880L,
    "stop_times.txt",
    269891L,
    "stops.txt",
    3141L
  );

  @Test
  void testAccessorsForNoneExistingFile() throws IOException {
    // Given:
    File target = ConstantsForTests.CALTRAIN_GTFS;
    File copyTarget = ConstantsForTests.CALTRAIN_GTFS;
    CompositeDataSource subject = new ZipStreamDataSourceDecorator(
      new FileDataSource(target, GTFS)
    );
    CompositeDataSource copySubject = new ZipStreamDataSourceDecorator(
      new FileDataSource(copyTarget, GTFS)
    );
    String expectedPath = target.getPath();

    // Verify zip file exist before we start the test
    assertTrue(target.exists(), target.getAbsolutePath());

    // Then
    assertEquals("caltrain_gtfs.zip", subject.name());
    assertEquals(expectedPath, subject.path());
    assertEquals(GTFS, subject.type());
    assertTrue(subject.lastModified() > TIME, "Last modified: " + subject.lastModified());
    assertTrue(subject.size() > 100, "Size: " + subject.size());
    assertTrue(subject.exists());
    // We do not support writing to zip files
    assertFalse(subject.isWritable());

    assertEquals(expectedPath, subject.toString());

    subject.close();
    copySubject.close();
  }

  @Test
  void testIO() throws IOException {
    // Given:
    File target = ConstantsForTests.CALTRAIN_GTFS;
    CompositeDataSource subject = new ZipStreamDataSourceDecorator(
      new FileDataSource(target, GTFS)
    );

    Collection<DataSource> content = subject.content();
    Collection<String> names = content.stream().map(DataSource::name).toList();

    System.out.println(names);
    assertTrue(names.containsAll(EXPECTED_ZIP_ENTRIES), names.toString());

    DataSource entry = subject.entry("agency.txt");

    List<String> lines = new BufferedReader(
      new InputStreamReader(entry.asInputStream(), StandardCharsets.UTF_8)
    )
      .lines()
      .toList();
    assertEquals("agency_id,agency_name,agency_url,agency_timezone", lines.get(0));

    // Close zip
    subject.close();
  }

  @Test
  void testMaxZipEntrySizeInMemory() throws IOException {
    File target = ConstantsForTests.CALTRAIN_GTFS;
    // force offloading to disk by setting maxZipEntrySizeInMemory=1
    CompositeDataSource subject = new ZipStreamDataSourceDecorator(
      new FileDataSource(target, GTFS),
      1
    );
    Collection<DataSource> content = subject.content();
    Collection<String> names = content.stream().map(DataSource::name).toList();
    assertTrue(names.containsAll(EXPECTED_ZIP_ENTRIES));
    assertTrue(
      content
        .stream()
        .allMatch(dataSource -> EXPECTED_FILE_SIZES.get(dataSource.name()) == dataSource.size())
    );
    assertTrue(content.stream().allMatch(TemporaryFileDataSource.class::isInstance));
    assertTrue(content.stream().allMatch(DataSource::exists));
    subject.close();
    assertTrue(content.stream().noneMatch(DataSource::exists));
  }

  @Test
  void testEntryProperties() {
    // Given:
    File target = ConstantsForTests.CALTRAIN_GTFS;
    CompositeDataSource subject = new ZipStreamDataSourceDecorator(
      new FileDataSource(target, GTFS)
    );
    DataSource entry = subject.entry("trips.txt");

    assertEquals("trips.txt", entry.name());
    assertEquals("trips.txt (" + subject.path() + ")", entry.path());
    assertEquals(GTFS, entry.type());
    assertTrue(entry.lastModified() > TIME, "Last modified: " + entry.lastModified());
    assertTrue(entry.size() > 100, "Size: " + entry.size());
    assertTrue(entry.exists());
    // We do not support writing to zip entries
    assertFalse(entry.isWritable());
  }
}

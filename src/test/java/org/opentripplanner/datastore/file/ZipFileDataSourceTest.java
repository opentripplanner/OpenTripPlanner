package org.opentripplanner.datastore.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.datastore.api.FileType.GTFS;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.datastore.api.DataSource;

public class ZipFileDataSourceTest {

  // Sometime close to 2000-01-01
  private static final long TIME = 30 * 365 * 24 * 60 * 60 * 1000L;
  private static final String FILENAME = ConstantsForTests.CALTRAIN_GTFS;

  @Test
  public void testAccessorsForNoneExistingFile() throws IOException {
    // Given:
    File target = new File(FILENAME);
    File copyTarget = new File(FILENAME);
    CompositeDataSource subject = new ZipFileDataSource(target, GTFS);
    CompositeDataSource copySubject = new ZipFileDataSource(copyTarget, GTFS);
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

    assertEquals("GTFS " + expectedPath, subject.toString());
    assertEquals(copySubject, subject);
    assertEquals(copySubject.hashCode(), subject.hashCode());

    subject.close();
    copySubject.close();
  }

  @Test
  public void testIO() throws IOException {
    // Given:
    File target = new File(FILENAME);
    CompositeDataSource subject = new ZipFileDataSource(target, GTFS);

    Collection<DataSource> content = subject.content();
    Collection<String> names = content.stream().map(DataSource::name).toList();

    //System.out.println(names);
    assertTrue(
      names.containsAll(List.of("agency.txt", "stops.txt", "trips.txt")),
      names.toString()
    );

    DataSource entry = subject.entry("agency.txt");

    List<String> lines = IOUtils.readLines(entry.asInputStream(), StandardCharsets.UTF_8);
    assertEquals("agency_id,agency_name,agency_url,agency_timezone", lines.get(0));
    assertEquals("Caltrain,Caltrain,http://www.caltrain.com,America/Los_Angeles", lines.get(1));

    // Close zip
    subject.close();
  }

  @Test
  public void testEntryProperties() {
    // Given:
    File target = new File(FILENAME);
    CompositeDataSource subject = new ZipFileDataSource(target, GTFS);
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

  @Test
  public void testUnsupportedDelete() {
    // Given:
    File target = new File(FILENAME);
    CompositeDataSource subject = new ZipFileDataSource(target, GTFS);

    // When: delete entry is not implemented
    assertThrows(Exception.class, subject::delete, "ZipFileDataSource");
  }
}

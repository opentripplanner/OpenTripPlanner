package org.opentripplanner.datastore.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.datastore.api.FileType.GTFS;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.FileTime;
import java.util.Collection;
import java.util.List;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
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

  @Test
  public void testEntryEncoding() {
    // has worked before #4835, for verification remove the attempt to set to code page to cp437
    File target = new File(ConstantsForTests.UMLAUT_UTF8_ZIP);
    CompositeDataSource subject = new ZipFileDataSource(target, GTFS);
    DataSource entry = subject.content().iterator().next();

    assertEquals(ConstantsForTests.UMLAUT_TXT, entry.name());

    // has worked before #4835, for verification remove the attempt to set to code page to cp437
    target = new File(ConstantsForTests.UMLAUT_UTF8_ZIP_NO_EFS);
    subject = new ZipFileDataSource(target, GTFS);
    entry = subject.content().iterator().next();

    assertEquals(ConstantsForTests.UMLAUT_TXT, entry.name());

    // only works after #4835, will fail with "Invalid CEN header (bad entry name)" when verifying
    target = new File(ConstantsForTests.UMLAUT_CP437_ZIP);
    subject = new ZipFileDataSource(target, GTFS);
    entry = subject.content().iterator().next();

    assertEquals(ConstantsForTests.UMLAUT_TXT, entry.name());
  }

  /*
   * generate test files
   *
   * mvn exec:java -D"exec.mainClass"="org.opentripplanner.datastore.file.ZipFileDataSourceTest" -D"exec.classpathScope"=test
   */
  public static void main(String[] args) throws FileNotFoundException, IOException {
    /* cp437 encoded file names in zip */
    final ZipArchiveOutputStream zos = new ZipArchiveOutputStream(
      new FileOutputStream(ConstantsForTests.UMLAUT_CP437_ZIP)
    );
    /* set original ZIP character encoding aka OEM-US or DOS-US */
    zos.setEncoding("Cp437");

    final byte[] data = {};

    ZipArchiveEntry entry = new ZipArchiveEntry(ConstantsForTests.UMLAUT_TXT);
    entry.setSize(data.length);
    entry.setTime(FileTime.fromMillis(0));
    zos.putArchiveEntry(entry);
    zos.write(data);
    zos.closeArchiveEntry();

    zos.close();

    /* utf-8 encoded file names in zip */
    final ZipArchiveOutputStream zos2 = new ZipArchiveOutputStream(
      new FileOutputStream(ConstantsForTests.UMLAUT_UTF8_ZIP)
    );
    /* explicitely set Apache Commons default for documentation */
    zos2.setEncoding("utf-8");

    ZipArchiveEntry entry2 = new ZipArchiveEntry(ConstantsForTests.UMLAUT_TXT);
    entry2.setSize(data.length);
    entry2.setTime(FileTime.fromMillis(0));
    zos2.putArchiveEntry(entry2);
    zos2.write(data);
    zos2.closeArchiveEntry();

    zos2.close();

    /*
     * utf-8 encoded file names in zip, this time without EFS flag
     * e.g. Java pre 7b57
     * http://web.archive.org/web/20150718122844/https://blogs.oracle.com/xuemingshen/entry/non_utf_8_encoding_in
     */
    final ZipArchiveOutputStream zos3 = new ZipArchiveOutputStream(
      new FileOutputStream(ConstantsForTests.UMLAUT_UTF8_ZIP_NO_EFS)
    );
    /* explicitely set Apache Commons default for documentation */
    zos3.setEncoding("utf-8");
    /* no EFS flag! */
    zos3.setUseLanguageEncodingFlag(false);

    ZipArchiveEntry entry3 = new ZipArchiveEntry(ConstantsForTests.UMLAUT_TXT);
    entry3.setSize(data.length);
    entry3.setTime(FileTime.fromMillis(0));
    zos3.putArchiveEntry(entry3);
    zos3.write(data);
    zos3.closeArchiveEntry();

    zos3.close();
  }
}

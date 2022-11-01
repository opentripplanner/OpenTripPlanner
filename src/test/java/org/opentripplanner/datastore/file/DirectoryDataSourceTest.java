package org.opentripplanner.datastore.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.datastore.api.FileType.REPORT;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.datastore.api.DataSource;

public class DirectoryDataSourceTest {

  private static final String DIRNAME = "the_wall";
  private static final String UTF_8 = "UTF-8";
  private File tempDir;

  @BeforeEach
  public void setUp() throws IOException {
    tempDir = Files.createTempDirectory("OtpDataStoreTest-").toFile();
  }

  @AfterEach
  @SuppressWarnings("ResultOfMethodCallIgnored")
  public void tearDown() {
    tempDir.delete();
  }

  @Test
  public void testAccessorsForNoneExistingDirectory() throws IOException {
    File target = new File(tempDir, DIRNAME);
    File copyTarget = new File(tempDir, DIRNAME);
    CompositeDataSource subject = new DirectoryDataSource(target, REPORT);
    CompositeDataSource copySubject = new DirectoryDataSource(copyTarget, REPORT);
    String expectedPath = new File(tempDir, DIRNAME).getPath();

    assertTrue(subject.content().isEmpty(), subject.content().toString());
    assertEquals(DIRNAME, subject.name());
    assertEquals(expectedPath, subject.path());
    assertEquals(REPORT, subject.type());
    assertEquals(DataSource.UNKNOWN, subject.lastModified());
    assertEquals(DataSource.UNKNOWN, subject.size());
    assertFalse(subject.exists());
    assertTrue(subject.isWritable());

    assertEquals("REPORT " + expectedPath, subject.toString());
    assertEquals(copySubject, subject);
    assertEquals(copySubject.hashCode(), subject.hashCode());

    subject.close();
  }

  @Test
  public void testIO() throws IOException {
    // Given a file, yet not created
    File target = new File(tempDir, DIRNAME);
    File child = new File(target, "a.txt");

    // And then CREATE a file data source - our subject
    CompositeDataSource subject = new DirectoryDataSource(target, REPORT);

    // Verify content is empty
    assertEquals("[]", toString(subject.content()));

    // Write something to a file in the directory
    try (var outputStream = subject.entry(child.getName()).asOutputStream()) {
      IOUtils.write("Go, go, go!", outputStream, UTF_8);
    }

    // Verify the subject directory is created and still is writable
    assertTrue(subject.exists());
    assertTrue(subject.isWritable());

    // Verify content is updated
    assertEquals("[a.txt]", toString(subject.content()));

    // Assert content can be read using the subject and file
    assertEquals("Go, go, go!", FileUtils.readFileToString(child, UTF_8));

    // Then delete subject with all content
    subject.delete();

    // Assert dir is still writable, none existing and content is gone
    assertTrue(subject.isWritable());
    assertFalse(subject.exists());
    assertFalse(target.exists());
    assertEquals("[]", toString(subject.content()));

    subject.close();
  }

  private String toString(Collection<DataSource> sources) {
    return sources.stream().map(DataSource::name).sorted().toList().toString();
  }
}

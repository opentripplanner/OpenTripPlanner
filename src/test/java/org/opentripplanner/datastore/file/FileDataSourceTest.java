package org.opentripplanner.datastore.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opentripplanner.datastore.api.FileType.GRAPH;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.datastore.api.DataSource;

public class FileDataSourceTest {

  private static final String FILENAME = "a.obj";
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
  public void testAccessorsForNoneExistingFile() {
    File target = new File(tempDir, FILENAME);
    File copyTarget = new File(tempDir, FILENAME);
    FileDataSource subject = new FileDataSource(target, GRAPH);
    FileDataSource copySubject = new FileDataSource(copyTarget, GRAPH);
    String expectedPath = new File(tempDir, FILENAME).getPath();

    assertEquals(FILENAME, subject.name());
    assertEquals(expectedPath, subject.path());
    assertEquals(GRAPH, subject.type());
    assertEquals(DataSource.UNKNOWN, subject.lastModified());
    assertEquals(DataSource.UNKNOWN, subject.size());
    assertFalse(subject.exists());
    assertTrue(subject.isWritable());

    assertEquals("GRAPH " + expectedPath, subject.toString());
    assertEquals(copySubject, subject);
    assertEquals(copySubject.hashCode(), subject.hashCode());
  }

  @Test
  public void testIO() throws IOException {
    // Given a file, yet not created
    File target = new File(tempDir, FILENAME);

    // And then CREATE a file data source - our subject
    FileDataSource subject = new FileDataSource(target, GRAPH);

    // Write to file
    FileUtils.write(target, "Hello!", UTF_8);

    // Verify the subject exist - and that the exist status is not cached
    assertTrue(subject.exists());

    // Verify the content by reading the file using the subject input stream
    assertEquals("Hello!", IOUtils.toString(subject.asInputStream(), StandardCharsets.UTF_8));

    // Then write something else - replacing the existing content
    IOUtils.write("Go, go, go!", subject.asOutputStream(), UTF_8);

    // Assert content can be read using the subject and file
    assertEquals("Go, go, go!", IOUtils.toString(subject.asInputStream(), StandardCharsets.UTF_8));
  }

  @Test
  public void verifyReadingNoneExistingFileFails() {
    // given
    FileDataSource subject = new FileDataSource(new File(tempDir, FILENAME), GRAPH);
    try {
      // When
      subject.asInputStream();
      // Then
      fail("The creation of the new file did not fail as expected!");
    } catch (Exception e) {
      assertTrue(e.getMessage().contains(FILENAME));
    }
  }

  @Test
  public void verifyWritingToAExistingDirectoryFails() {
    // given
    File file = new File(tempDir, "a_child");
    FileDataSource subject = new FileDataSource(file, GRAPH);

    // Make file a directory
    file.mkdirs();

    try {
      // When
      subject.asOutputStream();
      // Then
      fail("Did not expect to be able to open a directory for reading!");
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("a_child"));
    }
  }
}

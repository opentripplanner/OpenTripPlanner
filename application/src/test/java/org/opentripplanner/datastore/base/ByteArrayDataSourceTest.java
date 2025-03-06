package org.opentripplanner.datastore.base;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import org.junit.jupiter.api.Test;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.datastore.api.FileType;

public class ByteArrayDataSourceTest {

  private static final String PATH = "file:///a/b/c/name";
  private static final String NAME = "name";
  private static final FileType TYPE = FileType.UNKNOWN;
  private static final int SIZE = 4;
  private static final long LAST_MODIFIED = System.currentTimeMillis();
  private static final String DATA = "Hello";
  private static final byte[] BYTES = DATA.getBytes(UTF_8);

  @Test
  public void testAccessors() {
    DataSource subject = new ByteArrayDataSource(PATH, NAME, TYPE, SIZE, LAST_MODIFIED, true);

    assertEquals(PATH, subject.path());
    assertEquals(NAME, subject.name());
    assertEquals(TYPE, subject.type());
    assertTrue(subject.isWritable());
    assertTrue(subject.exists());
    assertEquals(SIZE, subject.size());
    assertEquals(LAST_MODIFIED, subject.lastModified());
    assertEquals(TYPE + " " + PATH, subject.toString());
  }

  @Test
  public void asOutputStream() throws IOException {
    DataSource subject = new ByteArrayDataSource(PATH, NAME, TYPE, SIZE, LAST_MODIFIED, true);

    try (OutputStream out = subject.asOutputStream()) {
      out.write(BYTES);
    }

    assertArrayEquals(BYTES, subject.asBytes());
  }

  @Test
  public void asInputStream() throws IOException {
    DataSource subject = new ByteArrayDataSource(
      PATH,
      NAME,
      TYPE,
      SIZE,
      LAST_MODIFIED,
      false
    ).withBytes(BYTES);

    assertEquals(BYTES, subject.asBytes());
    assertEquals(DATA, new String(subject.asInputStream().readAllBytes(), UTF_8));
  }

  @Test
  public void verifyAReadOnlyInstanceIsNotWritable() {
    DataSource subject = new ByteArrayDataSource(
      PATH,
      NAME,
      TYPE,
      SIZE,
      LAST_MODIFIED,
      false
    ).withBytes(BYTES);

    assertFalse(subject.isWritable());
    assertThrows(UnsupportedOperationException.class, subject::asOutputStream);
  }
}

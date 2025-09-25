package org.opentripplanner.datastore.base;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.datastore.api.FileType;
import org.opentripplanner.datastore.configure.DataStoreModule;

class ListCompositeDataSourceTest {

  private static final String LEAF_NAME = "leaf";
  private static final DataSource LEAF = DataStoreModule.dataSource(
    LEAF_NAME,
    FileType.GTFS,
    "Test"
  );

  private final CompositeDataSource subjectEmpty = new ListCompositeDataSource(
    "empty",
    FileType.GTFS,
    List.of()
  );
  private final CompositeDataSource subjectOne = new ListCompositeDataSource(
    "one",
    FileType.GTFS,
    List.of(LEAF)
  );

  @Test
  void content() {
    assertEquals(List.of(), subjectEmpty.content());
    assertEquals(List.of(LEAF), subjectOne.content());
  }

  @Test
  void entry() {
    assertEquals(null, subjectEmpty.entry(LEAF_NAME));
    assertEquals(LEAF, subjectOne.entry(LEAF_NAME));
  }

  @Test
  void close() throws IOException {
    // Nothing happens (exceptions not thrown)
    subjectEmpty.close();
    subjectOne.close();
  }

  @Test
  void name() {
    assertEquals("empty", subjectEmpty.name());
    assertEquals("one", subjectOne.name());
  }

  @Test
  void path() {
    assertEquals("ListCompositeDataSource/NNN", replaceHash(subjectEmpty.path()));
    assertEquals("ListCompositeDataSource/NNN", replaceHash(subjectOne.path()));
  }

  @Test
  void uri() {
    assertEquals("ListCompositeDataSource/NNN/empty", replaceHash(subjectEmpty.uri().toString()));
    assertEquals("ListCompositeDataSource/NNN/one", replaceHash(subjectOne.uri().toString()));
  }

  @Test
  void type() {
    assertEquals(FileType.GTFS, subjectEmpty.type());
    assertEquals(FileType.GTFS, subjectOne.type());
  }

  private String replaceHash(String name) {
    return name.replaceAll("\\d{6,}", "NNN");
  }
}

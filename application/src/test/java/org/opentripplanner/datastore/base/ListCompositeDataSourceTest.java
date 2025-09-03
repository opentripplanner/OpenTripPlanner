package org.opentripplanner.datastore.base;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.datastore.api.FileType;

class ListCompositeDataSourceTest {

  private static final String LEAF_NAME = "leaf";
  private static final ByteArrayDataSource LEAF = ByteArrayDataSource.testInput(
    LEAF_NAME,
    FileType.GTFS,
    "Test"
  );

  private final ListCompositeDataSource subjectEmpty = new ListCompositeDataSource(
    "empty",
    FileType.GTFS,
    List.of()
  );
  private final ListCompositeDataSource subjectOne = new ListCompositeDataSource(
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
  void close() {
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
    assertEquals("ListCompositeReadOnlyDataSource/NNN", replaceHash(subjectEmpty.path()));
    assertEquals("ListCompositeReadOnlyDataSource/NNN", replaceHash(subjectOne.path()));
  }

  @Test
  void uri() {
    assertEquals(
      "ListCompositeReadOnlyDataSource/NNN/empty",
      replaceHash(subjectEmpty.uri().toString())
    );
    assertEquals(
      "ListCompositeReadOnlyDataSource/NNN/one",
      replaceHash(subjectOne.uri().toString())
    );
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

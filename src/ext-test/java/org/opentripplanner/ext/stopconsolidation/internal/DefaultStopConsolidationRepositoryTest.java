package org.opentripplanner.ext.stopconsolidation.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.transit.model._data.TransitModelForTest.id;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.stopconsolidation.model.ConsolidatedStopGroup;

class DefaultStopConsolidationRepositoryTest {

  private static final ConsolidatedStopGroup GROUP = new ConsolidatedStopGroup(
    id("123"),
    List.of(id("456"))
  );

  @Test
  void add() {
    var subject = new DefaultStopConsolidationRepository();
    assertEquals(List.of(), subject.groups());
    subject.addGroups(List.of(GROUP));

    assertEquals(List.of(GROUP), subject.groups());
  }

  @Test
  void groupsAreImmutable() {
    var subject = new DefaultStopConsolidationRepository();
    assertThrows(Exception.class, () -> subject.groups().add(GROUP));
  }
}

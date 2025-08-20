package org.opentripplanner.apis.transmodel.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.organization.Agency;

class FixedFeedIdGeneratorTest {

  @Test
  public void resolveFixedFeedIdTest() {
    assertEquals("UNKNOWN_FEED", FixedFeedIdGenerator.generate(List.of()));
    assertEquals("F", FixedFeedIdGenerator.generate(List.of(agency("F", 1))));
    assertEquals("A", FixedFeedIdGenerator.generate(List.of(agency("A", 1), agency("A", 2))));
    assertEquals(
      "A",
      FixedFeedIdGenerator.generate(List.of(agency("A", 1), agency("A", 2), agency("B", 1)))
    );
    assertTrue(
      "AB".contains(
          FixedFeedIdGenerator.generate(
            List.of(agency("A", 1), agency("A", 2), agency("B", 1), agency("B", 2))
          )
        ),
      "In case of a tie, A or B should be used"
    );
  }

  Agency agency(String feedScope, int id) {
    // We use the test builder to make sure we get back an agency with all required fields
    return TimetableRepositoryForTest.agency("Agency " + id)
      .copy()
      .withId(new FeedScopedId(feedScope, Integer.toString(id)))
      .build();
  }
}

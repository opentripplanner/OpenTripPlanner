package org.opentripplanner.ext.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.apis.transmodel.mapping.TransitIdMapper;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.organization.Agency;

public class TransmodelMappingUtilTest {

  @Test
  public void resolveFixedFeedIdTest() {
    assertEquals("UNKNOWN_FEED", TransitIdMapper.setupFixedFeedId(List.of()));
    assertEquals("F", TransitIdMapper.setupFixedFeedId(List.of(agency("F", 1))));
    assertEquals("A", TransitIdMapper.setupFixedFeedId(List.of(agency("A", 1), agency("A", 2))));
    assertEquals(
      "A",
      TransitIdMapper.setupFixedFeedId(List.of(agency("A", 1), agency("A", 2), agency("B", 1)))
    );
    assertTrue(
      "AB".contains(
          TransitIdMapper.setupFixedFeedId(
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

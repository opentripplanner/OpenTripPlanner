package org.opentripplanner.ext.transmodelapi.mapping;

import org.junit.Test;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.FeedScopedId;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TransmodelMappingUtilTest {

  @Test
  public void resolveFixedFeedIdTest() {
    assertEquals("UNKNOWN_FEED", TransitIdMapper.setupFixedFeedId(List.of()));
    assertEquals("F", TransitIdMapper.setupFixedFeedId(List.of(agency("F", 1))));
    assertEquals(
        "A", TransitIdMapper.setupFixedFeedId(List.of(agency("A", 1), agency("A", 2)))
    );
    assertEquals(
        "A",
        TransitIdMapper.setupFixedFeedId(List.of(agency("A", 1), agency("A", 2), agency("B", 1)))
    );
    assertTrue(
        "In case of a tie, A or B should be used",
        "AB".contains(TransitIdMapper.setupFixedFeedId(List.of(agency("A", 1),
            agency("A", 2),
            agency("B", 1),
            agency("B", 2)
            ))
        )
    );
  }

  Agency agency(String feedScope, int id) {
    Agency agency = new Agency(
        new FeedScopedId(feedScope, Integer.toString(id)),
        "Agency " + id,
        "Europe/Paris"
        );
    return agency;
  }
}
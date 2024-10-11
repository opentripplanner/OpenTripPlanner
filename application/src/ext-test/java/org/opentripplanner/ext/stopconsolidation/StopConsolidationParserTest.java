package org.opentripplanner.ext.stopconsolidation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.test.support.ResourceLoader;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class StopConsolidationParserTest {

  @Test
  void parse() {
    try (var file = ResourceLoader.of(this).inputStream("consolidated-stops.csv")) {
      var groups = StopConsolidationParser.parseGroups(file);
      assertEquals(20, groups.size());

      var first = groups.get(0);
      assertEquals("kcm:10225", first.primary().toString());
      assertEquals(
        List.of("pierce:1705867009"),
        first.secondaries().stream().map(FeedScopedId::toString).toList()
      );
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}

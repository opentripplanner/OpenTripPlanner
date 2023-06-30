package org.opentripplanner.gtfs.mapping;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;

class FareLegRuleMapperTest {

  @Test
  void emptyDistance() {
    var mapper = new FareLegRuleMapper(new FareProductMapper(), DataImportIssueStore.NOOP);
    var fareLegRules = List.of();
  }
}

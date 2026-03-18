package org.opentripplanner.netex.mapping;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;
import org.rutebanken.netex.model.DestinationDisplay;
import org.rutebanken.netex.model.MultilingualString;

class HeadsignMapperTest {

  private static final MultilingualString AAA = new MultilingualString().withValue("AAA");
  private static final MultilingualString BBB = new MultilingualString().withValue("BBB");

  @Test
  void onlyFrontText() {
    var dd = new DestinationDisplay().withFrontText(AAA);
    var result = defaultMapper().map(dd);
    assertEquals("AAA", result.toString());
  }

  @Test
  void onlyName() {
    var dd = new DestinationDisplay().withName(AAA);
    var result = defaultMapper().map(dd);
    assertEquals("AAA", result.toString());
  }

  @Test
  void frontTextPreferred() {
    var dd = new DestinationDisplay().withName(AAA).withFrontText(BBB);
    var result = defaultMapper().map(dd);
    assertEquals("BBB", result.toString());
  }

  @Test
  void nullable() {
    var issueStore = new DefaultDataImportIssueStore();
    var mapper = new HeadsignMapper(issueStore);
    var dd = new DestinationDisplay();
    assertNull(mapper.map(dd));

    var types = issueStore.listIssues().stream().map(DataImportIssue::getType);
    assertThat(types).contains("EmptyDestinationDisplay");
  }

  private static HeadsignMapper defaultMapper() {
    return new HeadsignMapper(DataImportIssueStore.NOOP);
  }
}

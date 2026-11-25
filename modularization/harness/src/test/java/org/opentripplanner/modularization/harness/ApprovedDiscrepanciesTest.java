package org.opentripplanner.modularization.harness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ApprovedDiscrepanciesTest {

  private static final Difference STOP_NAME = new Difference("gtfs-stops", "data.stops[1].name", "\"a\"", "\"b\"");
  private static final Difference ROUTE = new Difference("gtfs-routes", "data.routes[0].mode", "\"BUS\"", "\"TRAM\"");

  private static ApprovedDiscrepancies.Entry entry(String scenario, String path) {
    return new ApprovedDiscrepancies.Entry(new Glob(scenario), new Glob(path), "reason", "owner");
  }

  @Test
  void everythingIsUnapprovedWhenTheListIsEmpty() {
    var result = ApprovedDiscrepancies.empty().check(List.of(STOP_NAME));
    assertEquals(List.of(STOP_NAME), result.unapproved());
    assertFalse(result.isCompliant());
  }

  @Test
  void noDifferencesAndNoEntriesIsCompliant() {
    assertTrue(ApprovedDiscrepancies.empty().check(List.of()).isCompliant());
  }

  @Test
  void matchingEntryApprovesTheDifference() {
    var approved = new ApprovedDiscrepancies(List.of(entry("gtfs-stops", "data.stops[*].name")));
    var result = approved.check(List.of(STOP_NAME, ROUTE));
    assertEquals(List.of(ROUTE), result.unapproved());
    assertTrue(result.unused().isEmpty());
  }

  @Test
  void entryThatMatchesNothingIsReportedAsUnusedAndFails() {
    var approved = new ApprovedDiscrepancies(List.of(entry("*", "data.gone")));
    var result = approved.check(List.of());
    assertEquals(1, result.unused().size());
    assertFalse(result.isCompliant());
  }

  @Test
  void entriesNeedReasonAndOwner(@TempDir Path tmp) throws IOException {
    Path file = tmp.resolve("approved.json");
    Files.writeString(
      file,
      "{\"discrepancies\":[{\"scenario\":\"*\",\"path\":\"*\",\"reason\":\"\",\"owner\":\"me\"}]}"
    );
    assertThrows(IllegalArgumentException.class, () -> ApprovedDiscrepancies.read(file));
  }

  @Test
  void entriesAreReadFromFile(@TempDir Path tmp) throws IOException {
    Path file = tmp.resolve("approved.json");
    Files.writeString(
      file,
      "{\"discrepancies\":[{\"scenario\":\"gtfs-stops\",\"path\":\"data.stops[*].name\"," +
      "\"reason\":\"renamed\",\"owner\":\"someone\"}]}"
    );
    var result = ApprovedDiscrepancies.read(file).check(List.of(STOP_NAME));
    assertTrue(result.isCompliant());
  }

  @Test
  void missingFileMeansNothingIsApproved(@TempDir Path tmp) {
    var result = ApprovedDiscrepancies.read(tmp.resolve("none.json")).check(List.of(STOP_NAME));
    assertEquals(1, result.unapproved().size());
  }
}

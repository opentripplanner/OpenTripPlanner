package org.opentripplanner.ext.emission.internal.csvdata.route;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csvreader.CsvReader;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;

class RouteParserTest {

  private static final String DATA =
    """
    route_id, avg_co2_per_vehicle_per_km, avg_passenger_count
    R:1, 2.0, 20.0
    R:2, 3.0, 24.0
    """;

  @Test
  void test() {
    var subject = new RouteCsvParser(DataImportIssueStore.NOOP, CsvReader.parse(DATA));
    assertTrue(subject.headersMatch(), subject::toString);
    assertTrue(subject.hasNext(), subject::toString);
    assertEquals(
      "RouteRow[routeId=R:1, avgCo2InGramPerKm=2.0, avgPassengerCount=20.0]",
      subject.next().toString()
    );
    assertTrue(subject.hasNext(), subject::toString);
    assertEquals(
      "RouteRow[routeId=R:2, avgCo2InGramPerKm=3.0, avgPassengerCount=24.0]",
      subject.next().toString()
    );
    assertFalse(subject.hasNext(), subject::toString);
  }
}

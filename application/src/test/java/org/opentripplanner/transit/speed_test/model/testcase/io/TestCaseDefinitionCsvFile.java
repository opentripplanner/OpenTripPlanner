package org.opentripplanner.transit.speed_test.model.testcase.io;

import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.via.VisitViaLocation;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.transit.speed_test.model.testcase.TestCaseDefinition;

public class TestCaseDefinitionCsvFile extends AbstractCsvFile<TestCaseDefinition> {

  private final String feedId;

  public TestCaseDefinitionCsvFile(File file, String feedId) {
    super(file);
    this.feedId = feedId;
  }

  @Override
  String cell(TestCaseDefinition row, String colName) {
    throw new IllegalStateException("We only support reading test-case-definitions");
  }

  @Override
  TestCaseDefinition parseRow() throws IOException {
    return new TestCaseDefinition(
      parseString("testCaseId"),
      parseString("description"),
      parseTime("departure"),
      parseTime("arrival"),
      parseDuration("window"),
      new GenericLocation(
        parseString("origin"),
        FeedScopedId.ofNullable(feedId, parseString("fromPlace")),
        parseDouble("fromLat"),
        parseDouble("fromLon")
      ),
      new GenericLocation(
        parseString("destination"),
        FeedScopedId.ofNullable(feedId, parseString("toPlace")),
        parseDouble("toLat"),
        parseDouble("toLon")
      ),
      parseViaLocation(),
      parseString("category"),
      new QualifiedModeSet(parseCollection("modes").toArray(new String[0]))
    );
  }

  @Nullable
  private VisitViaLocation parseViaLocation() {
    try {
      return new VisitViaLocation(
        parseString("viaLabel"),
        parseDuration("viaMinimumWaitTime"),
        List.of(),
        new WgsCoordinate(parseDouble("viaLat"), parseDouble("viaLon"))
      );
    } catch (Exception e) {
      return null;
    }
  }
}

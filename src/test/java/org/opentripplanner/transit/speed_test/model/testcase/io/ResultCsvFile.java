package org.opentripplanner.transit.speed_test.model.testcase.io;

import java.io.File;
import java.io.IOException;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.speed_test.model.testcase.Result;

/**
 * This class is responsible for reading and writing test cases and test case results to CSV files.
 */
public class ResultCsvFile extends AbstractCsvFile<Result> {

  private static final String TC_ID = "tcId";
  private static final String N_TRANSFERS = "nTransfers";
  private static final String DURATION = "duration";
  private static final String COST = "cost";
  private static final String WALK_DISTANCE = "walkDistance";
  private static final String START_TIME = "startTime";
  private static final String END_TIME = "endTime";
  private static final String AGENCIES = "agencies";
  private static final String MODES = "modes";
  private static final String ROUTES = "routes";
  private static final String STOPS = "stops";
  private static final String DETAILS = "details";

  private static final String[] HEADERS = {
    TC_ID,
    N_TRANSFERS,
    DURATION,
    COST,
    WALK_DISTANCE,
    START_TIME,
    END_TIME,
    AGENCIES,
    MODES,
    ROUTES,
    STOPS,
    DETAILS,
  };

  public ResultCsvFile(File file) {
    super(file, HEADERS);
  }

  @Override
  String cell(Result row, String colName) {
    return switch (colName) {
      case TC_ID -> row.testCaseId();
      case N_TRANSFERS -> Integer.toString(row.nTransfers());
      case DURATION -> duration2Str(row.duration());
      case COST -> Integer.toString(row.cost());
      case WALK_DISTANCE -> Integer.toString(row.walkDistance());
      case START_TIME -> time2str(row.startTime());
      case END_TIME -> time2str(row.endTime());
      case AGENCIES -> col2Str(row.agencies());
      case MODES -> col2Str(row.modes());
      case ROUTES -> col2Str(row.routes());
      case STOPS -> col2Str(row.stops());
      // Skip delimiter for the last value
      case DETAILS -> row.details();
      default -> throw new IllegalArgumentException("Unexpected column name: " + colName);
    };
  }

  @Override
  Result parseRow() throws IOException {
    return new Result(
      parseString(TC_ID),
      parseInt(N_TRANSFERS),
      parseDuration(DURATION),
      parseInt(COST),
      parseInt(WALK_DISTANCE),
      parseTime(START_TIME),
      parseTime(END_TIME),
      parseCollection(AGENCIES),
      parseCollection(MODES, TransitMode::valueOf),
      parseCollection(ROUTES),
      parseCollection(STOPS),
      parseString(DETAILS)
    );
  }
}

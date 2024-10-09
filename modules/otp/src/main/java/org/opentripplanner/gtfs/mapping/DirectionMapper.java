package org.opentripplanner.gtfs.mapping;

import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.timetable.Direction;

public class DirectionMapper {

  private final DataImportIssueStore issueStore;

  public DirectionMapper(DataImportIssueStore issueStore) {
    this.issueStore = issueStore;
  }

  Direction map(String gtfsCode, FeedScopedId id) {
    try {
      if (gtfsCode == null || gtfsCode.isBlank()) {
        return Direction.UNKNOWN;
      }
      return map(Integer.parseInt(gtfsCode));
    } catch (NumberFormatException e) {
      issueStore.add(
        "InvalidGTFSDirectionId",
        "Trip %s does not have direction id, defaults to -1",
        id
      );
    }
    return Direction.UNKNOWN;
  }

  public Direction map(int gtfsCode) {
    return switch (gtfsCode) {
      case 0 -> Direction.OUTBOUND;
      case 1 -> Direction.INBOUND;
      default -> Direction.UNKNOWN;
    };
  }
}

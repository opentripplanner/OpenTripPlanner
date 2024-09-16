package org.opentripplanner.netex.validation;

import java.util.Set;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.netex.index.NetexEntityIndex;

public class RouteToCentroidStationIdValidator {
  public static void validate(
    DataImportIssueStore issueStore,
    Set<String> routeToCentroidStationIds,
    NetexEntityIndex index
  ) {
    routeToCentroidStationIds
      .stream()
      .filter(id -> !index.stopPlaceById.containsKey(id))
      .forEach(id ->
        issueStore.add(
          "UnknownStopPlaceId",
          "routeToCentroidStationIds specified a stopPlace that does not exist: " + id
        )
      );
  }
}

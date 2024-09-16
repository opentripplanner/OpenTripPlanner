package org.opentripplanner.netex.validation;

import java.util.Set;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.netex.index.NetexEntityIndex;

public class RouteToCentroidStopPlaceIdValidator {

  public static void validate(
    DataImportIssueStore issueStore,
    Set<String> routeToCentroidStopPlaceIds,
    NetexEntityIndex index
  ) {
    routeToCentroidStopPlaceIds
      .stream()
      .filter(id -> !index.stopPlaceById.containsKey(id))
      .forEach(id ->
        issueStore.add(
          "UnknownStopPlaceId",
          "routeToCentroidStopPlaceIds specified a stop place that does not exist: %s",
          id
        )
      );
  }
}

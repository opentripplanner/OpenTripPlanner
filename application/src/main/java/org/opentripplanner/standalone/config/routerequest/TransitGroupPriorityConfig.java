package org.opentripplanner.standalone.config.routerequest;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_3;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_5;

import java.util.List;
import org.opentripplanner.routing.api.request.request.TransitRequestBuilder;
import org.opentripplanner.routing.api.request.request.filter.TransitGroupSelect;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.framework.json.OtpVersion;
import org.opentripplanner.transit.model.basic.TransitMode;

public class TransitGroupPriorityConfig {

  public static void mapTransitRequest(NodeAdapter root, TransitRequestBuilder transit) {
    var c = root
      .of("transitGroupPriority")
      .since(OtpVersion.V2_5)
      .summary(
        "Group transit patterns and give each group a mutual advantage in the Raptor search."
      )
      .description(
        """
        Use this to separate transit patterns into groups. Each group will be given a group-id. A
        path (multiple legs) will then have a set of group-ids based on the group-id from each leg.
        Hence, two paths with a different set of group-ids will BOTH be optimal unless the cost is
        worse than the relaxation specified in the `relaxTransitGroupPriority` parameter. This is
        only available in the TransmodelAPI for now.

        Unmatched patterns are put in the BASE priority-group.
        """
      )
      .experimentalFeature()
      .asObject();

    transit.withPriorityGroupsByAgency(
      TransitGroupPriorityConfig.mapList(
        c,
        "byAgency",
        "All groups here are split by agency. For example if you list mode " +
        "[RAIL, COACH] then all rail and coach services run by an agency get the same " +
        "group-id."
      )
    );
    transit.addPriorityGroupsGlobal(
      TransitGroupPriorityConfig.mapList(
        c,
        "global",
        "All services matching a 'global' group will get the same group-id. Use this " +
        "to assign the same id to a specific mode/sub-mode/route."
      )
    );
  }

  private static List<TransitGroupSelect> mapList(
    NodeAdapter root,
    String parameterName,
    String description
  ) {
    return root
      .of(parameterName)
      .since(V2_5)
      .summary("List of transit groups.")
      .description(description + " The max total number of group-ids are 32, so be careful.")
      .asObjects(TransitGroupPriorityConfig::mapTransitGroupSelect);
  }

  private static TransitGroupSelect mapTransitGroupSelect(NodeAdapter c) {
    return TransitGroupSelect.of()
      .addModes(
        c
          .of("modes")
          .since(V2_5)
          .summary("List all modes to select for this group.")
          .asEnumSet(TransitMode.class)
      )
      .addSubModeRegexp(
        c
          .of("subModes")
          .since(V2_5)
          .summary("List a set of regular expressions for matching sub-modes.")
          .asStringList(List.of())
      )
      .addAgencyIds(
        c.of("agencies").since(V2_3).summary("List agency ids to match.").asFeedScopedIds(List.of())
      )
      .addRouteIds(
        c.of("routes").since(V2_3).summary("List route ids to match.").asFeedScopedIds(List.of())
      )
      .build();
  }
}

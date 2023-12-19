package org.opentripplanner.apis.transmodel.model;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.api.resource.DebugOutput;
import org.opentripplanner.model.plan.TripPlan;
import org.opentripplanner.model.plan.paging.cursor.PageCursor;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.TripSearchMetadata;

public class PlanResponse {

  public TripPlan plan;
  public TripSearchMetadata metadata;
  public List<RoutingError> messages = new ArrayList<>();
  public DebugOutput debugOutput;
  public PageCursor previousPageCursor;
  public PageCursor nextPageCursor;

  @Override
  public String toString() {
    return (
      "PlanResponse{" +
      "plan=" +
      plan +
      ", metadata=" +
      metadata +
      ", messages=" +
      messages +
      ", debugOutput=" +
      debugOutput +
      ", previousPageCursor=" +
      previousPageCursor +
      ", nextPageCursor=" +
      nextPageCursor +
      '}'
    );
  }
}

package org.opentripplanner.routing.api.request.request;

import java.util.List;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class ViaPoint {

  private final List<FeedScopedId> ids;

  public ViaPoint(List<FeedScopedId> ids) {
    this.ids = List.copyOf(ids);
  }

  public List<FeedScopedId> ids() {
    return ids;
  }
}

package org.opentripplanner.api.model;

import java.util.List;
import java.util.Optional;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public record ApiFareComponent(
  FeedScopedId fareId,
  String name,
  ApiMoney price,
  List<FeedScopedId> routes
) {}

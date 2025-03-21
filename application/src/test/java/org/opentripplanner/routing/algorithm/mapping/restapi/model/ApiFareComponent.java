package org.opentripplanner.routing.algorithm.mapping.restapi.model;

import java.util.List;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public record ApiFareComponent(
  FeedScopedId fareId,
  String name,
  ApiMoney price,
  List<FeedScopedId> routes
) {}

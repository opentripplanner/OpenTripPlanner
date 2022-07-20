package org.opentripplanner.api.model;

import java.util.List;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public record ApiFareComponent(FeedScopedId fareId, ApiMoney price, List<FeedScopedId> routes) {}

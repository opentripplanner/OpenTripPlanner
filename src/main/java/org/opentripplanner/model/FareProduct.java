package org.opentripplanner.model;

import java.time.Duration;
import org.opentripplanner.routing.core.Money;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public record FareProduct(FeedScopedId id, String name, Money amount, Duration duration) {}

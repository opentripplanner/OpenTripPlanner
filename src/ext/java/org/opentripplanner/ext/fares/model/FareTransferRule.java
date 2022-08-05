package org.opentripplanner.ext.fares.model;

import java.time.Duration;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public record FareTransferRule(
  @Nonnull FeedScopedId fromLegGroup,
  @Nonnull FeedScopedId toLegGroup,
  int transferCount,
  @Nullable Duration timeLimit,
  @Nonnull FareProduct fareProduct
) {}

package org.opentripplanner.ext.fares.model;

import java.time.Duration;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public record FareTransferRule(
  @Nonnull String fromLegGroup,
  @Nonnull String toLegGroup,
  int transferCount,
  @Nullable Duration timeLimit,
  @Nonnull FareProduct fareProduct
) {}

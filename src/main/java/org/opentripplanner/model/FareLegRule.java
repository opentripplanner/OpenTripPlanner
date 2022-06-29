package org.opentripplanner.model;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public record FareLegRule(
  @Nonnull String feedId,
  @Nullable String networkId,
  @Nonnull FareProduct fareProduct
) {}

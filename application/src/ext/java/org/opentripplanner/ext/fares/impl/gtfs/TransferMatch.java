package org.opentripplanner.ext.fares.impl.gtfs;

import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.ext.fares.model.FareTransferRule;

public record TransferMatch(
  FareTransferRule transferRule,
  FareLegRule fromLegRule,
  FareLegRule toLegRule
) {}

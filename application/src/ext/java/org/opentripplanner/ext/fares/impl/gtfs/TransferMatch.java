package org.opentripplanner.ext.fares.impl.gtfs;

import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.ext.fares.model.FareTransferRule;

/**
 * A rule for transferring from one leg to another one.
 */
record TransferMatch(
  FareTransferRule transferRule,
  FareLegRule fromLegRule,
  FareLegRule toLegRule
) {}

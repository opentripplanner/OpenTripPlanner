package org.opentripplanner.graph_builder.module.transfer.api;

import org.opentripplanner.transit.transfer.regular.api.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.parameters.TransferProfileType;

/**
 * A configured {@code transfers:} profile (see {@code TransferProfilesConfig}), resolved into the
 * types the new raptor-data transfer pipeline needs.
 *
 * @param profileType this profile's identifying key
 * @param preferences this profile's own traveler preferences - the concrete {@code U} the
 *                    {@code transit-transfer} module caches and normalizes paths under
 */
public record TransferProfileConfig(
  TransferProfileType profileType,
  AbstractUserPreferences<?> preferences
) {}

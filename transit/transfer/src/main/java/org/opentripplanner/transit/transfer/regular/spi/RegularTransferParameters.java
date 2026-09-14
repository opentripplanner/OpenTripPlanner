package org.opentripplanner.transit.transfer.regular.spi;

import org.opentripplanner.transit.transfer.regular.RaptorTransferProfile;

import javax.annotation.Nullable;

/**
 * A configured transfer discovery profile.
 * <p>
 * Duration limits (including any per-target-stop {@code allowedModes} override) are entirely the
 * {@link TransferPathProvider} implementation's responsibility - {@code raptor-data} never sees
 * stop-classification data, only the opaque path/preferences types.
 *
 * @param profileId   this profile's identifying key
 * @param base        optional: another configured profile whose already-discovered paths this
 *                    profile deduplicates its own paths against (see
 *                    {@code DefaultTransferGenerator}). {@code base} references must form a DAG.
 * @param preferences this profile's own preferences, used both for its own discovery search and
 *                    to re-cost a {@code base}'s path when checking for equivalence
 * @param <U>         the user preferences type
 */
public record RegularTransferParameters<U>(
  RaptorTransferProfile profileId,
  @Nullable RaptorTransferProfile base,
  U preferences
) {}

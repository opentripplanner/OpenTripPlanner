package org.opentripplanner.transit.transfer.regular.spi;

import javax.annotation.Nullable;
import org.opentripplanner.transit.transfer.regular.RaptorTransferProfile;

/**
 * A configured transfer discovery profile.
 * <p>
 * Duration limits (including any per-target-stop {@code allowedModes} override) are entirely the
 * {@link TransferPathProvider} implementation's responsibility - {@code raptor-data} never sees
 * stop-classification data, only the opaque path/preferences types.
 *
 * @param profileId            this profile's identifying key
 * @param deduplicationProfile optional: another configured profile whose already-discovered
 *                             paths this profile deduplicates its own paths against (see
 *                             {@code DefaultTransferGenerator}). {@code deduplicationProfile}
 *                             references must form a DAG.
 * @param preferences          this profile's own preferences, used both for its own discovery
 *                             search and to re-cost a {@code deduplicationProfile}'s path when
 *                             checking for equivalence
 * @param <U>                  the user preferences type
 */
public record RegularTransferParameters<U>(
  RaptorTransferProfile profileId,
  @Nullable RaptorTransferProfile deduplicationProfile,
  U preferences
) {}

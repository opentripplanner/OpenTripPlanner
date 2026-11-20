package org.opentripplanner.transfer.constrained.model;

import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;

/**
 * A {@link ConstrainedTransfer} whose endpoints may still need their stop position resolved
 * against a trip's final stop-time sequence - see {@link DeferredConstrainedTransferPoint}.
 */
public record PendingConstrainedTransfer(
  @Nullable FeedScopedId id,
  DeferredConstrainedTransferPoint from,
  DeferredConstrainedTransferPoint to,
  TransferConstraint constraint
) {}

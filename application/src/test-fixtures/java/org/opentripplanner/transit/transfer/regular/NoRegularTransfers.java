package org.opentripplanner.transit.transfer.regular;

import java.util.Collections;
import java.util.Iterator;
import javax.annotation.Nullable;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.raptor.data.transfers.regular.RaptorRegularTransferService;
import org.opentripplanner.raptor.spi.RaptorTransfer;
import org.opentripplanner.transit.transfer.regular.api.AbstractUserPreferences;

/**
 * A regular-transfer snapshot without any transfers, for tests that route without regular
 * transfers. Use {@code TestServerContext.createRegularTransfers} to route with the transfers
 * generated from the test graph.
 */
public final class NoRegularTransfers {

  private NoRegularTransfers() {}

  public static RegularTransferRepositorySnapshot<NearbyStop> snapshot() {
    return SNAPSHOT;
  }

  public static RaptorRegularTransferService service() {
    return SERVICE;
  }

  private static final RaptorRegularTransferService SERVICE = new RaptorRegularTransferService() {
    @Override
    public Iterator<? extends RaptorTransfer> getTransfersFromStop(int fromStop) {
      return Collections.emptyIterator();
    }

    @Override
    public Iterator<? extends RaptorTransfer> getTransfersToStop(int toStop) {
      return Collections.emptyIterator();
    }
  };

  private static final RegularTransferRepositorySnapshot<NearbyStop> SNAPSHOT =
    new RegularTransferRepositorySnapshot<>() {
      @Override
      public RaptorRegularTransferService raptorTransfers(
        AbstractUserPreferences<?> userPreferences
      ) {
        return SERVICE;
      }

      @Nullable
      @Override
      public NearbyStop findPath(
        AbstractUserPreferences<?> userPreferences,
        int fromStop,
        int toStop
      ) {
        return null;
      }
    };
}

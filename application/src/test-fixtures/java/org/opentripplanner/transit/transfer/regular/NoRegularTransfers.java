package org.opentripplanner.transit.transfer.regular;

import java.util.Collections;
import java.util.Iterator;
import javax.annotation.Nullable;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.raptor.spi.RaptorTransfer;
import org.opentripplanner.transit.transfer.regular.api.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.parameters.TransferProfileType;

/**
 * A regular-transfer pipeline without any transfers, for tests that route without regular
 * transfers. Use {@code TestServerContext.createRegularTransferServiceFactory} to route with the
 * transfers generated from the test graph.
 */
public final class NoRegularTransfers {

  private NoRegularTransfers() {}

  public static RegularTransferServiceFactory<NearbyStop> factory() {
    return FACTORY;
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

  private static final RegularTransferServiceFactory<NearbyStop> FACTORY =
    new RegularTransferServiceFactory<>() {
      @Override
      public RaptorRegularTransferService create(
        TransferProfileType profileType,
        AbstractUserPreferences<?> preferences
      ) {
        return SERVICE;
      }

      @Nullable
      @Override
      public NearbyStop findPath(TransferProfileType profileType, int fromStop, int toStop) {
        return null;
      }
    };
}

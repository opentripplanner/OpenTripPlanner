package org.opentripplanner.transit.transfer.regular;

import java.util.Collections;
import java.util.Iterator;
import javax.annotation.Nullable;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.raptor.data.transfers.regular.RaptorRegularTransferService;
import org.opentripplanner.raptor.spi.RaptorTransfer;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.transit.transfer.regular.api.RegularTransfers;

/**
 * A regular-transfer snapshot without any transfers, for tests that route without regular
 * transfers. Use {@code TestServerContext.createRegularTransfers} to route with the transfers
 * generated from the test graph.
 */
public final class NoRegularTransfers {

  private NoRegularTransfers() {}

  public static RegularTransferRepositorySnapshot<NearbyStop, RouteRequest> snapshot() {
    return SNAPSHOT;
  }

  public static RegularTransfers<NearbyStop> transfers() {
    return TRANSFERS;
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

  private static final RegularTransfers<NearbyStop> TRANSFERS = new RegularTransfers<>() {
    @Override
    public RaptorRegularTransferService raptorTransfers() {
      return SERVICE;
    }

    @Nullable
    @Override
    public NearbyStop findPath(int fromStop, int toStop) {
      return null;
    }
  };

  private static final RegularTransferRepositorySnapshot<NearbyStop, RouteRequest> SNAPSHOT = _ ->
    TRANSFERS;
}

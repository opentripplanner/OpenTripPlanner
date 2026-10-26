package org.opentripplanner.transit.transfer.regular;

import java.util.Collections;
import java.util.Iterator;
import javax.annotation.Nullable;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.raptor.data.transfers.regular.RaptorRegularTransferService;
import org.opentripplanner.raptor.spi.RaptorTransfer;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.transit.transfer.regular.api.RegularTransfersService;
import org.opentripplanner.transit.transfer.regular.api.report.RegularTransferCacheMetrics;
import org.opentripplanner.transit.transfer.regular.api.report.RegularTransferCacheReport;

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

  public static RegularTransfersService<NearbyStop> transfers() {
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

  private static final RegularTransfersService<NearbyStop> TRANSFERS =
    new RegularTransfersService<>() {
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

  private static final RegularTransferRepositorySnapshot<NearbyStop, RouteRequest> SNAPSHOT =
    new RegularTransferRepositorySnapshot<>() {
      @Override
      public RegularTransfersService<NearbyStop> forRequest(RouteRequest request) {
        return TRANSFERS;
      }

      @Override
      public void warmUp() {}

      @Override
      public RegularTransferCacheReport cacheReport() {
        return RegularTransferCacheReport.DISABLED;
      }

      @Override
      public RegularTransferCacheMetrics cacheMetrics() {
        return new RegularTransferCacheMetrics() {
          @Override
          public long size() {
            return 0;
          }

          @Override
          public long hitCount() {
            return 0;
          }

          @Override
          public long missCount() {
            return 0;
          }

          @Override
          public long putCount() {
            return 0;
          }

          @Override
          public long evictionCount() {
            return 0;
          }

          @Override
          public long loadFailureCount() {
            return 0;
          }

          @Override
          public long totalLoadTimeNanos() {
            return 0;
          }
        };
      }
    };
}

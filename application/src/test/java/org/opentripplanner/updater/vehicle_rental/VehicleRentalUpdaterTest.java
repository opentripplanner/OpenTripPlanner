package org.opentripplanner.updater.vehicle_rental;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.util.concurrent.Futures;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.vehiclerental.internal.DefaultVehicleRentalService;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.updater.DefaultRealTimeUpdateContext;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.opentripplanner.updater.spi.HttpHeaders;
import org.opentripplanner.updater.vehicle_rental.datasources.VehicleRentalDataSource;
import org.opentripplanner.updater.vehicle_rental.datasources.params.RentalPickupType;
import org.opentripplanner.updater.vehicle_rental.datasources.params.VehicleRentalDataSourceParameters;

class VehicleRentalUpdaterTest {

  @Test
  void failingDataSourceCountsAsPrimed() {
    var source = new FailingDataSource();
    var updater = new VehicleRentalUpdater(
      new VehicleRentalUpdaterParameters("A", Duration.ofMinutes(1), new FakeParams()),
      source,
      null,
      new DefaultVehicleRentalService()
    );

    assertFalse(updater.isPrimed());
    var manager = new MockManager(updater);
    manager.startUpdaters();
    assertTrue(source.hasFailed());
    manager.stop(false);
    assertTrue(updater.isPrimed());
  }

  static class MockManager extends GraphUpdaterManager {

    public MockManager(VehicleRentalUpdater updater) {
      super(
        new DefaultRealTimeUpdateContext(new Graph(), new TimetableRepository()),
        List.of(updater)
      );
    }

    @Override
    public Future<?> execute(GraphWriterRunnable runnable) {
      return Futures.immediateVoidFuture();
    }
  }

  static class FailingDataSource implements VehicleRentalDataSource {

    private final CompletableFuture<Boolean> hasFailed = new CompletableFuture<>();

    @Override
    public boolean update() {
      hasFailed.complete(true);
      throw new RuntimeException("An error occurred while updating the source.");
    }

    @Override
    public List<VehicleRentalPlace> getUpdates() {
      return null;
    }

    private boolean hasFailed() {
      try {
        return hasFailed.get(5, TimeUnit.SECONDS);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  static class FakeParams implements VehicleRentalDataSourceParameters {

    @Override
    public String url() {
      return "https://example.com";
    }

    @Nullable
    @Override
    public String network() {
      return "Test";
    }

    @Override
    public VehicleRentalSourceType sourceType() {
      return VehicleRentalSourceType.GBFS;
    }

    @Override
    public HttpHeaders httpHeaders() {
      return HttpHeaders.empty();
    }

    @Override
    public boolean allowRentalType(RentalPickupType rentalPickupType) {
      return true;
    }
  }
}

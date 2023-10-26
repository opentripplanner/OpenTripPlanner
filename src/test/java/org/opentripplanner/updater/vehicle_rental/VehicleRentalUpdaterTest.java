package org.opentripplanner.updater.vehicle_rental;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.util.concurrent.Futures;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.vehiclerental.internal.DefaultVehicleRentalService;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.opentripplanner.updater.spi.HttpHeaders;
import org.opentripplanner.updater.vehicle_rental.datasources.VehicleRentalDatasource;
import org.opentripplanner.updater.vehicle_rental.datasources.params.VehicleRentalDataSourceParameters;

class VehicleRentalUpdaterTest {

  @Test
  void failingDatasourceCountsAsPrimed() {
    var source = new FailingDatasource();
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
      super(new Graph(), new TransitModel(), List.of(updater));
    }

    @Override
    public Future<?> execute(GraphWriterRunnable runnable) {
      return Futures.immediateVoidFuture();
    }
  }

  static class FailingDatasource implements VehicleRentalDatasource {

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

    @Nonnull
    @Override
    public String url() {
      return "https://example.com";
    }

    @Nullable
    @Override
    public String network() {
      return "Test";
    }

    @Nonnull
    @Override
    public VehicleRentalSourceType sourceType() {
      return VehicleRentalSourceType.GBFS;
    }

    @Nonnull
    @Override
    public HttpHeaders httpHeaders() {
      return HttpHeaders.empty();
    }
  }
}

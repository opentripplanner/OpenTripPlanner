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
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.vehiclerental.internal.DefaultVehicleRentalService;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.updater.DefaultRealTimeUpdateContext;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.opentripplanner.updater.spi.HttpHeaders;
import org.opentripplanner.updater.spi.UpdaterConstructionException;
import org.opentripplanner.updater.vehicle_rental.datasources.VehicleRentalDataSource;
import org.opentripplanner.updater.vehicle_rental.datasources.params.RentalPickupType;
import org.opentripplanner.updater.vehicle_rental.datasources.params.VehicleRentalDataSourceParameters;

class VehicleRentalUpdaterTest {

  public static final VehicleRentalUpdaterParameters PARAMS = new VehicleRentalUpdaterParameters(
    "A",
    Duration.ofMinutes(1),
    new FakeParams()
  );
  public static final DefaultVehicleRentalService SERVICE = new DefaultVehicleRentalService();

  @Test
  void failingDataSourceCountsAsPrimed() {
    var source = new FailingDataSource();
    var updater = new VehicleRentalUpdater(PARAMS, source, null, SERVICE);

    assertFalse(updater.isPrimed());
    var manager = new MockManager(updater);
    manager.startUpdaters();
    assertTrue(source.hasFailed());
    manager.stop(false);
    assertTrue(updater.isPrimed());
  }

  /**
   * It's not clear why this tests fails on Windows and I don't have a test machine to find out.
   */
  @Test
  @DisabledOnOs(OS.WINDOWS)
  void failingSetup() {
    var source = new FailingSetupDataSource();
    var updater = new VehicleRentalUpdater(PARAMS, source, null, SERVICE);

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

  private static class FailingDataSource implements VehicleRentalDataSource {

    private final CompletableFuture<Boolean> hasFailed = new CompletableFuture<>();

    @Override
    public boolean update() {
      hasFailed.complete(true);
      throw new UpdaterConstructionException("An error occurred while setting up the source.");
    }

    @Override
    public List<VehicleRentalPlace> getUpdates() {
      return List.of();
    }

    private boolean hasFailed() {
      try {
        return hasFailed.get(5, TimeUnit.SECONDS);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static class FailingSetupDataSource implements VehicleRentalDataSource {

    private final CompletableFuture<Boolean> hasFailed = new CompletableFuture<>();

    @Override
    public void setup() {
      this.hasFailed.complete(true);
      throw new UpdaterConstructionException("An error occurred while setting up the source.");
    }

    @Override
    public boolean update() {
      return true;
    }

    @Override
    public List<VehicleRentalPlace> getUpdates() {
      return List.of();
    }

    private boolean hasFailed() {
      try {
        return hasFailed.get(5, TimeUnit.SECONDS);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static class FakeParams implements VehicleRentalDataSourceParameters {

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

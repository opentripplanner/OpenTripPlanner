package org.opentripplanner.updater.vehicle_rental;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.util.concurrent.Futures;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalPlace;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalService;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.opentripplanner.updater.vehicle_rental.datasources.VehicleRentalDatasource;
import org.opentripplanner.updater.vehicle_rental.datasources.params.VehicleRentalDataSourceParameters;

class VehicleRentalUpdaterTest {

  @Test
  void failingDatasourceCountsAsPrimed() {
    var source = new FailingDatasource();
    var updater = new VehicleRentalUpdater(
      new VehicleRentalUpdaterParameters("A", 1, new FakeParams()),
      source,
      null,
      new VehicleRentalService()
    );

    assertFalse(updater.isPrimed());
    var manager = new MockManager(updater);
    manager.startUpdaters();
    manager.stop();
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

    @Override
    public boolean update() {
      throw new RuntimeException("An error occurred while updating the source.");
    }

    @Override
    public List<VehicleRentalPlace> getUpdates() {
      return null;
    }
  }

  static class FakeParams implements VehicleRentalDataSourceParameters {

    @Nonnull
    @Override
    public String url() {
      return "https://example.com";
    }

    @Nonnull
    @Override
    public VehicleRentalSourceType sourceType() {
      return VehicleRentalSourceType.GBFS;
    }

    @Nonnull
    @Override
    public Map<String, String> httpHeaders() {
      return Map.of();
    }
  }
}

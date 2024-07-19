package org.opentripplanner.updater.vehicle_parking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opentripplanner.transit.model._data.TransitModelForTest.id;

import com.google.common.util.concurrent.Futures;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingService;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingSpaces;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.opentripplanner.updater.spi.DataSource;
import org.opentripplanner.updater.spi.GraphUpdater;

class VehicleParkingAvailabilityUpdaterTest {

  private static final VehicleParkingUpdaterParameters PARAMETERS = new VehicleParkingUpdaterParameters() {
    @Override
    public VehicleParkingSourceType sourceType() {
      return VehicleParkingSourceType.SIRI_FM;
    }

    @Override
    public UpdateType updateType() {
      return UpdateType.AVAILABILITY_ONLY;
    }

    @Override
    public Duration frequency() {
      return Duration.ZERO;
    }

    @Override
    public String configRef() {
      return null;
    }
  };
  private static final FeedScopedId ID = id("parking1");

  @Test
  void updateAvailability() {
    var service = new VehicleParkingService();

    var parking = VehicleParking
      .builder()
      .id(ID)
      .name(I18NString.of("parking"))
      .coordinate(WgsCoordinate.GREENWICH)
      .carPlaces(true)
      .capacity(VehicleParkingSpaces.builder().carSpaces(10).build())
      .build();
    service.updateVehicleParking(List.of(parking), List.of());

    var updater = new VehicleParkingAvailabilityUpdater(PARAMETERS, new StubDatasource(), service);

    runUpdaterOnce(updater);

    var updated = service.getVehicleParkings().toList().getFirst();
    assertEquals(ID, updated.getId());
    assertEquals(8, updated.getAvailability().getCarSpaces());
    assertNull(updated.getAvailability().getBicycleSpaces());
  }

  private void runUpdaterOnce(VehicleParkingAvailabilityUpdater updater) {
    class GraphUpdaterMock extends GraphUpdaterManager {

      private static final Graph GRAPH = new Graph();
      private static final TransitModel TRANSIT_MODEL = new TransitModel();

      public GraphUpdaterMock(List<GraphUpdater> updaters) {
        super(GRAPH, TRANSIT_MODEL, updaters);
      }

      @Override
      public Future<?> execute(GraphWriterRunnable runnable) {
        runnable.run(GRAPH, TRANSIT_MODEL);
        return Futures.immediateVoidFuture();
      }
    }

    var graphUpdaterManager = new GraphUpdaterMock(List.of(updater));
    graphUpdaterManager.startUpdaters();
    graphUpdaterManager.stop(false);
  }

  private static class StubDatasource implements DataSource<AvailabiltyUpdate> {

    @Override
    public boolean update() {
      return true;
    }

    @Override
    public List<AvailabiltyUpdate> getUpdates() {
      return List.of(new AvailabiltyUpdate(ID, 8));
    }
  }
}

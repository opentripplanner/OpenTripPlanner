package org.opentripplanner.updater.vehicle_parking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opentripplanner.standalone.config.framework.json.JsonSupport.newNodeAdapterForTest;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import com.google.common.util.concurrent.Futures;
import java.util.List;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.vehicleparking.VehicleParkingRepository;
import org.opentripplanner.service.vehicleparking.internal.DefaultVehicleParkingRepository;
import org.opentripplanner.service.vehicleparking.model.VehicleParking;
import org.opentripplanner.service.vehicleparking.model.VehicleParkingSpaces;
import org.opentripplanner.standalone.config.routerconfig.updaters.VehicleParkingUpdaterConfig;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.updater.DefaultRealTimeUpdateContext;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.opentripplanner.updater.spi.DataSource;
import org.opentripplanner.updater.spi.GraphUpdater;

class VehicleParkingAvailabilityUpdaterTest {

  private static final VehicleParkingUpdaterParameters PARAMETERS =
    VehicleParkingUpdaterConfig.create(
      "ref",
      newNodeAdapterForTest(
        """
          {
            "type" : "vehicle-parking",
            "feedId" : "parking",
            "sourceType" : "siri-fm",
            "frequency": "0s",
            "url" : "https://transmodel.api.opendatahub.com/siri-lite/fm/parking"
          }
        """
      )
    );

  private static final FeedScopedId ID = id("parking1");
  private static final AvailabiltyUpdate DEFAULT_UPDATE = new AvailabiltyUpdate(ID, 8);

  @Test
  void updateCarAvailability() {
    var service = buildParkingRepository(VehicleParkingSpaces.builder().carSpaces(10).build());
    var updater = new VehicleParkingAvailabilityUpdater(
      PARAMETERS,
      new StubDataSource(DEFAULT_UPDATE),
      service
    );

    runUpdaterOnce(updater);

    var updated = List.copyOf(service.listVehicleParkings()).getFirst();
    assertEquals(ID, updated.getId());
    assertEquals(8, updated.getAvailability().getCarSpaces());
    assertNull(updated.getAvailability().getBicycleSpaces());
  }

  @Test
  void updateBicycleAvailability() {
    var service = buildParkingRepository(VehicleParkingSpaces.builder().bicycleSpaces(15).build());
    var updater = new VehicleParkingAvailabilityUpdater(
      PARAMETERS,
      new StubDataSource(DEFAULT_UPDATE),
      service
    );

    runUpdaterOnce(updater);

    var updated = List.copyOf(service.listVehicleParkings()).getFirst();
    assertEquals(ID, updated.getId());
    assertEquals(8, updated.getAvailability().getBicycleSpaces());
    assertNull(updated.getAvailability().getCarSpaces());
  }

  @Test
  void notFound() {
    var service = buildParkingRepository(VehicleParkingSpaces.builder().bicycleSpaces(15).build());
    var updater = new VehicleParkingAvailabilityUpdater(
      PARAMETERS,
      new StubDataSource(new AvailabiltyUpdate(id("not-found"), 100)),
      service
    );

    runUpdaterOnce(updater);

    var updated = List.copyOf(service.listVehicleParkings()).getFirst();
    assertEquals(ID, updated.getId());
    assertNull(updated.getAvailability());
  }

  private static VehicleParkingRepository buildParkingRepository(VehicleParkingSpaces capacity) {
    var repo = new DefaultVehicleParkingRepository();

    var parking = parkingBuilder()
      .carPlaces(capacity.getCarSpaces() != null)
      .bicyclePlaces(capacity.getBicycleSpaces() != null)
      .capacity(capacity)
      .build();
    repo.updateVehicleParking(List.of(parking), List.of());
    return repo;
  }

  private static VehicleParking.VehicleParkingBuilder parkingBuilder() {
    return VehicleParking.builder()
      .id(ID)
      .name(I18NString.of("parking"))
      .coordinate(WgsCoordinate.GREENWICH);
  }

  private void runUpdaterOnce(VehicleParkingAvailabilityUpdater updater) {
    class GraphUpdaterMock extends GraphUpdaterManager {

      private static final Graph GRAPH = new Graph();
      private static final TimetableRepository TRANSIT_MODEL = new TimetableRepository();
      public static final DefaultRealTimeUpdateContext REAL_TIME_UPDATE_CONTEXT =
        new DefaultRealTimeUpdateContext(GRAPH, TRANSIT_MODEL);

      public GraphUpdaterMock(List<GraphUpdater> updaters) {
        super(REAL_TIME_UPDATE_CONTEXT, updaters);
      }

      @Override
      public Future<?> execute(GraphWriterRunnable runnable) {
        runnable.run(REAL_TIME_UPDATE_CONTEXT);
        return Futures.immediateVoidFuture();
      }
    }

    var graphUpdaterManager = new GraphUpdaterMock(List.of(updater));
    graphUpdaterManager.startUpdaters();
    graphUpdaterManager.stop(false);
  }

  private static class StubDataSource implements DataSource<AvailabiltyUpdate> {

    private final AvailabiltyUpdate update;

    private StubDataSource(AvailabiltyUpdate update) {
      this.update = update;
    }

    @Override
    public boolean update() {
      return true;
    }

    @Override
    public List<AvailabiltyUpdate> getUpdates() {
      return List.of(update);
    }
  }
}

package org.opentripplanner.updater.vehicle_parking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.Futures;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.vehicleparking.VehicleParkingRepository;
import org.opentripplanner.service.vehicleparking.VehicleParkingTestGraphData;
import org.opentripplanner.service.vehicleparking.VehicleParkingTestUtil;
import org.opentripplanner.service.vehicleparking.internal.DefaultVehicleParkingRepository;
import org.opentripplanner.service.vehicleparking.model.VehicleParking;
import org.opentripplanner.service.vehicleparking.model.VehicleParkingSpaces;
import org.opentripplanner.service.vehicleparking.model.VehicleParkingState;
import org.opentripplanner.street.model._data.StreetModelForTest;
import org.opentripplanner.street.model.edge.StreetVehicleParkingLink;
import org.opentripplanner.street.model.edge.VehicleParkingEdge;
import org.opentripplanner.street.model.vertex.VehicleParkingEntranceVertex;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.updater.DefaultRealTimeUpdateContext;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.opentripplanner.updater.spi.DataSource;
import org.opentripplanner.updater.spi.GraphUpdater;

class VehicleParkingUpdaterTest {

  private DataSource<VehicleParking> dataSource;
  private Graph graph;
  private DefaultRealTimeUpdateContext realTimeUpdateContext;

  private VehicleParkingUpdater vehicleParkingUpdater;
  private VehicleParkingRepository parkingRepository;

  @BeforeEach
  @SuppressWarnings("unchecked")
  public void setup() {
    VehicleParkingTestGraphData graphData = new VehicleParkingTestGraphData();
    graphData.initGraph();
    graph = graphData.getGraph();
    TimetableRepository timetableRepository = graphData.getTimetableRepository();
    parkingRepository = new DefaultVehicleParkingRepository();
    realTimeUpdateContext = new DefaultRealTimeUpdateContext(graph, timetableRepository);

    dataSource = (DataSource<VehicleParking>) Mockito.mock(DataSource.class);
    when(dataSource.update()).thenReturn(true);

    timetableRepository.index();
    graph.index();

    var parameters = new VehicleParkingUpdaterParameters() {
      @Override
      public VehicleParkingSourceType sourceType() {
        return null;
      }

      @Override
      public UpdateType updateType() {
        return UpdateType.FULL;
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
    vehicleParkingUpdater = new VehicleParkingUpdater(
      parameters,
      dataSource,
      graph.getLinker(),
      parkingRepository
    );
  }

  @Test
  void addVehicleParkingTest() {
    var vehicleParkings = List.of(
      VehicleParkingTestUtil.createParkingWithEntrances("1", 0.0001, 0)
    );

    when(dataSource.getUpdates()).thenReturn(vehicleParkings);
    runUpdaterOnce();

    assertVehicleParkingsInGraph(1);
  }

  @Test
  void updateVehicleParkingTest() {
    var vehiclePlaces = VehicleParkingSpaces.builder().bicycleSpaces(1).build();

    var vehicleParkings = List.of(
      VehicleParkingTestUtil.createParkingWithEntrances("1", 0.0001, 0, vehiclePlaces)
    );

    when(dataSource.getUpdates()).thenReturn(vehicleParkings);
    runUpdaterOnce();

    assertVehicleParkingsInGraph(1);

    var vehicleParkingInGraph = parkingRepository
      .listVehicleParkings()
      .stream()
      .findFirst()
      .orElseThrow();
    assertEquals(vehiclePlaces, vehicleParkingInGraph.getAvailability());
    assertEquals(vehiclePlaces, vehicleParkingInGraph.getCapacity());

    vehiclePlaces = VehicleParkingSpaces.builder().bicycleSpaces(2).build();
    vehicleParkings = List.of(
      VehicleParkingTestUtil.createParkingWithEntrances("1", 0.0001, 0, vehiclePlaces)
    );

    when(dataSource.getUpdates()).thenReturn(vehicleParkings);
    runUpdaterOnce();

    assertVehicleParkingsInGraph(1);

    vehicleParkingInGraph = parkingRepository
      .listVehicleParkings()
      .stream()
      .findFirst()
      .orElseThrow();
    assertEquals(vehiclePlaces, vehicleParkingInGraph.getAvailability());
    assertEquals(vehiclePlaces, vehicleParkingInGraph.getCapacity());
  }

  @Test
  void deleteVehicleParkingTest() {
    var vehicleParkings = List.of(
      VehicleParkingTestUtil.createParkingWithEntrances("1", 0.0001, 0),
      VehicleParkingTestUtil.createParkingWithEntrances("2", -0.0001, 0)
    );

    when(dataSource.getUpdates()).thenReturn(vehicleParkings);
    runUpdaterOnce();

    assertVehicleParkingsInGraph(2);

    vehicleParkings = List.of(VehicleParkingTestUtil.createParkingWithEntrances("1", 0.0001, 0));

    when(dataSource.getUpdates()).thenReturn(vehicleParkings);
    runUpdaterOnce();

    assertVehicleParkingsInGraph(1);
  }

  @Test
  void addNotOperatingVehicleParkingTest() {
    var vehicleParking = StreetModelForTest.vehicleParking()
      .state(VehicleParkingState.CLOSED)
      .build();

    when(dataSource.getUpdates()).thenReturn(List.of(vehicleParking));
    runUpdaterOnce();

    assertEquals(1, parkingRepository.listVehicleParkings().size());
    assertVehicleParkingNotLinked();
  }

  @Test
  void updateNotOperatingVehicleParkingTest() {
    var vehiclePlaces = VehicleParkingSpaces.builder().bicycleSpaces(1).build();

    var vehicleParking = StreetModelForTest.vehicleParking()
      .availability(vehiclePlaces)
      .state(VehicleParkingState.CLOSED)
      .build();

    when(dataSource.getUpdates()).thenReturn(List.of(vehicleParking));
    runUpdaterOnce();

    assertEquals(1, parkingRepository.listVehicleParkings().size());
    assertEquals(
      vehiclePlaces,
      parkingRepository.listVehicleParkings().stream().findFirst().orElseThrow().getAvailability()
    );
    assertVehicleParkingNotLinked();

    vehiclePlaces = VehicleParkingSpaces.builder().bicycleSpaces(2).build();

    vehicleParking = StreetModelForTest.vehicleParking()
      .availability(vehiclePlaces)
      .state(VehicleParkingState.CLOSED)
      .build();

    when(dataSource.getUpdates()).thenReturn(List.of(vehicleParking));
    runUpdaterOnce();

    assertEquals(1, parkingRepository.listVehicleParkings().size());
    assertEquals(
      vehiclePlaces,
      parkingRepository.listVehicleParkings().stream().findFirst().orElseThrow().getAvailability()
    );
    assertVehicleParkingNotLinked();
  }

  @Test
  void deleteNotOperatingVehicleParkingTest() {
    var vehicleParking = StreetModelForTest.vehicleParking()
      .state(VehicleParkingState.CLOSED)
      .build();

    when(dataSource.getUpdates()).thenReturn(List.of(vehicleParking));
    runUpdaterOnce();

    assertEquals(1, parkingRepository.listVehicleParkings().size());

    when(dataSource.getUpdates()).thenReturn(List.of());
    runUpdaterOnce();

    assertEquals(0, parkingRepository.listVehicleParkings().size());
  }

  private void assertVehicleParkingsInGraph(int vehicleParkingNumber) {
    var parkingVertices = graph.getVerticesOfType(VehicleParkingEntranceVertex.class);

    assertEquals(vehicleParkingNumber, parkingVertices.size());

    for (var parkingVertex : parkingVertices) {
      assertEquals(2, parkingVertex.getIncoming().size());
      assertEquals(2, parkingVertex.getOutgoing().size());

      assertEquals(
        1,
        parkingVertex
          .getIncoming()
          .stream()
          .filter(StreetVehicleParkingLink.class::isInstance)
          .count()
      );

      assertEquals(
        1,
        parkingVertex.getIncoming().stream().filter(VehicleParkingEdge.class::isInstance).count()
      );

      assertEquals(
        1,
        parkingVertex
          .getOutgoing()
          .stream()
          .filter(StreetVehicleParkingLink.class::isInstance)
          .count()
      );

      assertEquals(
        1,
        parkingVertex.getOutgoing().stream().filter(VehicleParkingEdge.class::isInstance).count()
      );
    }

    assertEquals(vehicleParkingNumber, parkingRepository.listVehicleParkings().size());
  }

  private void runUpdaterOnce() {
    class GraphUpdaterMock extends GraphUpdaterManager {

      public GraphUpdaterMock(List<GraphUpdater> updaters) {
        super(realTimeUpdateContext, updaters);
      }

      @Override
      public Future<?> execute(GraphWriterRunnable runnable) {
        runnable.run(realTimeUpdateContext);
        return Futures.immediateVoidFuture();
      }
    }

    var graphUpdaterManager = new GraphUpdaterMock(List.of(vehicleParkingUpdater));
    graphUpdaterManager.startUpdaters();
    graphUpdaterManager.stop(false);
  }

  private void assertVehicleParkingNotLinked() {
    assertEquals(0, graph.getVerticesOfType(VehicleParkingEntranceVertex.class).size());
    assertEquals(0, graph.getEdgesOfType(StreetVehicleParkingLink.class).size());
    assertEquals(0, graph.getEdgesOfType(VehicleParkingEdge.class).size());
  }
}

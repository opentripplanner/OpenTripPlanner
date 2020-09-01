package org.opentripplanner.routing.edgetype.rentedgetype;

import com.sun.tools.javac.util.List;
import org.junit.Before;
import org.opentripplanner.graph_builder.linking.TemporaryStreetSplitter;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.core.vehicle_sharing.*;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.updater.vehicle_sharing.vehicles_positions.BikeStationsGraphWriterRunnable;

import static org.mockito.Mockito.mock;

public class AddBikeStationsToGraphTest {
    private static final CarDescription CAR_1 = new CarDescription("1", 0, 0, FuelType.ELECTRIC, Gearbox.AUTOMATIC, new Provider(1, "PANEK"));

    private static final BikeRentalStation station11 = new BikeRentalStation("11", 0, 0, 1, 1, new Provider(1, "provider1"));
    private static final BikeRentalStation station12 = new BikeRentalStation("12", 1, 1, 1, 1, new Provider(1, "provider1"));

    private static final BikeRentalStation station21 = new BikeRentalStation("21", 0, 0, 1, 1, new Provider(1, "provider2"));

    private RoutingRequest request;
    private State state, rentingState;
    private BikeDescription bike1;

    private Graph graph = new Graph();

    @Before
    public void setUp() {
        TemporaryStreetSplitter temporaryStreetSplitter = TemporaryStreetSplitter.createNewDefaultInstance(graph, null, null);

        request = new RoutingRequest();
        request.setDummyRoutingContext(graph);
        request.setModes(new TraverseModeSet(TraverseMode.WALK, TraverseMode.BICYCLE));
        request.setStartingMode(TraverseMode.WALK);

        request.vehicleValidator = mock(VehicleValidator.class);
        request.rentingAllowed = true;


        Vertex v1 = new IntersectionVertex(graph, "v1", 0, 0, "v1");
        Vertex v2 = new IntersectionVertex(graph, "v2", 1, 1, "v2");
        Vertex v3 = new IntersectionVertex(graph, "v3", 2, 2, "v3");
        state = new State(v1, request);

        bike1 = station11.getBikeFromStation();


        BikeStationsGraphWriterRunnable graphRunnable = new BikeStationsGraphWriterRunnable(temporaryStreetSplitter, List.of(station11, station21));

        graphRunnable.run(graph);


    }

//    @Test
//    public void switchFromAnotherVehicle(){
//        //when
//        StateEditor se = state.edit(rentEdge11);
//        se.beginVehicleRenting(CAR_1);
//        rentingState = se.makeState();
//        when(request.vehicleValidator.isValid(bike1)).thenReturn(true);
//
//        //given
//        State traversed = rentEdge11.traverse(rentingState);
//        //then
//        assertNotNull(traversed);
////Chce przetestowac przyjechanie autkiem na stacje rowerowoą i wzięcie roweru.
//
//    }
}

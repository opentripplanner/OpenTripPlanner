package org.opentripplanner.updater.vehicle_sharing.vehicles_positions;

import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.CoordinateXY;
import org.opentripplanner.graph_builder.linking.TemporaryStreetSplitter;
import org.opentripplanner.routing.core.vehicle_sharing.CarDescription;
import org.opentripplanner.routing.core.vehicle_sharing.FuelType;
import org.opentripplanner.routing.core.vehicle_sharing.Gearbox;
import org.opentripplanner.routing.core.vehicle_sharing.Provider;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.TemporaryRentVehicleVertex;

import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class VehicleSharingGraphWriterRunnableTest {

    private static final CarDescription CAR_1 = new CarDescription("1", 0, 0, FuelType.ELECTRIC, Gearbox.AUTOMATIC, new Provider(2, "PANEK"));
    private static final CarDescription CAR_2 = new CarDescription("2", 1, 1, FuelType.ELECTRIC, Gearbox.AUTOMATIC, new Provider(2, "PANEK"));

    private Graph graph;

    private TemporaryStreetSplitter temporaryStreetSplitter;

    private TemporaryRentVehicleVertex vertex;

    @Before
    public void setUp() {
        graph = new Graph();

        temporaryStreetSplitter = mock(TemporaryStreetSplitter.class);

        vertex = new TemporaryRentVehicleVertex("id", new CoordinateXY(1, 2), "name");
    }

    @Test
    public void shouldAddAppearedRentableVehicles() {
        // given
        when(temporaryStreetSplitter.linkRentableVehicleToGraph(CAR_1)).thenReturn(Optional.of(vertex));
        VehicleSharingGraphWriterRunnable runnable = new VehicleSharingGraphWriterRunnable(temporaryStreetSplitter, singletonList(CAR_1));

        // when
        runnable.run(graph);

        // then
        assertEquals(1, graph.vehiclesTriedToLink.size());
        assertTrue(graph.vehiclesTriedToLink.containsKey(CAR_1));
        verify(temporaryStreetSplitter, times(1)).linkRentableVehicleToGraph(CAR_1);
        verifyNoMoreInteractions(temporaryStreetSplitter);
    }

    @Test
    public void shouldRemoveDisappearedRentableVehicles() {
        // given
        graph.vehiclesTriedToLink.put(CAR_1, Optional.of(vertex));
        VehicleSharingGraphWriterRunnable runnable = new VehicleSharingGraphWriterRunnable(temporaryStreetSplitter, emptyList());

        // when
        runnable.run(graph);

        // then
        assertTrue(graph.vehiclesTriedToLink.isEmpty());
        verifyZeroInteractions(temporaryStreetSplitter);
    }

    @Test
    public void shouldPreserveExistingRentableVehicles() {
        // given
        graph.vehiclesTriedToLink.put(CAR_1, Optional.of(vertex));
        VehicleSharingGraphWriterRunnable runnable = new VehicleSharingGraphWriterRunnable(temporaryStreetSplitter, singletonList(CAR_1));

        // when
        runnable.run(graph);

        // then
        assertEquals(1, graph.vehiclesTriedToLink.size());
        assertTrue(graph.vehiclesTriedToLink.containsKey(CAR_1));
        verifyZeroInteractions(temporaryStreetSplitter);
    }
}

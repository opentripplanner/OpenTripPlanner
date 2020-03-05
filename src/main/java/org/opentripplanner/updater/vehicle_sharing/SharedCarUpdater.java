package org.opentripplanner.updater.vehicle_sharing;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.rentedgetype.RentCarAnywhereEdge;
import org.opentripplanner.routing.edgetype.rentedgetype.RentVehicleAnywhereEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.opentripplanner.updater.PollingGraphUpdater;

import java.util.LinkedList;
import java.util.List;

public class SharedCarUpdater extends PollingGraphUpdater {
    GraphUpdaterManager graphUpdaterManager;
    VehiclePositionsGetter vehiclePositionsGetter = new VehiclePositionsGetter(TraverseMode.CAR);
    Graph graph;

    private List<Vertex> coordsToVertex(List<VehiclePosition> vehiclePositions) {
//        TODO
        return null;
    }

    @Override
    protected void runPolling() throws Exception {
        VehiclePostionsDiff vehiclePostionsDiff = vehiclePositionsGetter.getVehiclePositionsDiff();
        List<Vertex> appearedVertex = coordsToVertex(vehiclePostionsDiff.appeared);
        List<Vertex> disappearedVertex = coordsToVertex(vehiclePostionsDiff.disappeared);
        List<Edge> apppearedEdge = new LinkedList<>();
        List<Edge> disappeareedEdge = new LinkedList<>();

        for (Vertex vertex : disappearedVertex) {
            for (Edge edge : vertex.getOutgoing()) {
                if (edge instanceof RentCarAnywhereEdge) {
                    disappeareedEdge.add(edge);
                }
            }
        }

        for (Vertex vertex : appearedVertex) {
            for (Edge edge : vertex.getOutgoing()) {
                if (edge instanceof RentCarAnywhereEdge) {
                    apppearedEdge.add(edge);
                }
            }
        }

        VehicleSharingGraphWritterRunnable graphWritterRunnable = new VehicleSharingGraphWritterRunnable(apppearedEdge,disappeareedEdge);
        graphUpdaterManager.execute(graphWritterRunnable);
    }

    @Override
    protected void configurePolling(Graph graph, JsonNode config) throws Exception {

    }

    @Override
    public void setGraphUpdaterManager(GraphUpdaterManager updaterManager) {
        this.graphUpdaterManager = updaterManager;
    }

    @Override
    public void setup(Graph graph) throws Exception {
        this.graph = graph;

    }

    @Override
    public void teardown() {

    }

}

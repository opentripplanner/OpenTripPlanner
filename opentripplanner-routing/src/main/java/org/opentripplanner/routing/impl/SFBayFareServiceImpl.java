package org.opentripplanner.routing.impl;

import java.util.ArrayList;
import java.util.List;

import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.services.GtfsRelationalDao;
import org.opentripplanner.routing.core.Fare;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.services.FareService;
import org.opentripplanner.routing.services.FareServiceFactory;
import org.opentripplanner.routing.spt.GraphPath;

public class SFBayFareServiceImpl implements FareService {

    public static final int SFMTA_TRANSFER_DURATION = 60 * 60 * 2; // ?
    
    @Override
    public Fare getCost(GraphPath path) {
        Long sfmtaTransferIssueTime = null;
        Edge lastEdge = null;
        TraverseMode lastMode = null;
        Stop firstStop = null;
        for (Edge edge : path.edges) {
            TraverseMode mode = edge.getMode();
            Trip trip = edge.getTrip();
            Route route = trip.getRoute();
            Agency agency = route.getAgency();
            System.out.println(mode);
            System.out.println(trip);
            System.out.println(route);
            System.out.println(agency);
            System.out.println("---");
        }
        Fare fare = new Fare();
        return fare;
    }

    public static class Factory implements FareServiceFactory {
        @Override
        public FareService makeFareService() { return new SFBayFareServiceImpl(); }
        @Override
        public void setDao(GtfsRelationalDao dao) { /* do nothing */ }
    }
}

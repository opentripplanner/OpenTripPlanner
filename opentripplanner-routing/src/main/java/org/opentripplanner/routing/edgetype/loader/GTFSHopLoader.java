package org.opentripplanner.routing.edgetype.loader;

import java.util.ArrayList;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.services.GtfsRelationalDao;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.Alight;
import org.opentripplanner.routing.edgetype.Board;
import org.opentripplanner.routing.edgetype.DrawHandler;
import org.opentripplanner.routing.edgetype.Hop;
import org.opentripplanner.routing.edgetype.factory.GTFSHopFactory;
import org.opentripplanner.routing.vertextypes.TransitStop;

public class GTFSHopLoader {

    private Graph _graph;

    private GtfsRelationalDao _dao;

    private GtfsContext _context;

    public GTFSHopLoader(Graph graph, GtfsContext context) {
        _graph = graph;
        _context = context;
        _dao = context.getDao();
    }

    public void load(DrawHandler drawHandler, boolean verbose) throws Exception {

        // Load stops
        for (Stop stop : _dao.getAllStops()) {
            Vertex vertex = _graph.addVertex(new Vertex(id(stop.getId()), stop.getLon(),
                    stop.getLat()));
            vertex.type = TransitStop.class;
        }

        // Load hops
        if (verbose) {
            System.out.println("Loading hops");
        }
        GTFSHopFactory hf = new GTFSHopFactory(_context);
        ArrayList<Hop> hops = hf.run(verbose);
        for (Hop hop : hops) {
            if (drawHandler != null) {
                drawHandler.handle(hop);
            }
            StopTime startStopTime = hop.getStartStopTime();
            StopTime endStopTime = hop.getEndStopTime();
            Stop startStop = startStopTime.getStop();
            Stop endStop = endStopTime.getStop();
            Vertex startStation = _graph.getVertex(id(startStop.getId()));
            Vertex endStation = _graph.getVertex(id(endStop.getId()));

            // create journey vertices
            Vertex startJourney = _graph.addVertex(id(startStop.getId()) + "_"
                    + id(startStopTime.getTrip().getId()), startStop.getLon(), startStop.getLat());
            Vertex endJourney = _graph.addVertex(id(endStop.getId()) + "_"
                    + id(endStopTime.getTrip().getId()), endStop.getLon(), endStop.getLat());

            Board boarding = new Board(hop);
            _graph.addEdge(startStation, startJourney, boarding);
            _graph.addEdge(endJourney, endStation, new Alight());

            _graph.addEdge(startJourney, endJourney, hop);

        }
    }

    public void load(DrawHandler drawHandler) throws Exception {
        load(drawHandler, false);
    }

    public void load() throws Exception {
        load(null);
    }

    private String id(AgencyAndId id) {
        return id.getAgencyId() + "_" + id.getId();
    }

}

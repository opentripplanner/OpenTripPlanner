package org.opentripplanner.jags.edgetype.loader;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.services.GtfsRelationalDao;

import org.opentripplanner.jags.core.Graph;
import org.opentripplanner.jags.core.SpatialVertex;
import org.opentripplanner.jags.core.Vertex;
import org.opentripplanner.jags.edgetype.Alight;
import org.opentripplanner.jags.edgetype.Board;
import org.opentripplanner.jags.edgetype.DrawHandler;
import org.opentripplanner.jags.edgetype.Hop;
import org.opentripplanner.jags.edgetype.factory.GTFSHopFactory;
import org.opentripplanner.jags.gtfs.GtfsContext;
import org.opentripplanner.jags.vertextypes.TransitStop;

import java.util.ArrayList;

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
            Vertex vertex = _graph.addVertex(new SpatialVertex(id(stop.getId()), stop.getLon(),
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
            Vertex startStation = _graph.getVertex(id(startStopTime.getStop().getId()));
            Vertex endStation = _graph.getVertex(id(endStopTime.getStop().getId()));

            // create journey vertices
            Vertex startJourney = _graph.addVertex(id(startStopTime.getStop().getId()) + "_"
                    + id(startStopTime.getTrip().getId()));
            Vertex endJourney = _graph.addVertex(id(endStopTime.getStop().getId()) + "_"
                    + id(endStopTime.getTrip().getId()));

            // FIXME: do not create board/alight edges for last/first stop
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

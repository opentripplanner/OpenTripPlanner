package org.opentripplanner.routing.edgetype.loader;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.services.GtfsRelationalDao;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.factory.GTFSPatternHopFactory;
import org.opentripplanner.routing.vertextypes.TransitStop;

public class GTFSPatternHopLoader {

    private Graph _graph;

    private GtfsRelationalDao _dao;

    private GtfsContext _context;

    public GTFSPatternHopLoader(Graph graph, GtfsContext context) {
        _graph = graph;
        _context = context;
        _dao = context.getDao();
    }

    public void load(boolean verbose) throws Exception {

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
        GTFSPatternHopFactory hf = new GTFSPatternHopFactory(_context);
        hf.run(_graph);
    }

    private String id(AgencyAndId id) {
        return id.getAgencyId() + "_" + id.getId();
    }

    public void load() throws Exception {
        load(false);
    }

}

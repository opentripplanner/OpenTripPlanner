package org.opentripplanner.updater.transportation_network_company;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.transportation_network_company.TransportationNetworkCompanyService;
import org.opentripplanner.updater.GraphUpdater;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.transportation_network_company.lyft.LyftTransportationNetworkCompanyDataSource;
import org.opentripplanner.updater.transportation_network_company.uber.UberTransportationNetworkCompanyDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransportationNetworkCompanyUpdater implements GraphUpdater {

    private static Logger LOG = LoggerFactory.getLogger(TransportationNetworkCompanyUpdater.class);

    private GraphUpdaterManager updaterManager;
    private TransportationNetworkCompanyDataSource source;
    private TransportationNetworkCompanyService service;

    @Override
    public void setGraphUpdaterManager(GraphUpdaterManager updaterManager) {
        this.updaterManager = updaterManager;
    }

    @Override
    public void setup(Graph graph) {
        service = graph.getService(TransportationNetworkCompanyService.class, true);
    }

    @Override
    public void run() throws Exception {
        // Adds the source upon startup
        service.addSource(source);
    }

    @Override
    public void teardown() { }

    @Override
    public void configure(Graph graph, JsonNode config) throws Exception {
        // Set data source type from config JSON
        String sourceType = config.path("sourceType").asText();
        if (sourceType != null) {
            if (sourceType.equals("uber")) {
                source = new UberTransportationNetworkCompanyDataSource(config);
            } else if (sourceType.equals("lyft")) {
                source = new LyftTransportationNetworkCompanyDataSource(config);
            } else if (sourceType.equals("no-api")) {
                source = new NoApiTransportationNetworkCompanyDataSource(config);
            }
        }

        if (source == null) {
            throw new IllegalArgumentException("Unknown transportation netowrk company source type: " + sourceType);
        }

        LOG.info("Setup a transportation netowrk company updater for type: " + sourceType);
    }
}
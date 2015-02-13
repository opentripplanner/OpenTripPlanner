package org.opentripplanner.standalone;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.servlet.ReflectiveQueryScraper;

/**
 * These are parameters that can be applied to an individual Router at run time.
 * These settings could conceivably even be reloaded in a running server if they have changed.
 * They are distinct from the GraphBuilderParameters which must be applied ahead of time when the graph is being built.
 * However, eventually both classes may be initialized from the same config file so make sure there is no overlap
 * in the JSON keys used.
 */
public class RouterParameters {

    RoutingRequest prototypeRoutingRequest;

    /**
     * Set all parameters from the given Jackson JSON tree, applying defaults.
     * Supplying MissingNode.getInstance() will cause all the defaults to be applied.
     */
    public RouterParameters (JsonNode config) {
        ReflectiveQueryScraper<RoutingRequest> scraper = new ReflectiveQueryScraper(RoutingRequest.class);
        prototypeRoutingRequest = scraper.scrape(config.path("prototypeRoutingRequest"));
    }

}

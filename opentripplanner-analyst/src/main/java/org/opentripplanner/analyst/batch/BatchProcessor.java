package org.opentripplanner.analyst.batch;

import java.io.IOException;
import java.util.TimeZone;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.opentripplanner.analyst.batch.aggregator.Aggregator;
import org.opentripplanner.analyst.request.Renderer;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.SPTService;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

public class BatchProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(BatchProcessor.class);
    private static final String CONFIG = "batch-context.xml";
    
    @Autowired GraphService graphService;
    @Autowired SPTService sptService;
    @Resource Population origins;
    @Resource Population destinations;
    
    private TraverseModeSet modes = new TraverseModeSet("WALK,TRANSIT");
    private final String DATE = "2011-02-04";
    private static final String TIME = "08:00 AM";
    private static final TimeZone TIMEZONE = TimeZone.getDefault();

    static void main(String[] args) throws IOException {

        GenericApplicationContext ctx = new GenericApplicationContext();
        XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader(ctx);
        xmlReader.loadBeanDefinitions(new ClassPathResource(CONFIG));
        ctx.refresh();
        ctx.registerShutdownHook();
        //ctx.getBean(BatchProcessor.class).run();
    }

    // actually probably better to run explicitly, so batch processor will do nothing unless it
    // is run intentionally
    @PostConstruct
    private void runAfterSetters() {
        /* to cover several kinds of batch requests, there are two modes: agg and non-agg.
         * The batch processor chooses a mode based on whether the aggregator property has been
         * set or not.
         * AggregateOrigins=false
         * 
         * In either mode, both the source and target population properties must be set.
         * The batch analysis is always carried out as a loop over the source set.
         * 
         * In aggregate mode, the supplied aggregate function is evaluated over the target set for 
         * every element of the source set. The resulting aggregate value is associated with the
         * origin individual that produced it, and the entire set of aggregates are saved together 
         * in a format appropriate for that population type. 
         * Thus, aggregate mode produces a single output object/stream/buffer, containing one unit 
         * of output (tuple/line/pixel) per individual in the source set.
         * 
         * In non-aggregate mode, one output object/stream/buffer is produced per source location.
         * Thus, for S sources and D destinations, S output objects will be produced, each
         * containing D data items.
         * 
         * default TraverseOptions can be supplied.
         * 
         * Aggregate over origins or destinations option
         */
        for (Individual oi : origins) {
            RoutingRequest req = buildRequest(oi.getLat(), oi.getLon());
            ShortestPathTree spt = sptService.getShortestPathTree(req);
            destinations.writeCsv("/home/abyrd/disagg.csv", spt, oi);
//            for (Individual di : destinations) {
//                long travelTime = di.sample.eval(spt);
//                // if an aggregator is defined over 
//            }
            req.cleanup();
        }
    }
    
    private RoutingRequest buildRequest(double lat, double lon) {
        RoutingRequest req = new RoutingRequest();
        req.setDateTime(DATE, TIME, TIMEZONE);
        req.setFrom(String.format("%f, %f", lat, lon));
        req.batch = true;
        req.setRoutingContext(graphService.getGraph());
        return req;
    }

}


package org.opentripplanner.analyst.batch;

import java.io.IOException;
import java.util.TimeZone;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import lombok.Data;

import org.opentripplanner.analyst.batch.aggregator.Aggregator;
import org.opentripplanner.routing.core.PrototypeRoutingRequest;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.error.VertexNotFoundException;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.SPTService;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;

@Data
public class BatchProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(BatchProcessor.class);
    private static final String CONFIG = "batch-context.xml";
    
    @Autowired private GraphService graphService;
    @Autowired private SPTService sptService;
    @Resource private Population origins;
    @Resource private Population destinations;
    private PrototypeRoutingRequest prototypeRoutingRequest;
    private Aggregator aggregator;
    private Accumulator accumulator;
    
    private String routerId;
    private String date = "2011-02-04";
    private String time = "08:00 AM";
    private TimeZone timeZone = TimeZone.getDefault();
    private TraverseModeSet modes = new TraverseModeSet("WALK,TRANSIT");
    private String outputPath;

    public static void main(String[] args) throws IOException {

        GenericApplicationContext ctx = new GenericApplicationContext();
        XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader(ctx);
        xmlReader.loadBeanDefinitions(new ClassPathResource(CONFIG));
        ctx.refresh();
        ctx.registerShutdownHook();
        ctx.getBean(BatchProcessor.class).run();
    }

    private void run() {
        int nOrigins = origins.getIndividuals().size();
        if (aggregator != null) {
            ResultSet aggregates = new ResultSet(origins);
            int i = 0;
            for (Individual oi : origins) {
                LOG.debug("individual {}: {}", i, oi);
                if (i%100 == 0)
                    LOG.info("individual {}/{}", i, nOrigins);
                RoutingRequest req = buildRequest(oi);
                if (req != null) {
                    ShortestPathTree spt = sptService.getShortestPathTree(req);
                    ResultSet result = ResultSet.forTravelTimes(destinations, spt);
                    aggregates.results[i] = aggregator.computeAggregate(result);
                    req.cleanup();
                }
                i += 1;
            }
            aggregates.writeAppropriateFormat(outputPath);
        } else if (accumulator != null) {
            ResultSet accumulated = new ResultSet(destinations);
            int i = 0;
            for (Individual oi : origins) {
                LOG.debug("individual {}: {}", i, oi);
                if (i%100 == 0)
                    LOG.info("individual {}/{}", i, nOrigins);
                RoutingRequest req = buildRequest(oi);
                if (req != null) {
                    ShortestPathTree spt = sptService.getShortestPathTree(req);
                    ResultSet times = ResultSet.forTravelTimes(destinations, spt);
                    accumulator.accumulate(oi.input, times, accumulated);
                    req.cleanup();
                }
                i += 1;
            }
            accumulator.finish();
            accumulated.writeAppropriateFormat(outputPath);
        } else { 
            // neither aggregator nor accumlator
            if (nOrigins > 1 && !outputPath.contains("{}")) {
                LOG.error("output filename must contain origin placeholder.");
                return;
            }
            int i = 0;
            for (Individual oi : origins) {
                RoutingRequest req = buildRequest(oi);
                if (req != null) {
                    ShortestPathTree spt = sptService.getShortestPathTree(req);
                    ResultSet result = ResultSet.forTravelTimes(destinations, spt);
                    if (nOrigins == 1) {
                        result.writeAppropriateFormat(outputPath);
                    } else {
                        String subName = outputPath.replace("{}", String.format("%d_%s", i, oi.label));
                        result.writeAppropriateFormat(subName);
                    }
                    req.cleanup();
                    i += 1;
                }
            }
        }
    }
    
    private RoutingRequest buildRequest(Individual i) {
        RoutingRequest req = prototypeRoutingRequest.clone();
        req.setRouterId(routerId);
        req.setDateTime(date, time, timeZone);
        String latLon = String.format("%f,%f", i.getLat(), i.getLon());
        req.batch = true;
        if (req.arriveBy)
            req.setTo(latLon);
        else
            req.setFrom(latLon);
        try {
            req.setRoutingContext(graphService.getGraph(routerId));
            return req;
        } catch (VertexNotFoundException vnfe) {
            LOG.debug("no vertex could be created near the origin point");
            return null;
        }
    }

}


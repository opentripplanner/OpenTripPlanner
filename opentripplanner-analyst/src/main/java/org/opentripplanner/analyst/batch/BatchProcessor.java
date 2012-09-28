package org.opentripplanner.analyst.batch;

import java.io.IOException;
import java.util.TimeZone;

import javax.annotation.Resource;

import lombok.Setter;

import org.opentripplanner.analyst.batch.aggregator.Aggregator;
import org.opentripplanner.analyst.core.Sample;
import org.opentripplanner.analyst.request.SampleFactory;
import org.opentripplanner.routing.core.RoutingRequest;
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
import org.springframework.core.io.FileSystemResource;

public class BatchProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(BatchProcessor.class);
    private static final String EXAMPLE_CONTEXT = "batch-context.xml";
    
    @Autowired private GraphService graphService;
    @Autowired private SPTService sptService;
    @Autowired private SampleFactory sampleFactory;

    @Resource private Population origins;
    @Resource private Population destinations;
    @Resource private RoutingRequest prototypeRoutingRequest;

    @Setter private Aggregator aggregator;
    @Setter private Accumulator accumulator;
    
    @Setter private String date = "2011-02-04";
    @Setter private String time = "08:00 AM";
    @Setter private TimeZone timeZone = TimeZone.getDefault();
    @Setter private String outputPath = "/tmp/analystOutput";

    public static void main(String[] args) throws IOException {
        org.springframework.core.io.Resource appContextResource;
        if( args.length == 0) {
            LOG.warn("no configuration XML file specified; using example on classpath");
            appContextResource = new ClassPathResource(EXAMPLE_CONTEXT);
        } else {
            String configFile = args[0];
            appContextResource = new FileSystemResource(configFile);
        }
        GenericApplicationContext ctx = new GenericApplicationContext();
        XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader(ctx);
        xmlReader.loadBeanDefinitions(appContextResource);
        ctx.refresh();
        ctx.registerShutdownHook();
        BatchProcessor processor = ctx.getBean(BatchProcessor.class);
        if (processor == null)
            LOG.error("No BatchProcessor bean was defined.");
        else
            processor.run();
    }

    private void run() {

        origins.setup();
        destinations.setup();
        linkIntoGraph(destinations);
        
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
        req.setDateTime(date, time, timeZone);
        // TODO PARAMETERIZE
        req.worstTime = req.dateTime + 3600;
        String latLon = String.format("%f,%f", i.lat, i.lon);
        req.batch = true;
        if (req.arriveBy)
            req.setTo(latLon);
        else
            req.setFrom(latLon);
        try {
            req.setRoutingContext(graphService.getGraph(req.routerId));
            return req;
        } catch (VertexNotFoundException vnfe) {
            LOG.debug("no vertex could be created near the origin point");
            return null;
        }
    }
    
    /** 
     * Generate samples for (i.e. non-invasively link into the Graph) only those individuals that 
     * were not rejected by filters. Other Individuals will have null samples, indicating that they 
     * should be skipped.
     */
    private void linkIntoGraph(Population p) {
        LOG.info("linking population {} to the graph...", p);
        int n = 0, nonNull = 0;
        for (Individual i : p) {
            Sample s = sampleFactory.getSample(i.lon, i.lat);
            i.sample = s;
            n += 1;
            if (s != null)
                nonNull += 1;
        }
        LOG.debug("successfully linked {} individuals out of {}", nonNull, n);
    }

}


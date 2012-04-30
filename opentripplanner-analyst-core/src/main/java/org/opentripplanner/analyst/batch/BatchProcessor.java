package org.opentripplanner.analyst.batch;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.opentripplanner.analyst.batch.aggregator.Aggregator;
import org.opentripplanner.analyst.request.Renderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

public class BatchProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(BatchProcessor.class);
    private static final String CONFIG = "application-context.xml";
    private static final String BATCH_CONFIG = "batch-context.xml";

    @Autowired Renderer renderer;
    @Resource Population origins;
    @Resource Population destinations;
    @Resource Aggregator aggregator;
    private boolean aggregate = false;
    
    public static void main(String[] args) throws IOException {

        GenericApplicationContext ctx = new GenericApplicationContext();
        XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader(ctx);
        xmlReader.loadBeanDefinitions(new ClassPathResource(CONFIG));
        xmlReader.loadBeanDefinitions(new ClassPathResource(BATCH_CONFIG));
        ctx.refresh();
        ctx.registerShutdownHook();
        //ctx.getBean(BatchProcessor.class).run();
    }

    // actually probably better to run explicitly, so batch processor will do nothing unless it
    // is run intentionally
    @PostConstruct
    private void runAfterSetters() {
        LOG.info("Hello, this is the batch processor. My renderer is: {}", renderer);
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
    }

}

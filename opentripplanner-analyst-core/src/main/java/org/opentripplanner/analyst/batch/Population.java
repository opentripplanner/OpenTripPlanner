package org.opentripplanner.analyst.batch;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

import org.opentripplanner.analyst.core.Sample;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A collection of individual locations that will be used as either the origin set or the 
 * destination set in a many-to-many search.
 * 
 * @author andrewbyrd
 */
public class Population extends ArrayList<Individual> {
    
    private static final long serialVersionUID = 20120201L;
    
    private static final Logger LOG = LoggerFactory.getLogger(Population.class);
    
    public Population(Individual... individuals) {
        super(Arrays.asList(individuals));
    }
    
    public void writeCsv(String outFileName, ShortestPathTree spt) {
        LOG.debug("Writing population to CSV: {}", outFileName);
        File outFile = new File(outFileName);
        PrintWriter csvWriter;
        try {
            csvWriter = new PrintWriter(outFile);
            csvWriter.printf("data;traveltime\n");
            for (Individual i : this) {
                Sample s = i.sample;
                long t = Long.MAX_VALUE;
                if (s != null)
                    t = s.eval(spt);
                csvWriter.printf("%f;%d\n", i.data, t);
            }
            csvWriter.close();
        } catch (Exception e) {
            LOG.debug("error writing population to CSV: {}", e);
        }
        LOG.debug("Done writing population to CSV.");
    }
    
}

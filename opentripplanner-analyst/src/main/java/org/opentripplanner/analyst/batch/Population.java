package org.opentripplanner.analyst.batch;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.opentripplanner.analyst.core.Sample;
import org.opentripplanner.common.geometry.DistanceLibrary;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A collection of individual locations that will be used as either the origin set or the 
 * destination set in a many-to-many search.
 * 
 * @author andrewbyrd
 */
public class Population implements Iterable<Individual> {
    
    private static final long serialVersionUID = 20120201L;
    
    private static final Logger LOG = LoggerFactory.getLogger(Population.class);
    
    List<Individual> individuals = new ArrayList<Individual>(); 
    
    public Population() {    }

    public Population(Individual... individuals) {
        this.individuals = Arrays.asList(individuals);
    }
    
    public void setIndividuals(List<Individual> individuals) {
        this.individuals = individuals;
    }
    
    public void add(Individual individual) {
        this.individuals.add(individual);
    }

    public void writeCsv(String outFileName, ShortestPathTree spt, Individual origin) {
        LOG.debug("Writing population to CSV: {}", outFileName);
        File outFile = new File(outFileName);
        PrintWriter csvWriter;
        try {
            csvWriter = new PrintWriter(outFile);
            csvWriter.printf("lat,lon,data,traveltime,birdfly\n");
            for (Individual i : this.individuals) {
                Sample s = i.sample;
                long t = Long.MAX_VALUE;
                //double birdfly = DistanceLibrary.distance(origin.getLat(), origin.getLon(), i.getLat(), i.getLon());
                if (s != null)
                    t = s.eval(spt);
                csvWriter.printf("%f,%f,%f,%d,%f\n", i.getLat(), i.getLon(), i.data, t);
            }
            csvWriter.close();
        } catch (Exception e) {
            LOG.debug("error writing population to CSV: {}", e);
        }
        LOG.debug("Done writing population to CSV.");
    }

    @Override
    public Iterator<Individual> iterator() {
        return this.individuals.iterator();
    }
    
}

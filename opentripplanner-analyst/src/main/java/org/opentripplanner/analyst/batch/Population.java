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
 * A collection of individual locations that will be used as either the origin
 * set or the destination set in a many-to-many search.
 * 
 * @author andrewbyrd
 */
public interface Population extends Iterable<Individual> {

    public List<Individual> getIndividuals();
    
    public void setIndividuals(List<Individual> individuals);
    
    public void add(Individual individual);
    
    public void setOutputToTravelTime(ShortestPathTree spt, Individual origin); // output mode enum?
    
    public void writeCsv(String outFileName);

    // loadIndividuals method to be called by batch processor before running 
}

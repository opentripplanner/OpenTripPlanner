package org.opentripplanner.analyst.batch;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.annotation.PostConstruct;

import org.opentripplanner.analyst.core.Sample;
import org.opentripplanner.common.geometry.DistanceLibrary;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A collection of individual locations that will be used as either the origin set or the 
 * destination set in a many-to-many (batch Analyst) search.
 * 
 * @author andrewbyrd
 */
public interface Population extends Iterable<Individual> {

    public List<Individual> getIndividuals();

    public void setIndividuals(List<Individual> individuals);

    public void clearIndividuals(List<Individual> individuals);

    public void addIndividual(Individual individual);

    /**
     * Prepare the population for use. This includes loading or generating the individuals, 
     * filtering them, and 
     */
    public void setup();

    /**
     * Subclass-specific method to load the individuals from a file or create them based on other 
     * properties. This method should fill in all fields of each individual except the sample, 
     * since sampling will be carried out after the filter chain is applied.
     */
    public void createIndividuals();

    /** @return the number of individuals in this population. */
    public int size();
    
    /**
     * Save the output data in this population to a file, using a format that is appropriate for the specific class of population. For example, a
     * population loaded from an image file or generated on a regular grid will be saved as a Geotiff raster. A population of points that are not
     * known to be aligned on a regular grid in some CRS will be saved as a CSV file.
     */
    public void writeAppropriateFormat(String fileName, ResultSet results);

}

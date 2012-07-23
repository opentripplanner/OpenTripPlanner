package org.opentripplanner.analyst.batch;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.annotation.PostConstruct;

import lombok.Getter;
import lombok.Setter;

import org.opentripplanner.analyst.core.Sample;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class BasicPopulation implements Population {
	    
    private static final Logger LOG = LoggerFactory.getLogger(BasicPopulation.class);
    
    @Setter 
    public String sourceFilename;
    
    @Setter @Getter 
    public List<Individual> individuals = new ArrayList<Individual>(); 
    
    @Autowired 
    IndividualFactory individualFactory;
    
    public BasicPopulation() {  }

    public BasicPopulation(Individual... individuals) {
        this.individuals = Arrays.asList(individuals);
    }
    
    public BasicPopulation(Collection<Individual> individuals) {
        this.individuals = new ArrayList<Individual>(individuals);
    }

    @Override 
    public void addIndividual(Individual individual) {
        this.individuals.add(individual);
    }

    @Override 
    public Iterator<Individual> iterator() {
        return this.individuals.iterator();
    }

    @Override
    public void clearIndividuals(List<Individual> individuals) {
        //
    }

    @Override @PostConstruct
    public void createIndividuals() {
        // nothing to do in basic population case
    }

    @Override
    public int size() {
        return this.individuals.size();
    }
        
    protected void writeCsv(String outFileName, ResultSet results) {
        LOG.debug("Writing population to CSV: {}", outFileName);
        File outFile = new File(outFileName);
        PrintWriter csvWriter;
        try {
            csvWriter = new PrintWriter(outFile);
            csvWriter.printf("label,lat,lon,input,output\n"); // output could be travel time when aggregator not present
            int i = 0;
            for (Individual indiv : this) {
                csvWriter.printf("%s,%f,%f,%f,%f\n", indiv.label, indiv.lat, indiv.lon, indiv.input, results.results[i]);
            }
            csvWriter.close();
        } catch (Exception e) {
            LOG.debug("error writing population to CSV: {}", e);
        }
        LOG.debug("Done writing population to CSV.");
    }

    @Override
    public void writeAppropriateFormat(String outFileName, ResultSet results) {
        this.writeCsv(outFileName, results);
    }

}

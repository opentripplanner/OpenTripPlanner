package org.opentripplanner.analyst.batch;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

import org.opentripplanner.analyst.core.Sample;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class BasicPopulation implements Population {
	
    private static final long serialVersionUID = 20120201L;
    
    private static final Logger LOG = LoggerFactory.getLogger(BasicPopulation.class);
    
    @Setter public String sourceFilename;
    
    @Setter @Getter public List<Individual> individuals = new ArrayList<Individual>(); 
    
    @Autowired IndividualFactory individualFactory;
    
    public BasicPopulation() {
    }

    public BasicPopulation(Individual... individuals) {
        this.individuals = Arrays.asList(individuals);
    }
    
    public BasicPopulation(Collection<Individual> individuals) {
        this.individuals = new ArrayList<Individual>(individuals);
    }

//    @Override
//    public void setIndividuals(List<Individual> individuals) {
//        this.individuals = individuals;
//    }
    
    @Override
    public void add(Individual individual) {
        this.individuals.add(individual);
    }

    @Override
    public Iterator<Individual> iterator() {
        return this.individuals.iterator();
    }

    @Override
    public void writeCsv(String outFileName, ShortestPathTree spt, Individual origin) {
        LOG.debug("Writing population to CSV: {}", outFileName);
        File outFile = new File(outFileName);
        PrintWriter csvWriter;
        try {
            csvWriter = new PrintWriter(outFile);
            csvWriter.printf("lat,lon,data,traveltime\n");
            for (Individual i : this.individuals) {
                Sample s = i.sample;
                long t = Long.MAX_VALUE;
                if (s != null)
                    t = s.eval(spt);
                csvWriter.printf("%f,%f,%f,%d\n", i.getLat(), i.getLon(), i.input, t);
            }
            csvWriter.close();
        } catch (Exception e) {
            LOG.debug("error writing population to CSV: {}", e);
        }
        LOG.debug("Done writing population to CSV.");
    }

}

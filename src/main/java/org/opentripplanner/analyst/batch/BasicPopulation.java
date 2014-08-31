/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.analyst.batch;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.csvreader.CsvWriter;

public class BasicPopulation implements Population {

    private static final Logger LOG = LoggerFactory.getLogger(BasicPopulation.class);
    
    public String sourceFilename;
    
    public List<Individual> individuals = new ArrayList<Individual>(); 
    
    public List<IndividualFilter> filterChain = null; 

    private boolean[] skip = null;
    
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
        return new PopulationIterator();
    }

    @Override
    public void clearIndividuals(List<Individual> individuals) {
        this.individuals.clear();
    }

    @Override
    public void createIndividuals() {
        // nothing to do in the basic population case
    }

    @Override
    public int size() {
        return this.individuals.size();
    }
        
    protected void writeCsv(String outFileName, ResultSet results) {
        LOG.debug("Writing population to CSV: {}", outFileName);
        try {
            CsvWriter writer = new CsvWriter(outFileName, ',', Charset.forName("UTF8"));
            writer.writeRecord( new String[] {"label", "lat", "lon", "input", "output"} );
            int i = 0;
            int j = 0;
            // using internal list rather than filtered iterator
            for (Individual indiv : this.individuals) {
                if ( ! this.skip[i]) {
                    String[] entries = new String[] { 
                            indiv.label, Double.toString(indiv.lat), Double.toString(indiv.lon), 
                            Double.toString(indiv.input), Double.toString(results.results[j]) 
                    };
                    writer.writeRecord(entries);
                    j++;
                }
                i++;
            }
            writer.close(); // flush writes and close
        } catch (Exception e) {
            LOG.error("Error while writing to CSV file: {}", e.getMessage());
            return;
        }
        LOG.debug("Done writing population to CSV at {}.", outFileName);
    }

    @Override
    public void writeAppropriateFormat(String outFileName, ResultSet results) {
        // as a default, save to CSV. override this method in subclasses when more is known about data structure.
        this.writeCsv(outFileName, results);
    }

    // TODO maybe store skip values in the samples themselves?
    /** 
     * If a filter chain is specified, apply it to the individuals. Must be called after loading 
     * or generating the individuals. Filtering does not actually remove individuals from the 
     * population, it merely tags them as rejected. This is important for structured populations 
     * like rasters, where we may need to write out all individuals including those that were 
     * skipped.
     */
    private void applyFilterChain() {
        this.skip = new boolean[individuals.size()]; // initialized to false
        if (filterChain == null) // no filter chain, do not reject any individuals
            return;
        for (IndividualFilter filter : filterChain) {
            LOG.info("applying filter {}", filter);
            int rejected = 0;
            int i = 0;
            for (Individual individual : this.individuals) {
                boolean skipThis = ! filter.filter(individual);
                if (skipThis)
                    rejected += 1;
                skip[i++] |= skipThis;
            }
            LOG.info("accepted {} rejected {}", skip.length - rejected, rejected);
        }
        int rejected = 0;
        for (boolean s : skip)
            if (s)
                rejected += 1;
        LOG.info("TOTALS: accepted {} rejected {}", skip.length - rejected, rejected);
        
    }

    @Override
    public void setup() {
        // call the subclass-specific file loading method
        this.createIndividuals();
        // call the shared filter chain method
        this.applyFilterChain();
    }

    class PopulationIterator implements Iterator<Individual> {

        int i = 0;
        int n = individuals.size();
        Iterator<Individual> iter = individuals.iterator();
        
        public boolean hasNext() {
            while (i < n && skip[i]) {
                //LOG.debug("in iter, i = {}", i);
                if (! iter.hasNext())
                    return false;
                i += 1;
                iter.next();
            }
            //LOG.debug("done skipping at {}", i);
            return iter.hasNext();
        }
        
        public Individual next() {
            if (this.hasNext()) {
                Individual ret = iter.next();
                i += 1;
                return ret;
            } else {
                throw new NoSuchElementException();
            }
        }

        public void remove() {
            throw new UnsupportedOperationException(); 
        }
        
    }

}


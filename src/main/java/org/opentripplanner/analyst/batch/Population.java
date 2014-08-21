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

import java.util.List;

/**
 * A collection of individual locations that will be used as either the origin set or the 
 * destination set in a many-to-many (batch Analyst) search.
 * 
 * An iterator over a population will skip those individuals that have been marked as rejected by
 * its filter chain, and is not the same as an iterator over a population's list of individuals. 
 * 
 * @author andrewbyrd
 */
public interface Population extends Iterable<Individual> {

    public void addIndividual(Individual individual);

    public void clearIndividuals(List<Individual> individuals);

    /** @return the number of individuals in this population. */
    public int size();

    /**
     * Prepare the population for use. This includes loading or generating the individuals, 
     * filtering them, but not sampling (linking them into the graph) because origin populations
     * do not need to be permanently linked.
     */
    public void setup();

    /**
     * Subclass-specific method to load the individuals from a file or create them based on other 
     * properties. This method should fill in all fields of each individual except the sample, 
     * since sampling will be carried out after the filter chain is applied.
     */
    public void createIndividuals();

    /**
     * Save the output data in this population to a file, using a format that is appropriate for the 
     * specific class of population. For example, a population loaded from an image file or 
     * generated on a regular grid will be saved as a Geotiff raster. A population of points that 
     * are not known to be aligned on a regular grid in some CRS will be saved as a CSV file.
     */
    public void writeAppropriateFormat(String fileName, ResultSet results);

}

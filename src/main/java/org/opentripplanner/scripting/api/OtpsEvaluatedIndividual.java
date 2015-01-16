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

package org.opentripplanner.scripting.api;

/**
 * This class encapsulate both an individual and evaluated values (time...).
 * 
 * Objects are returned by the eval method of the SPT.
 * 
 * @see OtpsSPT.eval()
 * 
 * @author laurent
 */
public class OtpsEvaluatedIndividual {

    private OtpsIndividual individual;

    // TODO Add boardings, walk distance, etc...
    private long time;

    protected OtpsEvaluatedIndividual(OtpsIndividual individual, long time) {
        this.individual = individual;
        this.time = time;
    }

    /**
     * @return The time, in seconds, for this evualuated individual. Return null/None if no time is
     *         available (position not snapped or out of time range).
     */
    public Long getTime() {
        if (time == Long.MAX_VALUE)
            return null;
        return time;
    }

    /**
     * @return The individual evaluated (the same individual from the evuluated population).
     */
    public OtpsIndividual getIndividual() {
        return individual;
    }

    @Override
    public String toString() {
        return individual.toString() + " -> t=" + (getTime() == null ? "null" : (getTime() + "s"));
    }
}

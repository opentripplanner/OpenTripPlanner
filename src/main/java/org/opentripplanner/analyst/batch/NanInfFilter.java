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

public class NanInfFilter implements IndividualFilter {

    private boolean rejectInfinite = true;
    private boolean rejectNan = true;
    private double replaceInfiniteWith = 0;
    private double replaceNanWith = 0;
    
    @Override
    public boolean filter(Individual individual) {
        double input = individual.input;
        if (Double.isInfinite(input)) {
            if (rejectInfinite) {
                return false;
            } else {
                individual.input = replaceInfiniteWith;
            }
        } else if (Double.isNaN(input)) {
            if (rejectNan) {
                return false;
            } else {
                individual.input = replaceNanWith;
            }
        }
        return true;
    }

}

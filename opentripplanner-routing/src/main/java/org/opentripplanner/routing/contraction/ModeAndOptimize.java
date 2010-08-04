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

package org.opentripplanner.routing.contraction;

import java.io.Serializable;

import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.TraverseMode;

/** 
 * Bean holding a @link{TraverseMode} and @link{OptimizeType}, for quick lookup
 * @author novalis
 *
 */

public class ModeAndOptimize implements Serializable {

    private static final long serialVersionUID = 8921229844989906143L;

    public TraverseMode mode;

    public OptimizeType optimizeFor;

    public ModeAndOptimize() {

    }

    public ModeAndOptimize(TraverseMode mode, OptimizeType optimizeFor) {
        this.mode = mode;
        this.optimizeFor = optimizeFor;
    }

    public TraverseMode getMode() {
        return mode;
    }
    
    public OptimizeType getOptimizeFor() {
        return optimizeFor;
    }
    
    public void setMode(TraverseMode mode) {
        this.mode = mode;
    }

    public void setOptimizeFor(OptimizeType optimizeFor) {
        this.optimizeFor = optimizeFor;
    }
    
    public boolean equals(Object other) {
        if (other instanceof ModeAndOptimize) {
            ModeAndOptimize mo = (ModeAndOptimize) other;
            return mo.mode.equals(mode) && mo.optimizeFor.equals(optimizeFor);
        }
        return false;
    }
    
    public int hashCode() {
        return mode.hashCode() ^ optimizeFor.hashCode();
    }
    
    public String toString () {
        return (mode + " optimize for " + optimizeFor);
    }
}

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

package org.opentripplanner.routing.util;


/**
 * Generates unique identifiers by incrementing an internal counter.
 * 
 * @author avi
 */
public class IncrementingIdGenerator<T> implements UniqueIdGenerator<T> {
    
    private int next;
    
    public IncrementingIdGenerator() {
        this(0);
    }
    
    /**
     * Construct with a starting counter. 
     * 
     * First call to next() will return start.
     * 
     * @param start
     */
    public IncrementingIdGenerator(int start) {
        next = start;
    }
    
    /**
     * Generates the next identifier.
     * 
     * @return 
     */
    public int getId(T elem) {
        return next++;
    }
}

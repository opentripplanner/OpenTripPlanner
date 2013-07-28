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

package org.opentripplanner.routing.algorithm;

/**
 * This exception is thrown when an edge has a negative weight. Dijkstra's
 * algorithm (and A*) don't work on graphs that have negative weights.  This
 * exception almost always indicates a programming error, but could be 
 * caused by bad GTFS data.
 */
public class NegativeWeightException extends RuntimeException {

    private static final long serialVersionUID = -1018391017439852795L;

    public NegativeWeightException(String message) {
        super(message);
    }

}

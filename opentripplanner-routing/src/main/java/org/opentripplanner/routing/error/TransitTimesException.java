/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (props, at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.error;

/**
 * Indicates that a transit mode was specified, but the search date provided 
 * was outside the date range of the transit data feed used to construct the graph.
 * In other words, there is no transit service information available, 
 * and the user needs to be told this.  
 */
public class TransitTimesException extends RuntimeException {

	private static final long serialVersionUID = 1L;

}

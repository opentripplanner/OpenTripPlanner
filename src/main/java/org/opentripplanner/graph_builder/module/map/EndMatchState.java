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

package org.opentripplanner.graph_builder.module.map;

import java.util.List;

/** 
 * The end of a route's geometry, meaning that the search can quit
 * @author novalis
 *
 */
public class EndMatchState extends MatchState {

    public EndMatchState(MatchState parent, double error, double distance) {
        super(parent, null, distance);
        this.currentError = error;
    }

    @Override
    public List<MatchState> getNextStates() {
        return null;
    }

}

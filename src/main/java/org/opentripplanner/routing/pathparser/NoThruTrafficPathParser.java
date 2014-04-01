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

package org.opentripplanner.routing.pathparser;

import static org.opentripplanner.routing.automata.Nonterminal.plus;
import static org.opentripplanner.routing.automata.Nonterminal.seq;
import static org.opentripplanner.routing.automata.Nonterminal.star;

import org.opentripplanner.routing.automata.DFA;
import org.opentripplanner.routing.automata.Nonterminal;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;

/**
 * Reject non-walking trips that use no-thru-traffic streets
 */
public class NoThruTrafficPathParser extends PathParser {

    private static final int TRANSIT = 1;
    private static final int NOTRAFFIC = 2;
    private static final int REGULAR = 3;

    // 3,4,5 come from StreetEdge.java

    private static final DFA DFA;
    static {

        //T*I*N*I*(T+I*N*I*)*
        Nonterminal rule = seq(star(TRANSIT), star(NOTRAFFIC), star(REGULAR), star(NOTRAFFIC), star(plus(TRANSIT), star(NOTRAFFIC), star(REGULAR), star(NOTRAFFIC)));
        DFA = rule.toDFA().minimize();
        System.out.println(DFA.toGraphViz());
        System.out.println(DFA.dumpTable());
    }

    @Override
    protected DFA getDFA() {
        return DFA;
    }

    @Override
    public int terminalFor(State state) {
        if (state.getBackEdge() instanceof PlainStreetEdge) {
            if (((PlainStreetEdge)state.getBackEdge()).isNoThruTraffic()) {
                return NOTRAFFIC;
            } else {
                return REGULAR;
            }
        } else {
            return TRANSIT;
        }
    }

}

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

package org.opentripplanner.routing.automata;

import static org.opentripplanner.routing.automata.Nonterminal.*;
import junit.framework.TestCase;

public class AutomatonTest extends TestCase {

    static final int WALK = 0;
    static final int STATION = 1;
    static final int TRANSIT = 2;
    
    static final int NONTHRU = 0;
    static final int THRU = 1;

    private Nonterminal itinerary;
    
    public void setUp() {
        Nonterminal walkLeg = plus(WALK);
        Nonterminal transitLeg = plus(plus(STATION), plus(TRANSIT), plus(STATION));
        itinerary = seq(walkLeg, star(transitLeg, walkLeg));
    }
    
    public void testAutomata() {
        // NFA nfa = seq(WALK, STATION, choice(TRANSIT, WALK), STATION, WALK).toNFA();
        // NFA nfa = seq(WALK, STATION, star(TRANSIT), STATION, WALK).toNFA();
        // NFA nfa = new NTKleenePlus(new NTTrivial(TRANSIT)).toNFA();
        NFA nfa = itinerary.toNFA();
        System.out.print(nfa.toGraphViz());
        DFA dfa = new DFA(nfa);
        System.out.print(dfa.toGraphViz());
        System.out.print(dfa.dumpTable());
        testParse(dfa);
        nfa = nfa.reverse().reverse().reverse().reverse();
        dfa = new DFA(nfa);
        testParse(dfa);

        dfa = dfa.minimize();
        System.out.print(dfa.toGraphViz());
        System.out.print(dfa.dumpTable());
        testParse(dfa);

        dfa = itinerary.toDFA().minimize();
        System.out.print(dfa.toGraphViz());
        System.out.print(dfa.dumpTable());
        testParse(dfa);

        // dfa = seq(star(NONTHRU), star(THRU)).toDFA();
        // System.out.print(dfa.toGraphViz());
        // System.out.print(dfa.dumpTable());
        // testParse(dfa, NONTHRU, NONTHRU, NONTHRU, WALK, WALK, WALK, WALK);

    }

    //this one tests the choice method
    public void testAutomata2() {
        Nonterminal any = choice(WALK,STATION,TRANSIT);
        NFA nfa = choice(star(WALK), seq(star(any), TRANSIT, star(any))).toNFA();
        System.out.print(nfa.toGraphViz());
        DFA dfa = new DFA(nfa);
        testParse(dfa);
        nfa = nfa.reverse().reverse().reverse().reverse();
        dfa = new DFA(nfa);
        testParse(dfa);

        dfa = dfa.minimize();
        testParse(dfa);

    }


    private static void testParse(DFA dfa) {
        testParse(dfa, true, WALK, WALK, WALK, WALK, WALK, WALK, WALK);
        testParse(dfa, true, WALK, STATION, TRANSIT, STATION, WALK, WALK, WALK);
        testParse(dfa, true, WALK, STATION, TRANSIT, STATION, STATION, TRANSIT, STATION, WALK);
        testParse(dfa, false, WALK, WALK, STATION, STATION, STATION, WALK, WALK);
    }

    private static void testParse(DFA dfa, boolean acceptable, int... symbols) {
        boolean accepted = dfa.parse(symbols);
        if (acceptable)
            assertTrue("DFA should accept this input.", accepted);
        else 
            assertFalse("DFA should reject this input.", accepted);
    }
}

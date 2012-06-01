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

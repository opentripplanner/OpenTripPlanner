package org.opentripplanner.routing.automata;

import static org.opentripplanner.routing.automata.Nonterminal.*;

public class AutomatonTest {

	static final int WALK = 0;
	static final int STATION = 1;
	static final int TRANSIT = 2;

	static final int NONTHRU = 0;
	static final int THRU = 1;
	
	public static void main(String[] args) {
		Nonterminal walkLeg = plus(WALK);
		Nonterminal transitLeg = plus(plus(STATION), plus(TRANSIT), plus(STATION));
		Nonterminal itinerary = seq(walkLeg, star(transitLeg, walkLeg));
//		NFA nfa = seq(WALK, STATION, choice(TRANSIT, WALK), STATION, WALK).toNFA();
//		NFA nfa = seq(WALK, STATION, star(TRANSIT), STATION, WALK).toNFA();
//		NFA nfa = new NTKleenePlus(new NTTrivial(TRANSIT)).toNFA();
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

//		dfa = seq(star(NONTHRU), star(THRU)).toDFA();
//		System.out.print(dfa.toGraphViz());
//		System.out.print(dfa.dumpTable());
//		testParse(dfa, NONTHRU, NONTHRU, NONTHRU, WALK, WALK, WALK, WALK);
		
	}
	
	private static void testParse(DFA dfa) {
		testParse(dfa, WALK, WALK, WALK, WALK, WALK, WALK, WALK);
		testParse(dfa, WALK, STATION, TRANSIT, STATION, WALK, WALK, WALK);
		testParse(dfa, WALK, STATION, TRANSIT, STATION, STATION, TRANSIT, STATION, WALK);
		testParse(dfa, WALK, WALK, STATION, STATION, STATION, WALK, WALK);
	}

	private static void testParse(DFA dfa, int... symbols) {
		boolean accepted = dfa.parse(symbols);
		for (int i : symbols) {
			System.out.print(i + " ");
		}
		System.out.print(": ");
		System.out.println(accepted);
	}
}

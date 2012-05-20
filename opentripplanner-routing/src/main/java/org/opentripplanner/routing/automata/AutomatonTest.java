package org.opentripplanner.routing.automata;

import static org.opentripplanner.routing.automata.Nonterminal.*;

public class AutomatonTest {


	public static void main(String[] args) {
		final int WALK = 1;
		final int STATION = 2;
		final int TRANSIT = 3;
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
		testParse(dfa, WALK, WALK, WALK, WALK, WALK, WALK, WALK);
		testParse(dfa, WALK, STATION, TRANSIT, STATION, WALK, WALK, WALK);
		testParse(dfa, WALK, STATION, TRANSIT, STATION, STATION, TRANSIT, STATION, WALK);
		testParse(dfa, WALK, WALK, STATION, STATION, STATION, WALK, WALK);
		
		final int NONTHRU = 0;
		final int THRU = 1;

		dfa = seq(star(NONTHRU), star(THRU)).toDFA();
		System.out.print(dfa.toGraphViz());
		System.out.print(dfa.dumpTable());
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

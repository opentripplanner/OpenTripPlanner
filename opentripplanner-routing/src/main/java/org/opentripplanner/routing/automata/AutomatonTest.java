package org.opentripplanner.routing.automata;

import static org.opentripplanner.routing.automata.Nonterminal.*;

public class AutomatonTest {

	private static final int WALK = 1;
	private static final int STATION = 2;
	private static final int TRANSIT = 3;

	public static void main(String[] args) {
		Nonterminal walkLeg = plus(WALK);
		Nonterminal transitLeg = star(plus(STATION), plus(TRANSIT), plus(STATION));
		Nonterminal itinerary = seq(walkLeg, star(seq(transitLeg, walkLeg)));
		NFA nfa = itinerary.toNFA();
		System.out.print(nfa.toGraphViz());
		DFA dfa = itinerary.toDFA();
		System.out.print(dfa.toGraphViz());
		System.out.print(dfa.dumpTable());
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

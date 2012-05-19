package org.opentripplanner.routing.automata;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class NFA {

	final Nonterminal nt; 
	final AutomatonState start;
	
	public NFA(Nonterminal nt) {
		this.nt = nt;
		this.start = new AutomatonState();
		AutomatonState acceptState = nt.build(start);
		acceptState.accept = true;
	}

	protected NFA (AutomatonState start, Nonterminal nt) {
		this.start = start;
		this.nt = nt;
	}
	
	public DFA toDFA() {
		return new DFA(this);
	}

	public String toGraphViz() {

		// first, find all states reachable from the start state
		Queue<AutomatonState> q = new LinkedList<AutomatonState>();
		Map<AutomatonState, String> names = new HashMap<AutomatonState, String>();
		q.add(start);
		names.put(start, "START");
		//names.put(end, "END");
		char counter = 'A';
		while ( ! q.isEmpty()) {
			AutomatonState s = q.poll();
			for (Transition e : s) {
				if ( ! names.containsKey(e.target)) {
					names.put(e.target, Character.toString(counter++));
					q.add(e.target);
				}
			}
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("digraph automaton { \n");
		sb.append("  rankdir=LR; \n");
		sb.append("  node [shape = doublecircle]; \n");
		for (AutomatonState as : names.keySet())
			if (as.accept)
				sb.append(String.format("  %s; \n", names.get(as)));
		sb.append("  node [shape = circle]; \n");

		for (AutomatonState as : names.keySet()) {
			for (Transition e : as) {
				sb.append("  ");
				sb.append(names.get(as));
				sb.append("->");
				sb.append(names.get(e.target));
				String label = Integer.toString(e.token);
				if (e.token == Transition.EPSILON)
					label = "e";
				sb.append(String.format(" [label=%s];\n", label));
			}
		}
		sb.append("}\n");
		return sb.toString();
	}
	
}

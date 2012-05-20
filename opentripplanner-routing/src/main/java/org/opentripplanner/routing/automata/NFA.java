package org.opentripplanner.routing.automata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NFA {

    final Nonterminal nt; 
	final AutomatonState start;
	
	public NFA(Nonterminal nt) {
		this.nt = nt;
		this.start = new AutomatonState("START");
		AutomatonState acceptState = nt.build(start);
		//acceptState.label = "END";
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
		/* first, find all states reachable from the start state */
		Set<AutomatonState> states = new HashSet<AutomatonState>();
		{
			Queue<AutomatonState> q = new LinkedList<AutomatonState>();
			q.add(start);
			states.add(start);
			while ( ! q.isEmpty()) {
				AutomatonState s = q.poll();
				//System.out.println(s.toString());
				List<AutomatonState> targets = new ArrayList<AutomatonState>();
				for (Transition transition : s.transitions)
					targets.add(transition.target);
				targets.addAll(s.epsilonTransitions);
				for (AutomatonState target : targets)
					if (states.add(target))
						q.add(target);
			}
		}
		/* build DOT file node styles */
		StringBuilder sb = new StringBuilder();
		sb.append("digraph automaton { \n");
		sb.append("  rankdir=LR; \n");
		sb.append("  node [shape = doublecircle]; \n");
		for (AutomatonState as : states)
			if (as.accept)
				sb.append(String.format("  %s; \n", as.label));
		sb.append("  node [shape = circle]; \n"); 
		/* make edges for terminal and epsilon transitions */
		for (AutomatonState fromState : states) {
			for (Transition transition : fromState.transitions) {
				sb.append("  " + fromState.label);
				sb.append(" -> ");
				sb.append(transition.target.label);
				String label = Integer.toString(transition.terminal);
				sb.append(String.format(" [label=%s];\n", label));
			}
			for (AutomatonState toState : fromState.epsilonTransitions) {
				sb.append("  " + fromState.label);
				sb.append(" -> ");
				sb.append(toState.label);
				sb.append(" [label=ε];\n");
			}
		}
		sb.append("}\n");
		return sb.toString();
	}
	
}

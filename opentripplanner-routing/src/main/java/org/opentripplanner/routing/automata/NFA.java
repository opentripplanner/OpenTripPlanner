package org.opentripplanner.routing.automata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class NFA {

    final Nonterminal nt; 
	final Set<AutomatonState> startStates = new HashSet<AutomatonState>();
	final Set<AutomatonState> acceptStates = new HashSet<AutomatonState>();
	final List<AutomatonState> states = new ArrayList<AutomatonState>(); // fill me in during dfa creation, positions are DFA table indexes

	public NFA(Nonterminal nt) {
		this(nt, true);
	}

	protected NFA(Nonterminal nt, boolean build) {
		this.nt = nt;
		if (build) {
			AutomatonState start = new AutomatonState("START"); 
			AutomatonState accept = nt.build(start);
			this.startStates.add(start);
			this.acceptStates.add(accept);
		}
	}

	public DFA toDFA() {
		return new DFA(this);
	}

	/**
	 * Do a Dijkstra search from the start state to find all reachable states in this NFA.
	 * This is of course not O(1), cache the list if you need to reuse it.
	 */
	private Set<AutomatonState> findStates() {
		Set<AutomatonState> states = new HashSet<AutomatonState>();
		{
			Queue<AutomatonState> q = new LinkedList<AutomatonState>();
			q.addAll(startStates);
			states.addAll(startStates);
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
		return states;
	}

	public NFA reverse() {
		Set<AutomatonState> states = this.findStates();
		Map<AutomatonState, AutomatonState> newStates = new HashMap<AutomatonState, AutomatonState>();
		for (AutomatonState state : states)
			newStates.put(state, new AutomatonState(state.label));
		for (AutomatonState oldFromState : states) {
			AutomatonState newToState = newStates.get(oldFromState);
			for (Transition t : oldFromState.transitions) {
				AutomatonState newFromState = newStates.get(t.target);
				newFromState.transitions.add(new Transition(t.terminal, newToState));
			}
			for (AutomatonState oldToState : oldFromState.epsilonTransitions) {
				AutomatonState newFromState = newStates.get(oldToState);
				newFromState.epsilonTransitions.add(newToState);
			}
		}
		NFA result = new NFA(this.nt, false);
		for (AutomatonState old : this.acceptStates)
			result.startStates.add(newStates.get(old));
		for (AutomatonState old : this.startStates)
			result.acceptStates.add(newStates.get(old));
		return result;
	}
	
	/**
	 * Convert to a minimal DFA using Brzozowski's algorithm: reverse the
	 * transitions and apply the powerset construction, yielding a minimal DFA
	 * for the reverse language, then repeat the procedure to get a minimal DFA
	 * for the original language.
	 */
	public DFA minimize() {
		return this.reverse().toDFA().reverse().toDFA();
	}
	
	public String toGraphViz() {
		/* first, find all states reachable from the start state */
		Set<AutomatonState> states = this.findStates();
		
		/* build DOT file node styles */
		StringBuilder sb = new StringBuilder();
		sb.append("digraph automaton { \n");
		sb.append("  rankdir=LR; \n");
		sb.append("  node [shape = doublecircle]; \n");
		for (AutomatonState as : acceptStates)
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

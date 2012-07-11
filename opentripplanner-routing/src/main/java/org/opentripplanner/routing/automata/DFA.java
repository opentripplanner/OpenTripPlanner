package org.opentripplanner.routing.automata;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

/**
 * A deterministic finite automaton, with a transition table for fast incremental parsing.
 * 
 * @author abyrd
 */
public class DFA extends NFA {

    final int[][] table;

    /** Build a deterministic finite automaton from an existing, potentially nondeterministic one. */
    public DFA(NFA nfa) {
        super(nfa.nt, false);
        this.table = determinize(nfa);
        this.relabelNodes();
    }

    /** Build a deterministic finite automaton that accepts the given nonterminal from a grammar */
    public DFA(Nonterminal nonterminal) {
        this(new NFA(nonterminal));
    }

    /**
     * A glorified set of automaton states, used in determinizing NFAs. Semantic equality inherited
     * from HashSet is needed for determinization.
     */
    private class NFAStateSet extends HashSet<AutomatonState> {
        private static final long serialVersionUID = 1L;

        NFAStateSet(AutomatonState... automatonStates) {
            this.addAll(Arrays.asList(automatonStates));
        }

        NFAStateSet(Collection<AutomatonState> automatonStates) {
            this.addAll(automatonStates);
        }

        public void followEpsilons() {
            Queue<AutomatonState> queue = new LinkedList<AutomatonState>();
            queue.addAll(this);
            while (!queue.isEmpty()) {
                AutomatonState as = queue.poll();
                for (AutomatonState target : as.epsilonTransitions)
                    if (this.add(target))
                        queue.add(target);
            }
        }
    }

    /**
     * convert a (potentially) nondeterministic automaton into a new deterministic automaton with no
     * epsilon edges
     */
    private int[][] determinize(NFA nfa) {

        int maxTerminal = 0; // track max token value so we know how big to make the transition table
        Map<NFAStateSet, AutomatonState> dfaStates = new HashMap<NFAStateSet, AutomatonState>();
        Queue<NFAStateSet> queue = new LinkedList<NFAStateSet>();

        /* initialize the work queue with the set of all states reachable from the NFA start state */
        {
            AutomatonState dfaStart = new AutomatonState("START");
            this.startStates.add(dfaStart);
            this.states.add(dfaStart);
            NFAStateSet startSet = new NFAStateSet(nfa.startStates);
            startSet.followEpsilons(); // be sure to follow episilons *before* using hashcode
            dfaStates.put(startSet, dfaStart);
            queue.add(startSet);
        }

        /* find all transitions between epsilon-connected subsets of the NFA states */
        while (!queue.isEmpty()) {
            NFAStateSet nfaFromStates = queue.poll();
            AutomatonState dfaFromState = dfaStates.get(nfaFromStates);
            Map<Integer, NFAStateSet> dfaTransitions = new HashMap<Integer, NFAStateSet>();
            for (AutomatonState nfaFromState : nfaFromStates) {
                if (nfa.acceptStates.contains(nfaFromState))
                    this.acceptStates.add(dfaFromState); // multiple adds OK, it's a set
                for (Transition t : nfaFromState.transitions) {
                    if (t.terminal > maxTerminal)
                        maxTerminal = t.terminal;
                    NFAStateSet nfaTargetStates = dfaTransitions.get(t.terminal);
                    if (nfaTargetStates == null) {
                        nfaTargetStates = new NFAStateSet();
                        dfaTransitions.put(t.terminal, nfaTargetStates);
                    }
                    nfaTargetStates.add(t.target);
                }
            }
            for (Entry<Integer, NFAStateSet> t : dfaTransitions.entrySet()) {
                int terminal = t.getKey();
                NFAStateSet nfaToStates = t.getValue();
                nfaToStates.followEpsilons();
                AutomatonState dfaToState = dfaStates.get(nfaToStates);
                if (dfaToState == null) {
                    dfaToState = new AutomatonState(); // nfaToStates.deriveLabel());
                    dfaStates.put(nfaToStates, dfaToState);
                    this.states.add(dfaToState);
                    queue.add(nfaToStates);
                }
                dfaFromState.transitions.add(new Transition(terminal, dfaToState));
            }
        }

        /* create a transition table, filled with the reserved value for the reject state */
        int[][] table = new int[this.states.size()][maxTerminal + 1];
        for (int[] row : table)
            Arrays.fill(row, AutomatonState.REJECT);

        /* copy all edges from the new DFA states into the table */
        for (int row = 0; row < this.states.size(); row++)
            for (Transition t : this.states.get(row).transitions)
                table[row][t.terminal] = this.states.indexOf(t.target);

        /* return the transition table to the DFA constructor */
        return table;
    }

    /** Dump the transition table to a string. Rows are states, columns are terminal symbols. */
    public String dumpTable() {
        StringBuilder sb = new StringBuilder();
        int r = 0;
        sb.append("      ");
        for (int i = 0; i < table[0].length; i++)
            sb.append(String.format("%2d ", i));
        sb.append(" \n");
        for (int[] row : table) {
            sb.append(String.format("%5s ", states.get(r).label));
            for (int i : row) {
                if (i == AutomatonState.REJECT)
                    sb.append(" - ");
                else
                    sb.append(String.format("%2s ", states.get(i).label));
            }
            sb.append("\n");
            r += 1;
        }
        return sb.toString();
    }

    /**
     * Return true if this DFA (and the nonterminal it is built from) match the given sequence of
     * terminal symbols.
     */
    public boolean parse(int... terminals) {
        int state = AutomatonState.START;
        for (int terminal : terminals) {
            state = transition(state, terminal);
            if (state == AutomatonState.REJECT)
                return false;
        }
        return this.accepts(state);
    }

    /** this method will not catch reject states; the caller must do so. */
    public int transition(int initState, int terminal) {
        return table[initState][terminal];
    }

    public boolean accepts(int state) {
        if (state == AutomatonState.REJECT)
            return false;
        return acceptStates.contains(states.get(state));
    }

}

package org.opentripplanner.routing.automata;

public class NTTrivial extends Nonterminal {

	int terminal;
	
	public NTTrivial(int terminal) {
		this.terminal = terminal;
	}

	@Override
	public AutomatonState build(AutomatonState in) {
		AutomatonState out = new AutomatonState();
		in.transitions.add(new Transition(terminal, out));
		return out;
	}

}

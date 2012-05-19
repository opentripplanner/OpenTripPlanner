package org.opentripplanner.routing.automata;

public class NTTrivial extends Nonterminal {

	int terminal;
	
	public NTTrivial(int terminal) {
		this.terminal = terminal;
	}

	@Override
	public AutomatonState build(AutomatonState start) {
		AutomatonState exit = new AutomatonState();
		start.add(new SymbolTransition(terminal, exit));
		return exit;
	}

}

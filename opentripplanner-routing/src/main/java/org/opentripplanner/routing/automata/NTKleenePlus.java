package org.opentripplanner.routing.automata;

public class NTKleenePlus extends Nonterminal {

	private Nonterminal nt; 
	
	public NTKleenePlus(Nonterminal nt) {
		this.nt = nt;
	}
	
	@Override
	public AutomatonState build(AutomatonState start) {
		AutomatonState exit = nt.build(start);
		exit.add(new EpsilonTransition(start));
		return exit;
	}

}

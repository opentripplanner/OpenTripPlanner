package org.opentripplanner.routing.automata;

public class NTKleeneStar extends Nonterminal {

	private Nonterminal nt; 
	
	public NTKleeneStar(Nonterminal nt) {
		this.nt = nt;
	}
	
	@Override
	public AutomatonState build(AutomatonState start) {
		Nonterminal plus = new NTKleenePlus(nt);
		AutomatonState exit = plus.build(start);
		start.add(new EpsilonTransition(exit));
		return exit;
	}

}

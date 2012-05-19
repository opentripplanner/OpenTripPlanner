package org.opentripplanner.routing.automata;

public class NTSequence extends Nonterminal {

	private Nonterminal[] nts; 
	
	public NTSequence(Nonterminal... nts) {
		this.nts = nts.clone(); // in case caller modifies the array later
	}
	
	@Override
	public AutomatonState build(AutomatonState start) {
		AutomatonState exit = start;
		for (Nonterminal nt : nts) {
			exit = nt.build(exit);
		}
		return exit;
	}

}

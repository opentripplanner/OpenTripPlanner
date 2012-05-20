package org.opentripplanner.routing.automata;

public class NTSequence extends Nonterminal {

	private Nonterminal[] nts; 
	
	public NTSequence(Nonterminal... nts) {
		this.nts = nts.clone(); // in case caller modifies the array later
	}
	
	@Override
	public AutomatonState build(AutomatonState in) {
		AutomatonState out = in;
		for (Nonterminal nt : nts) {
			out = nt.build(out);
		}
		return out;
	}

}

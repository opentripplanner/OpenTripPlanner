package org.opentripplanner.routing.automata;

public class NTKleenePlus extends Nonterminal {

	private Nonterminal nt; 
	
	public NTKleenePlus(Nonterminal nt) {
		this.nt = nt;
	}
	
	@Override
	public AutomatonState build(AutomatonState in) {
		// isolate epsilon loop from chained NFAs
		AutomatonState start = new AutomatonState();
		in.epsilonTransitions.add(start); 
		AutomatonState end = nt.build(start);
		end.epsilonTransitions.add(start);
		return end;
	}

}

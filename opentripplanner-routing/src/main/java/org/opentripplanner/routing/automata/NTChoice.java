package org.opentripplanner.routing.automata;

import java.util.Collection;
import java.util.LinkedList;

public class NTChoice extends Nonterminal {

	private Nonterminal[] nts; 
	
	public NTChoice(Nonterminal... nts) {
		this.nts = nts.clone(); // in case caller modifies the array later
	}

	@Override
	public AutomatonState build(AutomatonState start) {
		Collection<AutomatonState> exits = new LinkedList<AutomatonState>();
		for (Nonterminal nt : nts) {
			exits.add(nt.build(start));
		}
		AutomatonState exit = new AutomatonState();
		for (AutomatonState subExit : exits) {
			subExit.add(new EpsilonTransition(exit));
		}
		return exit;
	}

}

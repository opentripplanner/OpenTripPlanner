package org.opentripplanner.routing.automata;

public class NTKleeneStar extends Nonterminal {

	private Nonterminal nt; 
	
	public NTKleeneStar(Nonterminal nt) {
		this.nt = nt;
	}
	
	@Override
	public AutomatonState build(AutomatonState in) {
		Nonterminal plus = new NTKleenePlus(nt);
		AutomatonState out = plus.build(in);
		// bypass Kleene plus to match zero occurrences 
		in.epsilonTransitions.add(out);
		return out;
	}

}

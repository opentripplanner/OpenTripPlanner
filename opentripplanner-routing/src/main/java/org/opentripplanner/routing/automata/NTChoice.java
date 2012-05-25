package org.opentripplanner.routing.automata;

import java.util.Collection;
import java.util.LinkedList;

public class NTChoice extends Nonterminal {

    private Nonterminal[] nts;

    public NTChoice(Nonterminal... nts) {
        this.nts = nts.clone(); // in case caller modifies the array later
    }

    @Override
    public AutomatonState build(AutomatonState in) {
        Collection<AutomatonState> outs = new LinkedList<AutomatonState>();
        for (Nonterminal nt : nts) {
            outs.add(nt.build(in));
        }
        AutomatonState out = new AutomatonState();
        for (AutomatonState subExit : outs) {
            subExit.epsilonTransitions.add(out);
        }
        return out;
    }

}

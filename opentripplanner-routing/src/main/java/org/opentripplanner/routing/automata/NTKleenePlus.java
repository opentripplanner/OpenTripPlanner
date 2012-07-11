package org.opentripplanner.routing.automata;

public class NTKleenePlus extends Nonterminal {

    private Nonterminal nt;

    public NTKleenePlus(Nonterminal nt) {
        this.nt = nt;
    }

    @Override
    public AutomatonState build(AutomatonState in) {
        // isolate epsilon loop from chained NFAs
        AutomatonState in2 = new AutomatonState();
        in.epsilonTransitions.add(in2);
        AutomatonState out = nt.build(in2);
        out.epsilonTransitions.add(in2);
        return out;
    }

    // general rule for nonterminals: never add an epsilon edge to a state you did not create

}

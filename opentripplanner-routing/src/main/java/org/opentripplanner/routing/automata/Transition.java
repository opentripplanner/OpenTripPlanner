package org.opentripplanner.routing.automata;

public class Transition {

    public final int terminal;

    public final AutomatonState target;

    Transition(int terminal, AutomatonState target) {
        this.terminal = terminal;
        this.target = target;
        if (terminal < 0)
            throw new RuntimeException("negative terminal symbols are reserved");
    }

}

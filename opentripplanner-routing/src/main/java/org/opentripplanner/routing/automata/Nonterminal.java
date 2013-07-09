/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.automata;

import java.util.Arrays;
import java.util.List;

/**
 * An abstract superclass for all nonterminals, which are models for instantiated NFAs. Also
 * provides expression building methods which can be used via a static import.
 * 
 * Thanks to Matt Might, whose self-described "toy Java library" inspired the use of Java syntax to
 * build up automata:
 * http://matt.might.net/articles/implementation-of-nfas-and-regular-expressions-in-java/
 * 
 * His library also contained a comment suggesting the more functional style provided here by the
 * build() method.
 * 
 * @author abyrd
 */
public abstract class Nonterminal {

    /**
     * Recursively builds a new graph of automaton states (an NFA) based on the model provided by
     * this Nonterminal. This allows nonterminals to be reused in multiple expressions, or multiple
     * times within a single expression.
     * 
     * Because we allow epsilon moves in NFAs, every nonterminal can have a single entry and exit
     * point (i.e. start and accept state).
     * 
     * @param entry - the start state for the automaton to build
     * @return the accept state of the newly constructed automaton
     */
    public abstract AutomatonState build(AutomatonState in);

    /* postfix (instance) expression builder methods */

    public Nonterminal star() {
        return new NTKleeneStar(this);
    }

    public Nonterminal plus() {
        return new NTKleenePlus(this);
    }

    public Nonterminal or(Object that) {
        return new NTChoice(this, wrap0(that));
    }

    public Nonterminal chain(Object... objects) {
        List<Object> newObjects = Arrays.asList((Object) this);
        newObjects.addAll(Arrays.asList(objects));
        return new NTSequence(wrap(newObjects.toArray()));
    }

    /* prefix (factory) expression builder methods (use via static import) */

    public static Nonterminal seq(Object... objs) {
        return new NTSequence(wrap(objs));
    }

    public static Nonterminal star(Object... objs) {
        return new NTKleeneStar(seq(objs));
    }

    public static Nonterminal plus(Object... objs) {
        return new NTKleenePlus(seq(objs));
    }

    public static Nonterminal choice(Object... objs) {
        return new NTChoice(wrap(objs));
    }

    /* wrap terminals in trivial nonterminals to provide them with/to expression builder methods */

    private static Nonterminal wrap0(Object o) {
        if (o instanceof Integer)
            return new NTTrivial((Integer) o);
        else if (o instanceof Nonterminal)
            return (Nonterminal) o;
        else
            throw new RuntimeException(
                    "attempted to build an NFA out of something that was not a terminal or a nonterminal");
    }

    private static Nonterminal[] wrap(Object... objects) {
        Nonterminal[] nonterminals = new Nonterminal[objects.length];
        int i = 0;
        for (Object object : objects) {
            nonterminals[i++] = wrap0(object);
        }
        return nonterminals;
    }

    /* build automata from expressions */

    public NFA toNFA() {
        return new NFA(this);
    }

    public DFA toDFA() {
        return new DFA(this);
    }

}

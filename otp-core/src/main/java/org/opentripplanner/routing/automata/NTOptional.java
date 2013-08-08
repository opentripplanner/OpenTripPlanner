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

/** 
 * Consume zero or one copies of the specified nonterminal. This is equivalent to the question
 * mark quantifier in regular expressions.
 */
public class NTOptional extends Nonterminal {

    private Nonterminal nt;

    public NTOptional(Nonterminal nt) {
        this.nt = nt;
    }

    @Override
    public AutomatonState build(AutomatonState in) {
        AutomatonState out = nt.build(in);
        AutomatonState out2 = new AutomatonState();
        // general rule for nonterminals: 
        // never add an epsilon edge leading to (?) a state you did not create.
        out.epsilonTransitions.add(out2);
        in.epsilonTransitions.add(out2);
        return out2;
    }


}

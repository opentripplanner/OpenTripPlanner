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

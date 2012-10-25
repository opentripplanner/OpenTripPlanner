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

package org.opentripplanner.routing.pathparser;

import org.opentripplanner.routing.automata.DFA;
import org.opentripplanner.routing.core.State;

public abstract class PathParser {

	public int transition(int initState, int terminal) {
		return this.getDFA().transition(initState, terminal);
	}
	
	public boolean accepts(int parseState) {
		return this.getDFA().accepts(parseState);
	}

	/** 
	 * Concrete PathParsers implement this method to convert OTP States 
	 * (and their backEdges) into terminals in the language they define.
	 */
	public abstract int terminalFor(State state);
	
	/** 
	 * Concrete PathParsers implement this method to provide a DFA that
	 * will accept certain paths and not others. 
	 */
	protected abstract DFA getDFA();

}
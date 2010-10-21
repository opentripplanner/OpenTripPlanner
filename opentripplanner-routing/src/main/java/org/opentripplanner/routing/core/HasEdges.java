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

package org.opentripplanner.routing.core;

import java.util.Collection;

/**
 * Designates a source of incoming and outgoing {@link Edge} edges when performing graph traversal.
 * 
 * @author bdferris
 * @see GraphVertex
 */
public interface HasEdges {

    public int getDegreeIn();

    public int getDegreeOut();

    public Collection<Edge> getIncoming();

    public Collection<Edge> getOutgoing();
}

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

import org.opentripplanner.routing.spt.GraphPath;

/**
 * Interface to implement for {@link EdgeNarrative} implementations that wish to indicate that their
 * from and to vertices are mutable. This is useful for {@link EdgeNarrative} objects produced by
 * {@link Edge} implementations where there are multiple potential traversal results and target
 * vertices possible for a particular traversal. When doing reverse optimization in
 * {@link GraphPath}, the {@link Edge} may not actually know the resulting target {@link Vertex}
 * from the initial edge traversal, since it's only stored in the original {@link EdgeNarrative}. By
 * indicating that the reverse-optimized {@link EdgeNarrative}, is mutable, we can reset the
 * {@link EdgeNarrative#getFromVertex()} and {@link EdgeNarrative#getToVertex()} vertices from the
 * original edge narrative as appropriate.
 * 
 * @author bdferris
 * 
 */
public interface MutableEdgeNarrative {

    public void setFromVertex(Vertex fromVertex);

    public void setToVertex(Vertex toVertex);
}

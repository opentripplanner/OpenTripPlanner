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

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

/** Represents a vertex's graph information -- its incoming and outgoing edges
 * @author novalis
 *
 */
public class GraphVertex implements Serializable, HasEdges {
    private static final long serialVersionUID = 5209227015108443460L;
    
    public Vertex vertex;
    ArrayList<Edge> incoming;
    ArrayList<Edge> outgoing;
    
    public GraphVertex(Vertex v) {
        vertex = v;
        incoming = new ArrayList<Edge>();
        outgoing = new ArrayList<Edge>();
    }

    @SuppressWarnings("unchecked")
    public GraphVertex(GraphVertex gv) {
        vertex = gv.vertex;
        incoming = (ArrayList<Edge>) gv.incoming.clone();
        outgoing = (ArrayList<Edge>) gv.outgoing.clone();
    }

    public void addOutgoing(Edge ee) {
        outgoing.add(ee);
    }
    
    public void addIncoming(Edge ee) {
        incoming.add(ee);
    }
    
    public void removeOutgoing(Edge ee) {
        outgoing.remove(ee);
    }
    
    public void removeIncoming(Edge ee) {
        incoming.remove(ee);
    }
    
    /****
     * {@link HasEdges} Interface
     ****/
    
    public int getDegreeIn() {
        return incoming.size();
    }
 
    public int getDegreeOut() {
        return outgoing.size();
    }

    public Collection<Edge> getOutgoing() {
        return outgoing;
    }

    public Collection<Edge> getIncoming() {
        return incoming;
    }
    
    /****
     * Private Methods
     *****/
    
    private void writeObject(ObjectOutputStream out) throws IOException {
        incoming.trimToSize();
        outgoing.trimToSize();
        out.defaultWriteObject();
    }
    
    public boolean equals(Object other) {
        if (other instanceof GraphVertex) {
            GraphVertex gvother = (GraphVertex) other;
            return gvother.incoming.equals(incoming) && gvother.outgoing.equals(outgoing) && gvother.vertex.equals(vertex);
        }
        return false;
    }
}

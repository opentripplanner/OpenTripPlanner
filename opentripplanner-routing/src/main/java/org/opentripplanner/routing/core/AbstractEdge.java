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

import org.opentripplanner.routing.impl.DummyReferenceVertex;

public abstract class AbstractEdge implements Edge, Serializable {

    private static final long serialVersionUID = 1L;

    private Vertex fromv, tov;
    
    public void replaceDummyVertices(Graph graph) {
        if( fromv instanceof DummyReferenceVertex)
            fromv = graph.getVertex(((DummyReferenceVertex) fromv).getLabel());
        if( tov instanceof DummyReferenceVertex)
            tov = graph.getVertex(((DummyReferenceVertex) tov).getLabel());
    }

    public String toString() {
        return fromv.getLabel() + "-> " + tov.getLabel();
    }

    public AbstractEdge(Vertex fromv, Vertex tov) {
        this.fromv = fromv;
        this.tov = tov;
    }

    @Override
    public Vertex getFromVertex() {
        return fromv;
    }

    @Override
    public Vertex getToVertex() {
        return tov;
    }

    public void setFromVertex(Vertex fromv) {
        this.fromv = fromv;
    }

    public void setToVertex(Vertex tov) {
        this.tov = tov;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        if( fromv != null)
            fromv = new DummyReferenceVertex(fromv.getLabel());
        if( tov != null)
            tov = new DummyReferenceVertex(tov.getLabel());
        out.defaultWriteObject();
    }

}

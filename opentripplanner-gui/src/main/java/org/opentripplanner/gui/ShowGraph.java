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

package org.opentripplanner.gui;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.GenericVertex;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.GraphVertex;
import org.opentripplanner.routing.core.TransitStop;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.edgetype.StreetTransitLink;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.strtree.STRtree;

import processing.core.PApplet;
import processing.core.PFont;

/**
 * Show a map of the graph, intersections and TransitStops only. The user can drag to zoom. The user
 * can also click, and a list of nearby vertices will be sent to the associated
 * VertexSelectionListener
 */
public class ShowGraph extends PApplet {

    private static final long serialVersionUID = -8336165356756970127L;

    Graph graph;

    STRtree vertexIndex;

    STRtree edgeIndex;

    /*
     * static public void main(String args[]) { PApplet.main(new String[] {"ShowGraph"}); }
     */

    Envelope modelOuterBounds;

    Envelope modelBounds = new Envelope();

    VertexSelectionListener selector;

    private ArrayList<VertexSelectionListener> selectors;

    int startDragX, startDragY;

    private Edge highlightedEdge;

    private Vertex highlightedVertex;

    private Set<Vertex> highlightedVertices = new HashSet<Vertex>();

    protected double mouseModelX;

    protected double mouseModelY;

    public ShowGraph(VertexSelectionListener selector, Graph graph) {
        super();
        this.graph = graph;
        this.selector = selector;
        this.selectors = new ArrayList<VertexSelectionListener>();
    }


    public void setup() {
        background(0);
        size(getSize().width, getSize().height, P2D);
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                super.mouseMoved(e);
                Point p = e.getPoint();
                mouseModelX = toModelX(p.x);
                mouseModelY = toModelY(p.y);
            }
        });

        indexVertices();
        
        modelBounds.expandBy(0.02);
        /* fix aspect ratio */
        double yCenter = (modelBounds.getMaxY() + modelBounds.getMinY()) / 2;
        float xScale = cos((float) (yCenter * Math.PI / 180));
        double xSize = modelBounds.getMaxX() - modelBounds.getMinX();
        double ySize = modelBounds.getMaxY() - modelBounds.getMinY();
        double actualXSize = xSize * xScale;
        System.out.println("xs, ys, axs: " + xSize + ", " + ySize + "," + actualXSize);
        if (ySize > actualXSize) {
            // too tall, stretch horizontally
            System.out.println("stretching x by " + (ySize / xScale - actualXSize));
            modelBounds.expandBy((ySize / xScale - actualXSize) / 2, 0);
        } else {
            // too wide, stretch vertically
            System.out.println("stretching y by " + (actualXSize - ySize));
            modelBounds.expandBy(0, (actualXSize - ySize) / 2);
        }

        modelOuterBounds = new Envelope(modelBounds);

        /* find and set up the appropriate font */
        String[] fonts = PFont.list();
        String[] preferredFonts = { "Courier", "Mono" };
        PFont font = null;
        for (String preferredFontName : preferredFonts) {
            for (String fontName : fonts) {
                if (fontName.contains(preferredFontName)) {
                    font = createFont(fontName, 16);
                    break;
                }
            }
            if (font != null) {
                break;
            }
        }
        textFont(font);
    }

    public synchronized void indexVertices() {
        vertexIndex = new STRtree();
        edgeIndex = new STRtree();
        for (GraphVertex gv : graph.getVertices()) {
            Vertex v = gv.vertex;
            Envelope env = new Envelope(v.getCoordinate());
            modelBounds.expandToInclude(env);
            if (v instanceof TransitStop) {
                vertexIndex.insert(env, v);
            } else if (v instanceof StreetLocation) {
                vertexIndex.insert(env, v);
            } else if (v instanceof GenericVertex) {
                vertexIndex.insert(env, v);
            } else {
                // a street-transit link, or a journey vertex. no need for them in the ui.
            }

            for (Edge e : gv.getOutgoing()) {
                if (e.getGeometry() == null) {
                    continue;
                }
                env = e.getGeometry().getEnvelopeInternal();
                edgeIndex.insert(env, e);
            }
        }
        vertexIndex.build();
        edgeIndex.build();
    }

    public void zoomToDefault() {
        modelBounds = new Envelope(modelOuterBounds);
    }

    public void zoomToLocation(Coordinate c) {
        Envelope e = new Envelope();
        e.expandToInclude(c);
        e.expandBy(0.002);
        modelBounds = e;
    }

    public void zoomToVertex(Vertex v) {
        Envelope e = new Envelope();
        e.expandToInclude(v.getCoordinate());
        e.expandBy(0.002);
        modelBounds = e;
    }

    public void zoomOut() {
        modelBounds.expandBy(modelBounds.getWidth(), modelBounds.getHeight());
    }

    @SuppressWarnings("unchecked")
    public synchronized void draw() {
        fill(0);
        rect(0, 0, getSize().width - 1, getSize().height - 1);
        List<Vertex> vertices = (List<Vertex>) vertexIndex.query(modelBounds);

        List<Edge> edges = (List<Edge>) edgeIndex.query(modelBounds);
        for (Edge e : edges) {
            if (e == highlightedEdge)
                continue;

            if (e instanceof StreetTransitLink) {
                stroke(75, 150, 255);
            } else {
                stroke(30, 255, 255);
            }

            Coordinate[] coords = e.getGeometry().getCoordinates();
            for (int i = 1; i < coords.length; i++) {
                line((float) toScreenX(coords[i - 1].x), (float) toScreenY(coords[i - 1].y),
                        (float) toScreenX(coords[i].x), (float) toScreenY(coords[i].y));
            }
        }
        if (highlightedEdge != null && highlightedEdge.getGeometry() != null) {
            stroke(255, 70, 70);

            Coordinate[] coords = highlightedEdge.getGeometry().getCoordinates();
            for (int i = 1; i < coords.length; i++) {
                line((float) toScreenX(coords[i - 1].x), (float) toScreenY(coords[i - 1].y),
                        (float) toScreenX(coords[i].x), (float) toScreenY(coords[i].y));
            }
            stroke(0,255,0);
            ellipse(toScreenX(coords[0].x), toScreenY(coords[0].y), 5, 5);
            stroke(255,0,0);
            ellipse(toScreenX(coords[coords.length-1].x), toScreenY(coords[coords.length-1].y), 5, 5);
        }

        for (Vertex v : vertices) {
            if (v == highlightedVertex)
                continue;

            double x = v.getX();
            double y = v.getY();
            x = toScreenX(x);
            y = toScreenY(y);
            if (v instanceof TransitStop) {
                fill(0);
                stroke(255, 30, 255);
                ellipse(x, y, 5.0, 5.0);
            } else if (highlightedVertices.contains(v)) {
                stroke(0, 255, 0);
                fill(0, 255, 0);
                ellipse(x, y, 3.0, 3.0);
            } else {
                stroke(255);
                fill(255, 0, 0);
                ellipse(x, y, 3.0, 3.0);
            }
        }
        if (highlightedVertex != null) {
            stroke(255, 255, 30);
            fill(255, 255, 30);
            ellipse(toScreenX(highlightedVertex.getX()), toScreenY(highlightedVertex.getY()), 7.0,
                    7.0);
            noFill();
        }
        fill(255, 0, 0);
        text(mouseModelX + ", " + mouseModelY, 0, 10);
    }

    private double toScreenY(double y) {
        return map(y, modelBounds.getMinY(), modelBounds.getMaxY(), getSize().height, 0);
    }

    private double toScreenX(double x) {
        return map(x, modelBounds.getMinX(), modelBounds.getMaxX(), 0, getSize().width);
    }

    @SuppressWarnings("unchecked")
    public void mouseClicked() {
        Envelope screenEnv = new Envelope(new Coordinate(mouseX, mouseY));
        screenEnv.expandBy(3, 3);
        Envelope env = new Envelope(toModelX(screenEnv.getMinX()), toModelX(screenEnv.getMaxX()),
                toModelY(screenEnv.getMinY()), toModelY(screenEnv.getMaxY()));

        List<Vertex> nearby = (List<Vertex>) vertexIndex.query(env);
        selector.verticesSelected(nearby);
    }

    public void mousePressed() {
        startDragX = mouseX;
        startDragY = mouseY;
    }

    public void mouseReleased() {
        if (startDragX == -1 || startDragY == -1 || Math.abs(mouseX - startDragX) < 5
                || Math.abs(mouseY - startDragY) < 5) {
            startDragX = startDragY = -1;
            return;
        }
        // rescale
        double x1 = toModelX(startDragX);
        double x2 = toModelX(mouseX);
        if (x1 > x2) {
            double tmp = x1;
            x1 = x2;
            x2 = tmp;
        }
        double y1 = toModelY(startDragY);
        double y2 = toModelY(mouseY);
        if (y1 > y2) {
            double tmp = y1;
            y1 = y2;
            y2 = tmp;
        }

        double yDist = y2 - y1;
        double xDist = x2 - x1;

        if (yDist < 0.00001 || xDist < 0.00001) {
            startDragX = startDragY = -1;
            return;
        }

        // fix ratio
        double originalAspectRatio = modelOuterBounds.getWidth() / modelOuterBounds.getHeight();
        double zoomBoxAspectRatio = xDist / yDist;
        if (zoomBoxAspectRatio > originalAspectRatio) {
            double desiredYDist = yDist * zoomBoxAspectRatio / originalAspectRatio;
            y1 -= (desiredYDist - yDist) / 2;
            y2 += (desiredYDist - yDist) / 2;
        } else {
            double desiredXDist = xDist * originalAspectRatio / zoomBoxAspectRatio;
            x1 -= (desiredXDist - xDist) / 2;
            x2 += (desiredXDist - xDist) / 2;
        }

        modelBounds = new Envelope(x1, x2, y1, y2);
    }

    private double toModelY(double y) {
        return map(y, 0, getSize().height, modelBounds.getMaxY(), modelBounds.getMinY());
    }

    private double toModelX(double x) {
        return map(x, 0, getSize().width, modelBounds.getMinX(), modelBounds.getMaxX());
    }

    /**
     * A version of ellipse that takes double args, because apparently Java is too stupid to
     * downgrade automatically.
     * 
     * @param d
     * @param e
     * @param f
     * @param g
     */
    private void ellipse(double d, double e, double f, double g) {
        ellipse((float) d, (float) e, (float) f, (float) g);
    }

    /**
     * Set the Vertex selector to newSelector, and store the old selector on the stack of selectors
     * 
     * @param newSelector
     */
    public void pushSelector(VertexSelectionListener newSelector) {
        selectors.add(selector);
        selector = newSelector;
    }

    /**
     * Restore the previous vertexSelector
     */
    public void popSelector() {
        selector = selectors.get(selectors.size() - 1);
        selectors.remove(selectors.size() - 1);
    }

    public void highlightVertex(Vertex v) {
        Coordinate c = v.getCoordinate();
        double xd = 0, yd = 0;
        while (!modelBounds.contains(c)) {
            xd = modelBounds.getWidth() / 100;
            yd = modelBounds.getHeight() / 100;
            modelBounds.expandBy(xd, yd);
        }
        modelBounds.expandBy(xd, yd);
        highlightedVertex = v;
    }

    public void setHighlighed(Set<Vertex> vertices) {
        highlightedVertices = vertices;
    }

    public void highlightEdge(Edge selected) {
        highlightedEdge = selected;
    }
}

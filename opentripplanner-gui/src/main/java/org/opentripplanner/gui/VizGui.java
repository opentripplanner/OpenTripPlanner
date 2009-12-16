package org.opentripplanner.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import javax.swing.AbstractListModel;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.PatternAlight;
import org.opentripplanner.routing.edgetype.PatternBoard;
import org.opentripplanner.routing.edgetype.TripPattern;

/** 
 * Exit on window close.
 *
 */
class ExitListener extends WindowAdapter {
    public void windowClosing(WindowEvent event) {
        System.exit(0);
    }
}
/**
 *  DisplayVertex holds a vertex, but has a toString value that's a little more useful.  
 */ 
class DisplayVertex {
    public Vertex vertex;

    public DisplayVertex(Vertex v) {
        vertex = v;
    }

    public String toString() {
        return vertex.getLabel() + " - " + vertex.getName();
    }
}

/** 
 * This is a ListModel that holds Edges.  It gets its edges from a PatternBoard/PatternAlight,
 * hence the iterable.
 */
class EdgeListModel extends AbstractListModel {

    private static final long serialVersionUID = 1L;

    private ArrayList<Edge> edges;

    EdgeListModel(Iterable<Edge> edges) {
        this.edges = new ArrayList<Edge>();
        for (Edge e : edges) {
            this.edges.add(e);
        }
    }

    public int getSize() {
        return edges.size();
    }

    public Object getElementAt(int index) {
        return edges.get(index);
    }
}

/**
 * This is a ListModel that shows a TripPattern's departure times from a particular stop 
 */
class TripPatternListModel extends AbstractListModel {

    private static final long serialVersionUID = 1L;

    ArrayList<String> departureTimes = new ArrayList<String>();

    public TripPatternListModel(TripPattern pattern, int stopIndex) {
        for (int dt : pattern.getDepartureTimes(stopIndex)) {
            Calendar c = new GregorianCalendar();
            c.setTimeInMillis(dt * 1000);
            departureTimes.add(String.format("%02d:%02d:%02d", c.get(Calendar.HOUR), c
                    .get(Calendar.MINUTE), c.get(Calendar.SECOND)));
        }
    }

    @Override
    public Object getElementAt(int index) {
        return departureTimes.get(index);
    }

    @Override
    public int getSize() {
        return departureTimes.size();
    }

}

/** 
 * A simple visualizer for graphs.  It shows (using ShowGraph) a map of the graph, intersections and
 * TransitStops only, and allows a user to select stops, examine incoming and outgoing edges, and
 * examine trip patterns.  It's meant mainly for debugging, so it's totally OK if it develops (say)
 * a bunch of weird buttons designed to debug specific cases.
 *
 */
public class VizGui extends JFrame implements VertexSelectionListener {

    private static final long serialVersionUID = 1L;

    private JPanel rightPanel;

    private JPanel leftPanel;

    private ShowGraph showGraph;

    public JList nearbyVertices;

    private JList outgoingEdges;

    private JList incomingEdges;

    private JList departurePattern;

    private JLabel serviceIdLabel;

    public VizGui(String graphObj) {
        super();

        BorderLayout layout = new BorderLayout();
        setLayout(layout);
        Container pane = getContentPane();

        showGraph = new ShowGraph(this, graphObj);
        pane.add(showGraph, BorderLayout.CENTER);

        /*
         * left panel, top-to-bottom: list of nearby vertices, list of edges for selected vertex,
         * buttons
         */
        leftPanel = new JPanel();
        leftPanel.setLayout(new BorderLayout());

        pane.add(leftPanel, BorderLayout.LINE_START);

        JPanel vertexDataPanel = new JPanel();
        vertexDataPanel.setLayout(new BoxLayout(vertexDataPanel, BoxLayout.PAGE_AXIS));
        leftPanel.add(vertexDataPanel, BorderLayout.CENTER);

        JLabel nvLabel = new JLabel("Vertices");
        vertexDataPanel.add(nvLabel);
        nearbyVertices = new JList();
        nearbyVertices.setPrototypeCellValue("Bite the wax tadpole right on the nose");
        nearbyVertices.setVisibleRowCount(4);
        JScrollPane nvScrollPane = new JScrollPane(nearbyVertices);
        vertexDataPanel.add(nvScrollPane);

        JLabel ogeLabel = new JLabel("Outgoing edges");
        vertexDataPanel.add(ogeLabel);
        outgoingEdges = new JList();
        outgoingEdges.setVisibleRowCount(4);
        JScrollPane ogeScrollPane = new JScrollPane(outgoingEdges);
        vertexDataPanel.add(ogeScrollPane);

        JLabel iceLabel = new JLabel("Incoming edges");
        vertexDataPanel.add(iceLabel);
        incomingEdges = new JList();
        JScrollPane iceScrollPane = new JScrollPane(incomingEdges);
        vertexDataPanel.add(iceScrollPane);

        /* when a different edge is selected, change up the pattern pane */
        ListSelectionListener edgeChanged = new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                JList edgeList = (JList) e.getSource();
                Edge selected = (Edge) edgeList.getSelectedValue();
                if (selected == null) {
                    departurePattern.removeAll();
                    return;
                }
                // figure out the pattern, if any
                TripPattern pattern = null;
                int stopIndex = 0;
                if (selected instanceof PatternBoard) {
                    PatternBoard boardEdge = (PatternBoard) selected;
                    pattern = boardEdge.getPattern();
                    stopIndex = boardEdge.getStopIndex();
                } else if (selected instanceof PatternAlight) {
                    PatternAlight alightEdge = (PatternAlight) selected;
                    pattern = alightEdge.getPattern();
                    stopIndex = alightEdge.getStopIndex();
                } else {
                    departurePattern.removeAll();
                    return;
                }
                Trip trip = pattern.exemplar;
                serviceIdLabel.setText(trip.getServiceId().toString());
                ListModel model = new TripPatternListModel(pattern, stopIndex);

                departurePattern.setModel(model);

            }
        };

        outgoingEdges.addListSelectionListener(edgeChanged);
        incomingEdges.addListSelectionListener(edgeChanged);

        nearbyVertices.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                outgoingEdges.removeAll();
                incomingEdges.removeAll();
                DisplayVertex selected = (DisplayVertex) nearbyVertices.getSelectedValue();
                if (selected != null) {
                    Vertex nowSelected = selected.vertex;
                    outgoingEdges.setModel(new EdgeListModel(nowSelected.getOutgoing()));
                    incomingEdges.setModel(new EdgeListModel(nowSelected.getIncoming()));
                }
            }
        });

        /* buttons at bottom */
        JPanel buttonPanel = new JPanel();
        leftPanel.add(buttonPanel, BorderLayout.PAGE_END);

        JButton zoomDefaultButton = new JButton("Zoom to default");
        zoomDefaultButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showGraph.zoomToDefault();
            }
        });
        buttonPanel.add(zoomDefaultButton);

        /* right panel holds trip pattern info */
        rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.PAGE_AXIS));
        serviceIdLabel = new JLabel("trip pattern");
        rightPanel.add(serviceIdLabel);

        pane.add(rightPanel, BorderLayout.LINE_END);
        departurePattern = new JList();
        departurePattern.setPrototypeCellValue("Bite the wax tadpole right on the nose");
        JScrollPane dpScrollPane = new JScrollPane(departurePattern);
        rightPanel.add(dpScrollPane);

        showGraph.init();
        addWindowListener(new ExitListener());
        pack();
    }

    public static void main(String args[]) {
        VizGui gui = new VizGui(args[0]);
        gui.setVisible(true);

    }

    @Override
    public void verticesSelected(final List<Vertex> selected) {
        ListModel data = new AbstractListModel() {

            private static final long serialVersionUID = 1L;

            public int getSize() {
                return selected.size();
            }

            public Object getElementAt(int index) {
                return new DisplayVertex(selected.get(index));
            }
        };

        nearbyVertices.setModel(data);
    }
}

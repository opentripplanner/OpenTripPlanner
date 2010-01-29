package org.opentripplanner.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Vector;

import javassist.Modifier;

import javax.swing.AbstractListModel;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ListModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.PatternAlight;
import org.opentripplanner.routing.edgetype.PatternBoard;
import org.opentripplanner.routing.edgetype.Street;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.edgetype.Turn;
import org.opentripplanner.routing.impl.GraphSerializationLibrary;
import org.opentripplanner.routing.impl.PathServiceImpl;
import org.opentripplanner.routing.impl.RoutingServiceImpl;
import org.opentripplanner.routing.impl.StreetVertexIndexServiceImpl;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.SPTVertex;

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
 * DisplayVertex holds a vertex, but has a toString value that's a little more useful.
 */
class DisplayVertex {
    public Vertex vertex;

    public DisplayVertex(Vertex v) {
        vertex = v;
    }

    public String toString() {
        String label = vertex.getLabel();
        if (label.contains("osm node")) {
            label = vertex.getName();
        }
        return label;
    }
}

/**
 * This is a ListModel that holds Edges. It gets its edges from a PatternBoard/PatternAlight, hence
 * the iterable.
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
    	for (Integer dt : pattern.getDepartureTimes(stopIndex)) {
            Calendar c = new GregorianCalendar();
            c.setTimeInMillis((dt + 5 * 3600) * 1000);
            departureTimes.add(DateFormat.getTimeInstance().format(c.getTime()));
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
 * A list of vertices where the internal container is exposed.
 */
class VertexList extends AbstractListModel {

    private static final long serialVersionUID = 1L;

    public List<Vertex> selected;

    VertexList(List<Vertex> selected) {
        this.selected = selected;
    }

    public int getSize() {
        return selected.size();
    }

    public Object getElementAt(int index) {
        return new DisplayVertex(selected.get(index));
    }
};

/**
 * A simple visualizer for graphs. It shows (using ShowGraph) a map of the graph, intersections and
 * TransitStops only, and allows a user to select stops, examine incoming and outgoing edges, and
 * examine trip patterns. It's meant mainly for debugging, so it's totally OK if it develops (say) a
 * bunch of weird buttons designed to debug specific cases.
 * 
 */
public class VizGui extends JFrame implements VertexSelectionListener {

    private static final long serialVersionUID = 1L;

    private JPanel leftPanel;

    private ShowGraph showGraph;

    public JList nearbyVertices;

    private JList outgoingEdges;

    private JList incomingEdges;

    private JList departurePattern;

    private JLabel serviceIdLabel;

    private PathServiceImpl pathservice;

    private Graph graph;

    private StreetVertexIndexServiceImpl indexService;

    private RoutingServiceImpl routingService;

    private DefaultListModel metadataModel;

    public VizGui(String graphName) {
        super();

        BorderLayout layout = new BorderLayout();
        setLayout(layout);
        Container pane = getContentPane();

        try {
            graph = GraphSerializationLibrary.readGraph(new File(graphName));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        showGraph = new ShowGraph(this, graph);
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
        /*
         * when a different edge is selected, change up the pattern pane and list of nearby nodes
         */
        ListSelectionListener edgeChanged = new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {

                JList edgeList = (JList) e.getSource();
                Edge selected = (Edge) edgeList.getSelectedValue();
                if (selected == null) {
                    departurePattern.removeAll();
                    return;
                }
                showGraph.highlightEdge(selected);

                /* for turns, highlight the outgoing street's ends */
                if (selected instanceof Turn) {
                    HashSet<Vertex> vertices = new HashSet<Vertex>();
                    Vertex tov = selected.getToVertex();
                    for (Edge og : tov.getOutgoing()) {
                        if (og instanceof Street) {
                            vertices.add (og.getToVertex());
                            break;
                        }
                    }
                    Vertex fromv = selected.getFromVertex();
                    for (Edge ic : fromv.getIncoming()) {
                        if (ic instanceof Street) {
                            vertices.add (ic.getFromVertex());
                            break;
                        }
                    }
                    showGraph.setHighlighed(vertices);
                }

                /* add the connected vertices to the list of vertices */
                VertexList nearbyModel = (VertexList) nearbyVertices.getModel();
                List<Vertex> vertices = nearbyModel.selected;

                Vertex v;
                if (edgeList == outgoingEdges) {
                    v = selected.getToVertex();
                } else {
                    v = selected.getFromVertex();
                }
                if (!vertices.contains(v)) {
                    vertices.add(v);
                    nearbyModel = new VertexList(vertices);
                    nearbyVertices.setModel(nearbyModel); // this should just be an event, but for
                                                          // some reason, JList doesn't implement
                                                          // the right event.
                }

                /* set up metadata tab */
                metadataModel.clear();
                Class<?> c = selected.getClass();
                metadataModel.addElement("Class:" + c);
                Field[] fields = c.getDeclaredFields();
                for (int i = 0; i < fields.length; i++) {
                    Field field = fields[i];
                    int modifiers = field.getModifiers();
                    if ((modifiers & Modifier.STATIC) != 0) {
                        continue;
                    }
                    field.setAccessible(true);
                    String name = field.getName();
                    String value = "(unknown -- see console for stack trace)";
                    try {
                        value = "" + field.get(selected);
                    } catch (IllegalArgumentException e1) {
                        e1.printStackTrace();
                    } catch (IllegalAccessException e1) {
                        e1.printStackTrace();
                    }
                    metadataModel.addElement(name + ": " + value);
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
                ListModel model = new TripPatternListModel(pattern, stopIndex);
                departurePattern.setModel(model);

                Trip trip = pattern.exemplar;
                serviceIdLabel.setText(trip.getServiceId().toString());
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
                    showGraph.highlightVertex(nowSelected);
                    outgoingEdges.setModel(new EdgeListModel(nowSelected.getOutgoing()));
                    incomingEdges.setModel(new EdgeListModel(nowSelected.getIncoming()));
                }
            }
        });

        /* buttons at bottom */
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(0, 3));
        leftPanel.add(buttonPanel, BorderLayout.PAGE_END);

        JButton zoomDefaultButton = new JButton("Zoom to default");
        zoomDefaultButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showGraph.zoomToDefault();
            }
        });
        buttonPanel.add(zoomDefaultButton);

        final JFrame frame = this;

        JButton zoomToNodeButton = new JButton("Zoom to node");
        zoomToNodeButton.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		String nodeName = (String) JOptionPane.showInputDialog(frame, "Node id", JOptionPane.PLAIN_MESSAGE);
        		Vertex v = graph.getVertex(nodeName);
        		if (v == null) {
        			System.out.println("no such node " + nodeName);
        		} else {
        			showGraph.zoomToVertex(v);
        		}
        	}
        });
        buttonPanel.add(zoomToNodeButton);
        
        JButton routeButton = new JButton("Route");
        routeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String initialFrom = "";
                Object selected = nearbyVertices.getSelectedValue();
                if (selected != null) {
                    initialFrom = selected.toString();
                }
                RouteDialog dlg = new RouteDialog(frame, initialFrom); // modal
                String from = dlg.from;
                String to = dlg.to;
                route(from, to);
            }
        });
        buttonPanel.add(routeButton);

        JButton findButton = new JButton("Find node");
        findButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String nodeName = (String) JOptionPane.showInputDialog(frame, "Node id",
                        JOptionPane.PLAIN_MESSAGE);
                Vertex v = graph.getVertex(nodeName);
                if (v == null) {
                    System.out.println("no such node " + nodeName);
                } else {
                    showGraph.highlightVertex(v);
                    ArrayList<Vertex> l = new ArrayList<Vertex>();
                    l.add(v);
                    verticesSelected(l);
                }
            }
        });
        buttonPanel.add(findButton);

        JButton findEdgeButton = new JButton("Find edge");
        findEdgeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String edgeName = (String) JOptionPane.showInputDialog(frame, "Edge name like",
                        JOptionPane.PLAIN_MESSAGE);
                for (Vertex v : graph.getVertices()) {
                    for (Edge edge: v.getOutgoing()) {
                        if (edge.getName() != null && edge.getName().contains(edgeName)) {
                            showGraph.highlightVertex(v);
                            ArrayList<Vertex> l = new ArrayList<Vertex>();
                            l.add(v);
                            verticesSelected(l);
                        }
                    }
                }
            }
        });
        buttonPanel.add(findEdgeButton);

        JButton checkButton = new JButton("Check graph");
        checkButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                checkGraph();
            }
        });
        buttonPanel.add(checkButton);

        JButton traceButton = new JButton("Trace");
        traceButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                trace();
            }
        });
        buttonPanel.add(traceButton);
        
        
        /* right panel holds trip pattern and stop metadata */
        JTabbedPane rightPanel = new JTabbedPane();

        pane.add(rightPanel, BorderLayout.LINE_END);
        departurePattern = new JList();
        departurePattern.setPrototypeCellValue("Bite the wax tadpole right on the nose");
        JScrollPane dpScrollPane = new JScrollPane(departurePattern);
        rightPanel.addTab("trip pattern", dpScrollPane);

        JList metadataList = new JList();
        metadataModel = new DefaultListModel();
        metadataList.setModel(metadataModel);
        metadataList.setPrototypeCellValue("bicycleSafetyEffectiveLength : 10.42468803");
        JScrollPane mdScrollPane = new JScrollPane(metadataList);
        rightPanel.addTab("metadata", mdScrollPane);

        pathservice = new PathServiceImpl();
        pathservice.setGraph(graph);
        indexService = new StreetVertexIndexServiceImpl(graph);
        indexService.setup();
        pathservice.setIndexService(indexService);
        routingService = new RoutingServiceImpl();
        routingService.setGraph(graph);
        pathservice.setRoutingService(routingService);

        showGraph.init();
        addWindowListener(new ExitListener());
        pack();
    }

    protected void trace() {
        HashSet<Vertex> seenVertices = new HashSet<Vertex>();
        DisplayVertex selected = (DisplayVertex) nearbyVertices.getSelectedValue();
        if (selected == null) {
            System.out.println ("no vertex selected");
            return;
        }
        Vertex v = selected.vertex;
        System.out.println ("initial vertex: " + v);
        Queue<Vertex> toExplore = new LinkedList<Vertex>();
        toExplore.add(v);
        seenVertices.add(v);
        while (!toExplore.isEmpty()) {
            Vertex src = toExplore.poll();
            for (Edge e : src.getOutgoing()) {
                Vertex tov = e.getToVertex();
                if (!seenVertices.contains(tov)) {
                    seenVertices.add(tov);
                    toExplore.add(tov);
                }
            }
        }
        showGraph.setHighlighed(seenVertices);
        
    }

    protected void checkGraph() {
        
        HashSet<Vertex> seenVertices = new HashSet<Vertex>();
        Collection<Vertex> allVertices = graph.getVertices();
        Vertex v = allVertices.iterator().next();
        System.out.println ("initial vertex: " + v);
        Queue<Vertex> toExplore = new LinkedList<Vertex>();
        toExplore.add(v);
        seenVertices.add(v);
        while (!toExplore.isEmpty()) {
            Vertex src = toExplore.poll();
            for (Edge e : src.getOutgoing()) {
                Vertex tov = e.getToVertex();
                if (!seenVertices.contains(tov)) {
                    seenVertices.add(tov);
                    toExplore.add(tov);
                }
            }
        }

        System.out.println("After investigation, visited " + seenVertices.size() + " of " + allVertices.size());
        
        /*now, let's find an unvisited vertex */
        for (Vertex u : allVertices) {
            if (!seenVertices.contains(u)) {
                System.out.println ("unknown vertex" + u);
                break;
            }
        }
    }

    protected void route(String from, String to) {
        TraverseOptions options = new TraverseOptions();
        Date now = new Date(1260994200000L);
        System.out.println("Path from " + from + " to " + to + " at " + now);
        List<GraphPath> paths = pathservice.plan(from, to, now, options);
        if (paths.get(0) == null) {
            System.out.println("no path");
            return;
        }
        Vector<SPTVertex> vertices = paths.get(0).vertices;

        for (Vertex v : vertices) {
            System.out.println(v);
        }
    }

    public static void main(String args[]) {
    	if (args.length == 0) {
    		System.out.println("Usage: VizGui /path/to/Graph.obj");
    		System.exit(1);
    	}
        VizGui gui = new VizGui(args[0]);
        gui.setVisible(true);

    }

    @Override
    public void verticesSelected(final List<Vertex> selected) {
        ListModel data = new VertexList(selected);
        nearbyVertices.setModel(data);
    }
}

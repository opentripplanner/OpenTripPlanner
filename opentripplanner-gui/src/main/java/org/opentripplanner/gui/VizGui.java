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

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.TimeZone;

import javassist.Modifier;

import javax.swing.AbstractListModel;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.routing.algorithm.GenericAStar;
import org.opentripplanner.routing.core.GraphBuilderAnnotation;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.EdgeWithElevation;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.TableTripPattern;
import org.opentripplanner.routing.edgetype.TransitBoardAlight;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.GraphServiceImpl;
import org.opentripplanner.routing.impl.RetryingPathServiceImpl;
import org.opentripplanner.routing.services.StreetVertexIndexService;
import org.opentripplanner.routing.spt.GraphPath;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

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

    public Edge getElementAt(int index) {
        return edges.get(index);
    }
}

/**
 * This is a ListModel that shows a TripPattern's departure times from a particular stop
 */
class TripPatternListModel extends AbstractListModel {

    private static final long serialVersionUID = 1L;

    ArrayList<String> departureTimes = new ArrayList<String>();

    public TripPatternListModel(TableTripPattern pattern, int stopIndex) {
    	Iterator<Integer> departureTimeIterator = pattern.getScheduledDepartureTimes(stopIndex);
    	while (departureTimeIterator.hasNext()) {
    	    int dt = departureTimeIterator.next();

            Calendar c = new GregorianCalendar();
            c.setTimeInMillis(dt * 1000);
            Date date = c.getTime();
            //adjust the time for the system's timezone.  This is kind of a hack.
            int tzAdjust = TimeZone.getDefault().getOffset(date.getTime());
            c.setTimeInMillis(dt * 1000 - tzAdjust);
            date = c.getTime();
            departureTimes.add(DateFormat.getTimeInstance().format(date));
        }
    }

    public String getElementAt(int index) {
        return departureTimes.get(index);
    }

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

    public DisplayVertex getElementAt(int index) {
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

    private JTextField sourceVertex;
    
    private JTextField sinkVertex;
    
    private JCheckBox walkCheckBox;

    private JCheckBox bikeCheckBox;

    private JCheckBox trainCheckBox;

    private JCheckBox busCheckBox;

    private JCheckBox ferryCheckBox;

    private JCheckBox transitCheckBox;

    private JTextField searchDate;
    
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
       
    private JTextField boardingPenaltyField; 

    private JList departurePattern;

    private DefaultListModel annotationMatchesModel;

    private JList annotationMatches;

    private JLabel serviceIdLabel;

    private RetryingPathServiceImpl pathservice = new RetryingPathServiceImpl();

    private Graph graph;

    private GraphServiceImpl graphservice = new GraphServiceImpl() {
        public Graph getGraph(String routerId) { return graph; }
    };

    private StreetVertexIndexService indexService;

    private GenericAStar sptService = new GenericAStar();

    private DefaultListModel metadataModel;

    private HashSet<Vertex> closed;

    private Vertex tracingVertex;

    private HashSet<Vertex> open;

    private HashSet<Vertex> seen;

    private JList metadataList;

    public VizGui(String graphName) {
        super();
        File path = new File(graphName);
        if (path.getName().equals("Graph.obj")) {
            path = path.getParentFile();
        }
        try {
            graph = Graph.load(new File(graphName), Graph.LoadLevel.FULL);
        } catch (Exception e) {
            e.printStackTrace();
        }
        indexService = graph.streetIndex;
        pathservice.graphService = graphservice;
        pathservice.sptService   = sptService;
        setTitle("VizGui: " + graphName);
        init();
    }
    
    public void init() {
        BorderLayout layout = new BorderLayout();
        setLayout(layout);
        Container pane = getContentPane();

        showGraph = new ShowGraph(this, getGraph());
        pane.add(showGraph, BorderLayout.CENTER);
        sptService.setTraverseVisitor(new VisualTraverseVisitor(showGraph));

        /*
         * left panel, top-to-bottom: list of nearby vertices, list of edges for selected vertex,
         * buttons
         */
        leftPanel = new JPanel();
        leftPanel.setLayout(new BorderLayout());

        pane.add(leftPanel, BorderLayout.LINE_START);

        /* ROUTING SUBPANEL */
        JPanel routingPanel = new JPanel();
        routingPanel.setLayout(new GridLayout(0, 2));
        leftPanel.add(routingPanel, BorderLayout.NORTH);

        // row: source vertex
        JButton setSourceVertexButton = new JButton("set source");
        setSourceVertexButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	Object selected = nearbyVertices.getSelectedValue();
                if (selected != null) {
                	sourceVertex.setText(selected.toString());
                }
            }
        });
        routingPanel.add(setSourceVertexButton);
        sourceVertex = new JTextField();
        routingPanel.add(sourceVertex);      
        
        // row: sink vertex
        JButton setSinkVertexButton = new JButton("set sink");
        setSinkVertexButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	Object selected = nearbyVertices.getSelectedValue();
                if (selected != null) {
                	sinkVertex.setText(selected.toString());
                }
            }
        });
        routingPanel.add(setSinkVertexButton);
        sinkVertex = new JTextField();
        routingPanel.add(sinkVertex);        

        // row: set date
        JButton resetSearchDateButton = new JButton("now ->");
        resetSearchDateButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                searchDate.setText(dateFormat.format(new Date()));
            }
        });
        routingPanel.add(resetSearchDateButton);
        searchDate = new JTextField();
        searchDate.setText(dateFormat.format(new Date()));
        routingPanel.add(searchDate);        

        // 2 rows: transport mode options
        walkCheckBox = new JCheckBox("walk");
        routingPanel.add(walkCheckBox);
        bikeCheckBox = new JCheckBox("bike");
        routingPanel.add(bikeCheckBox);
        trainCheckBox = new JCheckBox("trainish");
        routingPanel.add(trainCheckBox);
        busCheckBox  = new JCheckBox("busish");
        routingPanel.add(busCheckBox);
        ferryCheckBox = new JCheckBox("ferry");
        routingPanel.add(ferryCheckBox);
        transitCheckBox = new JCheckBox("transit");
        routingPanel.add(transitCheckBox);
        
        // row: boarding penalty
        JLabel boardPenaltyLabel = new JLabel("Boarding penalty (min):");
        routingPanel.add(boardPenaltyLabel);
        boardingPenaltyField = new JTextField("5");
        routingPanel.add(boardingPenaltyField);      
        
        // row: launch and clear path search
        JButton routeButton = new JButton("path search");
        routeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String from = sourceVertex.getText();
                String to = sinkVertex.getText();
                route(from, to);
            }
        });
        routingPanel.add(routeButton);
        JButton clearRouteButton = new JButton("clear path");
        clearRouteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showGraph.highlightGraphPath(null);
                showGraph.clearHighlights();
            }
        });
        routingPanel.add(clearRouteButton);
                
        /* VERTEX INFO SUBPANEL */
        JPanel vertexDataPanel = new JPanel();
        vertexDataPanel.setLayout(new BoxLayout(vertexDataPanel, BoxLayout.PAGE_AXIS));
        vertexDataPanel.setPreferredSize(new Dimension(300, 600));
        leftPanel.add(vertexDataPanel, BorderLayout.CENTER);

        JLabel nvLabel = new JLabel("Vertices");
        vertexDataPanel.add(nvLabel);
        nearbyVertices = new JList();
        //nearbyVertices.setPrototypeCellValue("Bite the wax tadpole right on the nose");
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
                if (selected instanceof StreetEdge) {
                    List<Vertex> vertices = new ArrayList<Vertex>();
                    List<Edge> edges = new ArrayList<Edge>();
                    Vertex tov = selected.getToVertex();
                    for (Edge og : tov.getOutgoing()) {
                    	if (og instanceof StreetEdge) {
                    	    edges.add(og);
                    	    vertices.add (og.getToVertex());
                    	    break;
                        }
                    }
                    Vertex fromv = selected.getFromVertex();
                    for (Edge ic : fromv.getIncoming()) {
                        if (ic instanceof StreetEdge) {
                            edges.add(ic);
                            vertices.add (ic.getFromVertex());
                            break;
                        }
                    }
                    //showGraph.setHighlightedVertices(vertices);
                    showGraph.setHighlightedEdges(edges);
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
                Class<?> c;
                Field[] fields;
                getMetadata(selected);
                //fromv
                Vertex fromv = selected.getFromVertex();
                getMetadata(fromv);
                if (selected instanceof EdgeWithElevation) {
                    getMetadata(((EdgeWithElevation) selected).getElevationProfileSegment());
                }
                metadataList.revalidate();
                
                // figure out the pattern, if any
                TableTripPattern pattern = null;
                int stopIndex = 0;
                if (selected instanceof TransitBoardAlight && ((TransitBoardAlight) selected).isBoarding()) {
                    TransitBoardAlight boardEdge = (TransitBoardAlight) selected;
                    pattern = boardEdge.getPattern();
                    stopIndex = boardEdge.getStopIndex();
                } else if (selected instanceof TransitBoardAlight && !((TransitBoardAlight) selected).isBoarding()) {
                    TransitBoardAlight alightEdge = (TransitBoardAlight) selected;
                    pattern = alightEdge.getPattern();
                    stopIndex = alightEdge.getStopIndex();
                } else {
                    departurePattern.removeAll();
                    return;
                }
                ListModel model = new TripPatternListModel(pattern, stopIndex);
                departurePattern.setModel(model);

                Trip trip = pattern.getExemplar();
                serviceIdLabel.setText(trip.getServiceId().toString());
            }

            private void getMetadata(Object selected) {
                Class<?> c = selected.getClass();
                Field[] fields;
                while (c != null && c != Object.class) {
                    metadataModel.addElement("Class:" + c);
                    fields = c.getDeclaredFields();
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
                    c = c.getSuperclass();
                }
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
        		Vertex v = getGraph().getVertex(nodeName);
        		if (v == null) {
        			System.out.println("no such node " + nodeName);
        		} else {
        			showGraph.zoomToVertex(v);
        		}
        	}
        });
        buttonPanel.add(zoomToNodeButton);
        
        JButton zoomToLocationButton = new JButton("Zoom to location");
        zoomToLocationButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
              String result = JOptionPane.showInputDialog("Enter the location (lat lon)");
              if( result == null || result.length() == 0)
                return;
              String[] tokens = result.split("[\\s,]+");
              double lat = Double.parseDouble(tokens[0]);
              double lon = Double.parseDouble(tokens[1]);
              Coordinate c = new Coordinate(lon,lat);
              showGraph.zoomToLocation(c);
            }
        });
        buttonPanel.add(zoomToLocationButton);
        
        JButton zoomOutButton = new JButton("Zoom out");
        zoomOutButton.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            showGraph.zoomOut();
          }
        });
        buttonPanel.add(zoomOutButton);
        
        JButton routeButton2 = new JButton("Route");
        routeButton2.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
//                String initialFrom = "";
//                Object selected = nearbyVertices.getSelectedValue();
//                if (selected != null) {
//                    initialFrom = selected.toString();
//                }
//                RouteDialog dlg = new RouteDialog(frame, initialFrom); // modal
                String from = sourceVertex.getText();
                String to = sinkVertex.getText();
                route(from, to);
            }
        });
        buttonPanel.add(routeButton2);

        JButton findButton = new JButton("Find node");
        findButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String nodeName = (String) JOptionPane.showInputDialog(frame, "Node id",
                        JOptionPane.PLAIN_MESSAGE);
                Vertex v = getGraph().getVertex(nodeName);
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
                for (Vertex gv : getGraph().getVertices()) {
                	for (Edge edge : gv.getOutgoing()) {
                        if (edge.getName() != null && edge.getName().contains(edgeName)) {
                            showGraph.highlightVertex(gv);
                            ArrayList<Vertex> l = new ArrayList<Vertex>();
                            l.add(gv);
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

        // annotation search button
        JButton annotationButton = new JButton("Find annotations");
        annotationButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                findAnnotation();
            }
        });
        buttonPanel.add(annotationButton);
                    
        /* right panel holds trip pattern and stop metadata */
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BorderLayout());
        pane.add(rightPanel, BorderLayout.LINE_END);

        JTabbedPane rightPanelTabs = new JTabbedPane();

        rightPanel.add(rightPanelTabs, BorderLayout.LINE_END);
        serviceIdLabel = new JLabel("[service id]");
        rightPanel.add(serviceIdLabel, BorderLayout.PAGE_END);
        
        departurePattern = new JList();
        JScrollPane dpScrollPane = new JScrollPane(departurePattern);
        rightPanelTabs.addTab("trip pattern", dpScrollPane);

        metadataList = new JList();
        metadataModel = new DefaultListModel();
        metadataList.setModel(metadataModel);
        JScrollPane mdScrollPane = new JScrollPane(metadataList);
        mdScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        rightPanelTabs.addTab("metadata", mdScrollPane);

        // This is where matched annotations from a search go
        annotationMatches = new JList();
        annotationMatches.addListSelectionListener(new ListSelectionListener () {
            public void valueChanged(ListSelectionEvent e) {
                JList theList = (JList) e.getSource();
                SelectableGraphBuilderAnnotation anno = 
                    (SelectableGraphBuilderAnnotation) theList.getSelectedValue();

                if (anno == null) return;

                anno.select(showGraph);
            }
        });    

        annotationMatchesModel = new DefaultListModel();
        annotationMatches.setModel(annotationMatchesModel);
        JScrollPane amScrollPane = new JScrollPane(annotationMatches);
        amScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        rightPanelTabs.addTab("annotations", amScrollPane);

        Dimension size = new Dimension(200, 1600);

        amScrollPane.setMaximumSize(size);
        amScrollPane.setPreferredSize(size);
        mdScrollPane.setMaximumSize(size);
        mdScrollPane.setPreferredSize(size);
        rightPanelTabs.setMaximumSize(size);
        rightPanel.setMaximumSize(size);

        showGraph.init();
        addWindowListener(new ExitListener());
        pack();
    }

    protected void trace() {
        DisplayVertex selected = (DisplayVertex) nearbyVertices.getSelectedValue();
        if (selected == null) {
            return;
        }
        Vertex v = selected.vertex;

        if (tracingVertex != v) {
            tracingVertex = v;
            closed = new HashSet<Vertex>();
            open = new HashSet<Vertex>();
            open.add(v);
            seen = new HashSet<Vertex>();
        }
        HashSet<Vertex> newOpen = new HashSet<Vertex>();
        for (Vertex v2 : open) {
            closed.add(v2);
            for (Edge e: v2.getOutgoing()) {
                Vertex target = e.getToVertex();
                if (closed.contains(target)) {
                    continue;
                }
                newOpen.add(target);
            }      
        }
        seen.addAll(newOpen);
        open = newOpen;
        showGraph.setHighlightedVertices(seen);
    }
    
    protected void traceOld() {
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
        showGraph.setHighlightedVertices(seenVertices);   
    }

    protected void checkGraph() {
        
        HashSet<Vertex> seenVertices = new HashSet<Vertex>();
        Collection<Vertex> allVertices = getGraph().getVertices();
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
                System.out.println ("unvisited vertex" + u);
                break;
            }
        }
    }

    protected void route(String from, String to) {
    	// TraverseOptions is initialized to reasonable values in constructor?
    	Date when;
    	// Year + 1900
        try {
        	 when = dateFormat.parse(searchDate.getText());
        } catch (ParseException e) {
        	searchDate.setText("Format: " + dateFormat.toPattern());
        	return;
        }
        TraverseModeSet modeSet = new TraverseModeSet();
        modeSet.setWalk(walkCheckBox.isSelected());
        modeSet.setBicycle(bikeCheckBox.isSelected());
        modeSet.setFerry(ferryCheckBox.isSelected());
        modeSet.setTrainish(trainCheckBox.isSelected());
        modeSet.setBusish(busCheckBox.isSelected());
        // must set generic transit mode last, and only when it is checked
        // otherwise 'false' will clear trainish and busish 
        if (transitCheckBox.isSelected())
            modeSet.setTransit(true);
    	RoutingRequest options = new RoutingRequest(modeSet);
    	VisualTraverseVisitor visitor = new VisualTraverseVisitor(showGraph);
    	options.setWalkBoardCost(Integer.parseInt(boardingPenaltyField.getText()) * 60); // override low 2-4 minute values
    	// TODO LG Add ui element for bike board cost (for now bike = 2 * walk)
    	options.setBikeBoardCost(Integer.parseInt(boardingPenaltyField.getText()) * 60 * 2);
    	// there should be a ui element for walk distance and optimize type
    	options.setOptimize(OptimizeType.QUICK);
        options.setMaxWalkDistance(Double.MAX_VALUE);
        options.from = from;
        options.to   = to;
        options.numItineraries = 1;
        System.out.println("--------");
        System.out.println("Path from " + from + " to " + to + " at " + when);
        System.out.println("\tModes: " + modeSet);
        System.out.println("\tOptions: " + options);
        // TODO: check options properly intialized (AMB)
        List<GraphPath> paths = pathservice.getPaths(options);
        if (paths == null) {
            System.out.println("no path");
            showGraph.highlightGraphPath(null);
            return;
        }
        GraphPath gp = paths.get(0);
        for (State s : gp.states) {
            System.out.print(s.toString() + " <- ");
            System.out.println(s.getBackEdge());
        }
        showGraph.highlightGraphPath(gp);
    }

    /** this class makes a GraphBuilderAnnotation selectable in VizGui. It also adds a new
     * toString method that makes more sense in the app.
     * @author mattwigway
     */
    private static class SelectableGraphBuilderAnnotation extends GraphBuilderAnnotation {
        // doesn't much matter, this class is generated by casting anyhow
        public SelectableGraphBuilderAnnotation(GraphBuilderAnnotation.Variety variety, 
                                                Object... refs) {
            super(variety, refs);
        }

        @Override
        public String toString () {
            return getVariety().toString() + " at " + getReferencedObjects();
        }

        public void select (ShowGraph show) {
            Envelope env = new Envelope();

            // I think they will always have the same SRID, no reprojection needed
            // TODO: handle geometries.
            boolean found = false;
            
            // For highlighting
            ArrayList<Vertex> vertices = new ArrayList<Vertex>();

            for (Object obj : getReferencedObjects()) {
                if (obj instanceof Edge) {
                    found = true;
                    Edge e = (Edge) obj;
                    show.enqueueHighlightedEdge(e);
                    env.expandToInclude(e.getFromVertex().getCoordinate());
                    env.expandToInclude(e.getToVertex().getCoordinate());
                }
                else if (obj instanceof Vertex) {
                    found = true;
                    Vertex v = (Vertex) obj;
                    env.expandToInclude(v.getCoordinate());
                    vertices.add(v);
                }
            }

            // nothing associated
            if (!found) return;

            // make it a little bigger, especially needed for STOP_UNLINKED
            env.expandBy(0.02);

            // highlight relevant things
            show.clearHighlights();
            show.setHighlightedVertices(vertices);

            // zoom the graph display
            show.zoomToEnvelope(env);
            
            // and draw
            show.draw();
        }
    }
                

    protected void findAnnotation () {
        Object[] options = GraphBuilderAnnotation.Variety.values();

        GraphBuilderAnnotation.Variety variety = 
            (GraphBuilderAnnotation.Variety) JOptionPane.showInputDialog(
                null, // parentComponent; TODO: set correctly
                "Select the type of annotation to find", // question
                "Select annotation", // title
                JOptionPane.QUESTION_MESSAGE, // message type
                null, // no icon
                options, // options (built above)
                GraphBuilderAnnotation.Variety.STOP_UNLINKED // default value
            );
        
        // User clicked cancel
        if (variety == null) return;

        annotationMatchesModel.clear();

        // loop over the annotations and save the ones of the requested type
        for (GraphBuilderAnnotation anno : graph.getBuilderAnnotations()) {
            if (anno.getVariety() == variety) {
                annotationMatchesModel.addElement(
                    // TODO: is there a polymorphic way to do this?
                    new SelectableGraphBuilderAnnotation(
                        anno.getVariety(),
                        anno.getReferencedObjects().toArray()
                    )
                );
            }
        }

        System.out.println("Found " + annotationMatchesModel.getSize() + " annotations of type " + 
                           variety);
       
                                                                 
    }

    public static void main(String args[]) {
        VizGui gui = null;
    	if (args.length != 1) {
    	    System.out.println("Usage: VizGui /path/to/Graph.obj");
    	    System.exit(1);
    	} else {
    	    gui = new VizGui(args[0]);
    	}
        gui.setVisible(true);

    }

    public void verticesSelected(final List<Vertex> selected) {
        ListModel data = new VertexList(selected);
        nearbyVertices.setModel(data);
    }
    
    public Graph getGraph() {
        return graph;
    }

}

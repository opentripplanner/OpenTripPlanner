package org.opentripplanner.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
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

import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Vertex;

class ExitListener extends WindowAdapter {
    public void windowClosing(WindowEvent event) {
      System.exit(0);
    }
}

class DisplayVertex {
    public Vertex vertex;
    public DisplayVertex(Vertex v) {
        vertex = v;
    }
    public String toString() {
        return vertex.getLabel() + " - " + vertex.getName();
    }   
}

class EdgeListModel extends AbstractListModel {
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


public class VizGui extends JFrame implements VertexSelector {

    private static final long serialVersionUID = 1L;
    private JPanel rightPanel;
    private JPanel leftPanel;
    private ShowGraph showGraph;
    public JList nearbyVertices;
    private JList outgoingEdges;
    private JList incomingEdges;
    
    public VizGui(String graphObj) {
        super();
        
        BorderLayout layout = new BorderLayout();
        setLayout(layout);
        Container pane = getContentPane();

        showGraph = new ShowGraph(this, graphObj);
        pane.add(showGraph, BorderLayout.CENTER);
        
        /* left panel, top-to-bottom: list of nearby vertices, list of edges for selected vertex, buttons */
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
        
        /* right panel currently unused */
        rightPanel = new JPanel();
        pane.add(rightPanel, BorderLayout.LINE_END);
        
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
            public int getSize() { return selected.size(); }
            public Object getElementAt(int index) { return new DisplayVertex(selected.get(index)); }
        };

        nearbyVertices.setModel(data);
        System.out.println(selected);
    }
}

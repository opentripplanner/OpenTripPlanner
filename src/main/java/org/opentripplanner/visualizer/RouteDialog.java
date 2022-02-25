package org.opentripplanner.visualizer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


/**
 * A dialog box to plan a route.
 */
public class RouteDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private JTextField fromField, toField;
    private JButton goButton;
    
    public String from, to;
    
    public RouteDialog(JFrame owner, String initialFrom) {
        super(owner, true);
        fromField = new JTextField(initialFrom, 30);
        toField = new JTextField(30);
        goButton = new JButton("Go");
        
        Container pane = getContentPane();
        
        pane.setLayout(new BoxLayout(pane, BoxLayout.PAGE_AXIS));
        pane.add(new JLabel("From"));
        pane.add(fromField);
        pane.add(new JLabel("To"));
        pane.add(toField);
        pane.add(goButton);
        pack();
        final RouteDialog outer = this;
        goButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                from = fromField.getText().trim();
                to = toField.getText().trim();
                outer.setVisible(false);
            }
            
        });
        setVisible(true);
    }
}

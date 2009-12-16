package org.opentripplanner.gui;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;


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

            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("clicked");
                from = fromField.getText();
                to = toField.getText();
                outer.setVisible(false);
            }
            
        });
        setVisible(true);
    }
}

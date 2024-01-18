package org.opentripplanner.ext.interactivelauncher.debug.raptor;

import static org.opentripplanner.ext.interactivelauncher.support.ViewUtils.addComp;
import static org.opentripplanner.ext.interactivelauncher.support.ViewUtils.addLabel;
import static org.opentripplanner.ext.interactivelauncher.support.ViewUtils.addVerticalSectionSpace;
import static org.opentripplanner.routing.api.request.DebugEventType.DESTINATION_ARRIVALS;
import static org.opentripplanner.routing.api.request.DebugEventType.PATTERN_RIDES;
import static org.opentripplanner.routing.api.request.DebugEventType.STOP_ARRIVALS;

import java.awt.Component;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.function.Consumer;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JTextField;
import org.opentripplanner.routing.api.request.DebugEventType;

/**
 * This UI is used to set Raptor debug parameters, instrument the Raptor
 * search, and log event at decision points during routing.
 */
public class RaptorDebugView {

  private final RaptorDebugModel model;
  private final Box panel = Box.createVerticalBox();
  private final JCheckBox logStopArrivalsChk = new JCheckBox("Stop arrivals");
  private final JCheckBox logPatternRidesChk = new JCheckBox("Pattern rides");
  private final JCheckBox logDestinationArrivalsChk = new JCheckBox("Destination arrivals");
  private final JTextField stopsTxt = new JTextField(40);
  private final JTextField pathTxt = new JTextField(40);

  public RaptorDebugView(RaptorDebugModel model) {
    this.model = model;

    addLabel("Log Raptor events for", panel);
    addComp(logStopArrivalsChk, panel);
    addComp(logPatternRidesChk, panel);
    addComp(logDestinationArrivalsChk, panel);
    addVerticalSectionSpace(panel);

    addLabel("A list of stops to debug", panel);
    addComp(stopsTxt, panel);
    addVerticalSectionSpace(panel);
    addLabel("A a path (as a list of stops) to debug", panel);
    addComp(pathTxt, panel);
    addVerticalSectionSpace(panel);

    initValues();
    setupActionListeners();
  }

  private void initValues() {
    logStopArrivalsChk.setSelected(model.isEventTypeSet(STOP_ARRIVALS));
    logPatternRidesChk.setSelected(model.isEventTypeSet(PATTERN_RIDES));
    logDestinationArrivalsChk.setSelected(model.isEventTypeSet(DESTINATION_ARRIVALS));
    stopsTxt.setText(model.getStops());
    pathTxt.setText(model.getPath());
  }

  private void setupActionListeners() {
    setupActionListenerChkBox(logStopArrivalsChk, STOP_ARRIVALS);
    setupActionListenerChkBox(logPatternRidesChk, PATTERN_RIDES);
    setupActionListenerChkBox(logDestinationArrivalsChk, DESTINATION_ARRIVALS);
    setupActionListenerTextField(stopsTxt, model::setStops);
    setupActionListenerTextField(pathTxt, model::setPath);
  }

  public Component panel() {
    return panel;
  }

  private void setupActionListenerChkBox(JCheckBox box, DebugEventType type) {
    box.addActionListener(l -> model.enableEventTypes(type, box.isSelected()));
  }

  private static void setupActionListenerTextField(JTextField txtField, Consumer<String> model) {
    txtField.addFocusListener(
      new FocusAdapter() {
        @Override
        public void focusLost(FocusEvent e) {
          model.accept(txtField.getText());
        }
      }
    );
  }
}

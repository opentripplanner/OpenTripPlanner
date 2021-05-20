package org.opentripplanner.ext.interactivelauncher.views;

import static org.opentripplanner.ext.interactivelauncher.views.ViewUtils.addComp;
import static org.opentripplanner.ext.interactivelauncher.views.ViewUtils.addSectionDoubleSpace;
import static org.opentripplanner.ext.interactivelauncher.views.ViewUtils.addSectionSpace;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import org.opentripplanner.ext.interactivelauncher.Model;

class OptionsView {
  private final Box panel = Box.createVerticalBox();
  private final JCheckBox buildStreetGraphChk = new JCheckBox("Street graph", false);
  private final JCheckBox buildTransitGraphChk = new JCheckBox("Transit graph", false);
  private final List<JCheckBox> debugLoggingChks = new ArrayList<>();
  private final JCheckBox saveGraphChk = new JCheckBox("Save graph", true);
  private final JCheckBox startOptServerChk = new JCheckBox("Serve graph", true);

  OptionsView(Model model) {
    addComp(new JLabel("Build graph"), panel);
    addSectionSpace(panel);
    addComp(buildStreetGraphChk, panel);
    addComp(buildTransitGraphChk, panel);
    addSectionDoubleSpace(panel);

    // Toggle [ ] save on/off
    buildStreetGraphChk.addActionListener(this::onBuildGraphChkChanged);
    buildTransitGraphChk.addActionListener(this::onBuildGraphChkChanged);

    addComp(new JLabel("Actions"), panel);
    addSectionSpace(panel);
    addComp(saveGraphChk, panel);
    addComp(startOptServerChk, panel);

    addDebugCheckBoxes(model);

    addSectionDoubleSpace(panel);
    initValues(model);
  }

  Box panel() {
    return panel;
  }

  void addActionListener(ActionListener l) {
    buildStreetGraphChk.addActionListener(l);
    buildTransitGraphChk.addActionListener(l);
    saveGraphChk.addActionListener(l);
    startOptServerChk.addActionListener(l);
  }

  private void initValues(Model model) {
    buildStreetGraphChk.setSelected(model.isBuildStreet());
    buildTransitGraphChk.setSelected(model.isBuildTransit());
    saveGraphChk.setSelected(model.isSaveGraph());
    startOptServerChk.setSelected(model.isServeGraph());
  }

  public void updateModel(Model model) {
    model.setBuildStreet(buildStreet());
    model.setBuildTransit(buildTransit());
    model.setSaveGraph(saveGraph());
    model.setServeGraph(startOptServer());
    for (JCheckBox it : debugLoggingChks) {
      model.getDebugLogging().put(it.getText(), it.isSelected());
    }
  }

  void initState() {
    onBuildGraphChkChanged(null);
  }

  private void addDebugCheckBoxes(Model model) {
    addSectionSpace(panel);
    addComp(new JLabel("Debug logging"), panel);
    addSectionSpace(panel);
    var entries = model.getDebugLogging();
    List<String> keys = entries.keySet().stream().sorted().collect(Collectors.toList());
    for (String name : keys) {
      JCheckBox dbox =  new JCheckBox(name, entries.get(name));
      debugLoggingChks.add(dbox);
      addComp(dbox, panel);
    }
  }

  private boolean buildStreet() {
    return buildStreetGraphChk.isSelected();
  }

  private boolean buildTransit() {
    return buildTransitGraphChk.isSelected();
  }

  private boolean saveGraph() {
    return saveGraphChk.isSelected();
  }

  private boolean startOptServer() {
    return startOptServerChk.isSelected();
  }

  private void onBuildGraphChkChanged(ActionEvent e) {
    saveGraphChk.setEnabled(buildStreet() || buildTransit());
    startOptServerChk.setEnabled(buildTransit() || !buildStreet());
  }
}

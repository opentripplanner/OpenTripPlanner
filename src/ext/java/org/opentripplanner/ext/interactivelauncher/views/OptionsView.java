package org.opentripplanner.ext.interactivelauncher.views;

import static org.opentripplanner.ext.interactivelauncher.views.ViewUtils.addComp;
import static org.opentripplanner.ext.interactivelauncher.views.ViewUtils.addSectionDoubleSpace;
import static org.opentripplanner.ext.interactivelauncher.views.ViewUtils.addSectionSpace;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import org.opentripplanner.ext.interactivelauncher.Model;

class OptionsView {

  private final Box panel = Box.createVerticalBox();
  private final JCheckBox buildStreetGraphChk;
  private final JCheckBox buildTransitGraphChk;
  private final JCheckBox saveGraphChk;
  private final JCheckBox startOptServerChk;
  private final JCheckBox startOptVisualizerChk;
  private final Model model;

  OptionsView(Model model) {
    this.model = model;
    this.buildStreetGraphChk = new JCheckBox("Street graph", model.isBuildStreet());
    this.buildTransitGraphChk = new JCheckBox("Transit graph", model.isBuildTransit());
    this.saveGraphChk = new JCheckBox("Save graph", model.isSaveGraph());
    this.startOptServerChk = new JCheckBox("Serve graph", model.isServeGraph());
    this.startOptVisualizerChk = new JCheckBox("Visualizer", model.isVisualizer());

    addComp(new JLabel("Build graph"), panel);
    addSectionSpace(panel);
    addComp(buildStreetGraphChk, panel);
    addComp(buildTransitGraphChk, panel);
    addSectionDoubleSpace(panel);

    // Toggle [ ] save on/off
    buildStreetGraphChk.addActionListener(e -> onBuildGraphChkChanged());
    buildTransitGraphChk.addActionListener(e -> onBuildGraphChkChanged());
    startOptServerChk.addActionListener(e -> onStartOptServerChkChanged());

    addComp(new JLabel("Actions"), panel);
    addSectionSpace(panel);
    addComp(saveGraphChk, panel);
    addComp(startOptServerChk, panel);
    addComp(startOptVisualizerChk, panel);

    addDebugCheckBoxes(model);
    addSectionDoubleSpace(panel);
    bindCheckBoxesToModel();
  }

  Box panel() {
    return panel;
  }

  void initState() {
    onBuildGraphChkChanged();
  }

  void bind(JCheckBox box, Consumer<Boolean> modelUpdate) {
    box.addActionListener(l -> modelUpdate.accept(box.isSelected() && box.isEnabled()));
  }

  private void addDebugCheckBoxes(Model model) {
    addSectionSpace(panel);
    addComp(new JLabel("Debug logging"), panel);
    addSectionSpace(panel);
    var entries = model.getDebugLogging();
    List<String> keys = entries.keySet().stream().sorted().collect(Collectors.toList());
    for (String name : keys) {
      JCheckBox box = new JCheckBox(name, entries.get(name));
      box.addActionListener(l -> model.getDebugLogging().put(name, box.isSelected()));
      addComp(box, panel);
    }
  }

  private void bindCheckBoxesToModel() {
    bind(buildStreetGraphChk, model::setBuildStreet);
    bind(buildTransitGraphChk, model::setBuildTransit);
    bind(saveGraphChk, model::setSaveGraph);
    bind(startOptServerChk, model::setServeGraph);
    bind(startOptVisualizerChk, model::setVisualizer);
  }

  private boolean buildStreet() {
    return buildStreetGraphChk.isSelected();
  }

  private boolean buildTransit() {
    return buildTransitGraphChk.isSelected();
  }

  private void onBuildGraphChkChanged() {
    saveGraphChk.setEnabled(buildStreet() || buildTransit());
    startOptServerChk.setEnabled(buildTransit() || !buildStreet());
    startOptVisualizerChk.setEnabled(buildTransit() || !buildStreet());
  }

  private void onStartOptServerChkChanged() {
    startOptVisualizerChk.setEnabled(
      startOptServerChk.isEnabled() && startOptServerChk.isSelected()
    );
  }
}

package org.opentripplanner.ext.interactivelauncher.startup;

import static org.opentripplanner.ext.interactivelauncher.support.ViewUtils.addComp;
import static org.opentripplanner.ext.interactivelauncher.support.ViewUtils.addLabel;
import static org.opentripplanner.ext.interactivelauncher.support.ViewUtils.addVerticalSectionSpace;

import java.util.function.Consumer;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComponent;

class OptionsView {

  private final Box panel = Box.createHorizontalBox();
  private final JCheckBox buildStreetGraphChk;
  private final JCheckBox buildTransitGraphChk;
  private final JCheckBox saveGraphChk;
  private final JCheckBox startOptServerChk;
  private final JCheckBox startOptVisualizerChk;
  private final StartupModel model;

  OptionsView(StartupModel model) {
    this.model = model;
    this.buildStreetGraphChk = new JCheckBox("Street graph", model.isBuildStreet());
    this.buildTransitGraphChk = new JCheckBox("Transit graph", model.isBuildTransit());
    this.saveGraphChk = new JCheckBox("Save graph", model.isSaveGraph());
    this.startOptServerChk = new JCheckBox("Serve graph", model.isServeGraph());
    this.startOptVisualizerChk = new JCheckBox("Visualizer", model.isVisualizer());

    panel.add(Box.createGlue());
    addComp(createBuildBox(), panel);
    panel.add(Box.createGlue());
    addComp(createActionBox(), panel);
    panel.add(Box.createGlue());

    // Toggle [ ] save on/off
    buildStreetGraphChk.addActionListener(e -> onBuildGraphChkChanged());
    buildTransitGraphChk.addActionListener(e -> onBuildGraphChkChanged());
    startOptServerChk.addActionListener(e -> onStartOptServerChkChanged());

    //addSectionDoubleSpace(panel);
    bindCheckBoxesToModel();
  }

  private JComponent createBuildBox() {
    var buildBox = Box.createVerticalBox();
    addLabel("Build graph", buildBox);
    addVerticalSectionSpace(buildBox);
    addComp(buildStreetGraphChk, buildBox);
    addComp(buildTransitGraphChk, buildBox);
    buildBox.add(Box.createVerticalGlue());
    return buildBox;
  }

  private JComponent createActionBox() {
    var actionBox = Box.createVerticalBox();
    addLabel("Actions", actionBox);
    addVerticalSectionSpace(actionBox);
    addComp(saveGraphChk, actionBox);
    addComp(startOptServerChk, actionBox);
    addComp(startOptVisualizerChk, actionBox);
    return actionBox;
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

package org.opentripplanner.ext.interactivelauncher.views;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static org.opentripplanner.ext.interactivelauncher.views.ViewUtils.addComp;
import static org.opentripplanner.ext.interactivelauncher.views.ViewUtils.addSectionDoubleSpace;
import static org.opentripplanner.ext.interactivelauncher.views.ViewUtils.addSectionSpace;

class OptionsView {
  private final Box panel = Box.createVerticalBox();
  private final JCheckBox buildStreetGraphChk = new JCheckBox("Street graph", false);
  private final JCheckBox buildTransitGraphChk = new JCheckBox("Transit graph", false);
  private final JCheckBox saveGraphChk = new JCheckBox("Save graph", true);
  private final JCheckBox startOptServerChk = new JCheckBox("Serve graph", true);

  OptionsView() {
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
    addSectionDoubleSpace(panel);
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

  void initState() {
    onBuildGraphChkChanged(null);
  }

  boolean buildStreet() {
    return buildStreetGraphChk.isSelected();
  }

  boolean buildTransit() {
    return buildTransitGraphChk.isSelected();
  }

  boolean saveGraph() {
    return saveGraphChk.isSelected();
  }

  boolean startOptServer() {
    return startOptServerChk.isSelected();
  }

  private void onBuildGraphChkChanged(ActionEvent e) {
    saveGraphChk.setEnabled(buildStreet() || buildTransit());
    startOptServerChk.setEnabled(buildTransit() || !buildStreet());
  }
}

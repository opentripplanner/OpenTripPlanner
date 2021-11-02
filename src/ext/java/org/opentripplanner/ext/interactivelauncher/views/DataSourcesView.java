package org.opentripplanner.ext.interactivelauncher.views;

import java.util.stream.Collectors;
import org.opentripplanner.ext.interactivelauncher.Model;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import static org.opentripplanner.ext.interactivelauncher.views.ViewUtils.addComp;
import static org.opentripplanner.ext.interactivelauncher.views.ViewUtils.addSectionDoubleSpace;
import static org.opentripplanner.ext.interactivelauncher.views.ViewUtils.addSectionSpace;

class DataSourcesView {
  private final Box panel = Box.createVerticalBox();
  private final Box dataSourceSelectionPanel = Box.createVerticalBox();
  private final Model model;

  public DataSourcesView(Model model) {
    this.model = model;

    setupDataSources();

    addComp(new JLabel("Select data source"), panel);
    addSectionSpace(panel);
    addComp(dataSourceSelectionPanel, panel);
    addSectionDoubleSpace(panel);
  }

  public Box panel() {
    return panel;
  }

  private void setupDataSources() {
    final List<String> values = model.getDataSourceOptions();

    if(values.isEmpty()) {
      model.setDataSource(null);
      JLabel label = new JLabel("<No otp configuration files found>");
      label.setBackground(ViewUtils.BG_STATUS_BAR);
      label.setForeground(ViewUtils.FG_STATUS_BAR);
      addComp(label, dataSourceSelectionPanel);
      return;
    }

    String selectedValue = model.getDataSource();

    if(selectedValue == null) {
      selectedValue = values.get(0);
      model.setDataSource(selectedValue);
    }

    ButtonGroup selectDataSourceRadioGroup = new ButtonGroup();

    List<String> valuesSorted = values.stream().sorted().collect(Collectors.toList());

    for (String name : valuesSorted) {
      boolean selected = selectedValue.equals(name);
      JRadioButton radioBtn = newRadioBtn(selectDataSourceRadioGroup, name, selected);
      radioBtn.addActionListener(this::onDataSourceChange);
      addComp(radioBtn, dataSourceSelectionPanel);
    }
  }

  public void onRootDirChange() {
    model.setDataSource(null);
    dataSourceSelectionPanel.removeAll();
    setupDataSources();
    panel.repaint();
  }

  public void onDataSourceChange(ActionEvent e) {
    model.setDataSource(e.getActionCommand());
  }

  private static JRadioButton newRadioBtn(ButtonGroup group, String name, boolean selected) {
    JRadioButton radioButton = new JRadioButton(name, selected);
    group.add(radioButton);
    return radioButton;
  }
}

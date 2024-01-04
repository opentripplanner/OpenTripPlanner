package org.opentripplanner.ext.interactivelauncher.startup;

import static org.opentripplanner.ext.interactivelauncher.support.ViewUtils.addComp;
import static org.opentripplanner.ext.interactivelauncher.support.ViewUtils.addHorizontalGlue;
import static org.opentripplanner.ext.interactivelauncher.support.ViewUtils.addLabel;
import static org.opentripplanner.ext.interactivelauncher.support.ViewUtils.addVerticalSectionSpace;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.List;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import org.opentripplanner.ext.interactivelauncher.support.ViewUtils;

class DataSourcesView {

  /*
  |-----------------------------------------------|
  | Label                                         |
  |-----------------------------------------------|
  |  ( ) List 1   |  ( ) List 2   |  ( ) List 3   |
  |  ( ) List 1   |  ( ) List 2   |  ( ) List 3   |
  |-----------------------------------------------|
 */

  private final Box mainPanel = Box.createVerticalBox();
  private final Box listPanel = Box.createHorizontalBox();
  private final StartupModel model;

  public DataSourcesView(StartupModel model) {
    this.model = model;
    setupDataSources();

    addLabel("Select data source", mainPanel);
    addVerticalSectionSpace(mainPanel);

    listPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    addComp(listPanel, mainPanel);
  }

  public JComponent panel() {
    return mainPanel;
  }

  public void onRootDirChange() {
    model.setDataSource(null);
    listPanel.removeAll();
    setupDataSources();
    listPanel.repaint();
  }

  public void onDataSourceChange(ActionEvent e) {
    model.setDataSource(e.getActionCommand());
  }

  private void setupDataSources() {
    final List<String> values = model.getDataSourceOptions();

    if (values.isEmpty()) {
      model.setDataSource(null);
      var label = new JLabel("<No otp configuration files found>");
      label.setBackground(ViewUtils.BG_STATUS_BAR);
      label.setForeground(ViewUtils.FG_STATUS_BAR);
      addComp(label, listPanel);
      return;
    }

    String selectedValue = model.getDataSource();

    if (selectedValue == null) {
      selectedValue = values.get(0);
      model.setDataSource(selectedValue);
    }

    ButtonGroup selectDataSourceRadioGroup = new ButtonGroup();

    List<String> valuesSorted = values.stream().sorted().toList();
    int size = valuesSorted.size();

    // Split the list of configuration in one, two or three columns depending on the
    // number of configurations found.
    if (size <= 10) {
      addListPanel(valuesSorted, selectedValue, selectDataSourceRadioGroup);
    } else if (size <= 20) {
      int half = size / 2;
      addListPanel(valuesSorted.subList(0, half), selectedValue, selectDataSourceRadioGroup);
      addHorizontalGlue(listPanel);
      addListPanel(valuesSorted.subList(half, size), selectedValue, selectDataSourceRadioGroup);
    } else {
      int third = size / 3;
      addListPanel(valuesSorted.subList(0, third), selectedValue, selectDataSourceRadioGroup);
      addHorizontalGlue(listPanel);
      addListPanel(
        valuesSorted.subList(third, third * 2),
        selectedValue,
        selectDataSourceRadioGroup
      );
      addHorizontalGlue(listPanel);
      addListPanel(
        valuesSorted.subList(third * 2, size),
        selectedValue,
        selectDataSourceRadioGroup
      );
    }
  }

  private void addListPanel(
    List<String> values,
    String selectedValue,
    ButtonGroup selectDataSourceRadioGroup
  ) {
    Box column = Box.createVerticalBox();

    for (String name : values) {
      boolean selected = selectedValue.equals(name);
      JRadioButton radioBtn = newRadioBtn(selectDataSourceRadioGroup, name, selected);
      radioBtn.addActionListener(this::onDataSourceChange);
      addComp(radioBtn, column);
    }
    addComp(column, listPanel);
  }

  private static JRadioButton newRadioBtn(ButtonGroup group, String name, boolean selected) {
    JRadioButton radioButton = new JRadioButton(name, selected);
    group.add(radioButton);
    return radioButton;
  }
}

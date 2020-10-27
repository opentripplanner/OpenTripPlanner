package org.opentripplanner.ext.interactivelauncher.views;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.opentripplanner.ext.interactivelauncher.views.ViewUtils.addComp;
import static org.opentripplanner.ext.interactivelauncher.views.ViewUtils.addSectionDoubleSpace;
import static org.opentripplanner.ext.interactivelauncher.views.ViewUtils.addSectionSpace;

class ConfigDirsView {
  private final Box panel = Box.createVerticalBox();
  private final Map<JRadioButton, File> dataDirs = new HashMap<>();

  public ConfigDirsView(java.util.List<File> configDirs, String configDirPrefix) {
      ButtonGroup selectDataDir = new ButtonGroup();
      addComp(new JLabel("Select OTP data source"), panel);
      addSectionSpace(panel);

      boolean first = true;
      for (File dir : configDirs) {
        String name = dir.getAbsolutePath().substring(configDirPrefix.length());
        JRadioButton radioBtn = newRadioBtn(selectDataDir, name, first);
        dataDirs.put(radioBtn, dir);
        addComp(radioBtn, panel);
        first = false;
      }
      addSectionDoubleSpace(panel);
  }

  public void addActionListener(ActionListener l) {
    dataDirs.keySet().forEach(it -> it.addActionListener(l));
  }

  public Box panel() {
    return panel;
  }

  public File getSelectedOtpConfigDataDir() {
    for (Map.Entry<JRadioButton, File> e : dataDirs.entrySet()) {
      if(e.getKey().isSelected()) {
        return e.getValue();
      }
    }
    throw new IllegalStateException("No datasource selected - programming error!");
  }

  private static JRadioButton newRadioBtn(ButtonGroup group, String name, boolean selected) {
    JRadioButton radioButton = new JRadioButton(name, selected);
    group.add(radioButton);
    return radioButton;
  }
}

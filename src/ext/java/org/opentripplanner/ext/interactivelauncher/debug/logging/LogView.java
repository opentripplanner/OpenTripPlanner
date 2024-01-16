package org.opentripplanner.ext.interactivelauncher.debug.logging;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComponent;

/**
 * Display a list of loggers to turn on/off.
 */
public class LogView {

  private final Box panel = Box.createVerticalBox();
  private final LogModel model;

  public LogView(LogModel model) {
    this.model = model;
    DebugLoggers.list().forEach(this::add);
  }

  public JComponent panel() {
    return panel;
  }

  private void add(DebugLoggers.Entry entry) {
    var box = new JCheckBox(entry.label());
    box.setToolTipText("Logger: " + entry.logger());
    box.setSelected(model.isLoggerEnabled(entry.logger()));
    box.addActionListener(e -> selectLogger(entry.logger(), box.isSelected()));
    panel.add(box);
  }

  private void selectLogger(String logger, boolean selected) {
    model.turnLoggerOnOff(logger, selected);
  }
}

package org.opentripplanner.ext.interactivelauncher.startup;

import static org.opentripplanner.ext.interactivelauncher.support.ViewUtils.BG_STATUS_BAR;
import static org.opentripplanner.ext.interactivelauncher.support.ViewUtils.FG_STATUS_BAR;

import java.awt.Component;
import java.awt.Dimension;
import java.io.File;
import java.util.function.Consumer;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JTextField;
import org.opentripplanner.ext.interactivelauncher.support.ViewUtils;

class DataSourceRootView {

  private final Box panel;
  private final JTextField fileTxt = new JTextField();
  private final JButton searchBtn = new JButton("Open");
  private final Consumer<String> rootDirChangedListener;

  DataSourceRootView(String dir, Consumer<String> rootDirChangedListener) {
    this.fileTxt.setText(dir);
    this.rootDirChangedListener = rootDirChangedListener;

    panel = Box.createVerticalBox();

    // Add label
    JLabel lbl = new JLabel("Data source root");
    lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
    panel.add(lbl);

    // Configure text field
    fileTxt.setEditable(false);
    fileTxt.setBackground(BG_STATUS_BAR);
    fileTxt.setForeground(FG_STATUS_BAR);

    // Add text field and open button
    Box box = Box.createHorizontalBox();
    box.setAlignmentX(Component.LEFT_ALIGNMENT);
    box.add(fileTxt);
    box.add(searchBtn);
    searchBtn.addActionListener(l -> onSelectSource());
    panel.add(box);
  }

  public JComponent panel() {
    return panel;
  }

  Dimension minWidth(Dimension d, int minWidth) {
    return new Dimension(Math.max(minWidth, d.width), d.height);
  }

  private void onSelectSource() {
    JFileChooser chooser = new JFileChooser(new File(fileTxt.getText()));
    chooser.setBackground(ViewUtils.BACKGROUND);
    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    chooser.setDialogTitle("Choose directory to search");
    chooser.setCurrentDirectory(new File(fileTxt.getText()));
    chooser.setApproveButtonToolTipText("Select the directory to search for OTP data sources");
    int status = chooser.showDialog(panel, "Search");

    if (status == JFileChooser.APPROVE_OPTION) {
      File dir = chooser.getSelectedFile();
      if (!dir.exists()) {
        dir = dir.getParentFile();
      }
      fileTxt.setText(dir.getAbsolutePath());
      rootDirChangedListener.accept(dir.getAbsolutePath());
    }
  }
}

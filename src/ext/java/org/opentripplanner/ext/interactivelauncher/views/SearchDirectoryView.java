package org.opentripplanner.ext.interactivelauncher.views;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.function.Consumer;

import static org.opentripplanner.ext.interactivelauncher.views.ViewUtils.BG_STATUS_BAR;
import static org.opentripplanner.ext.interactivelauncher.views.ViewUtils.FG_STATUS_BAR;

public class SearchDirectoryView {
  private final Box panel;
  private final JTextField fileTxt = new JTextField();
  private final JButton searchBtn = new JButton("Open");
  private final Consumer<String> rootDirChangedListener;

  public SearchDirectoryView(String dir, Consumer<String> rootDirChangedListener) {
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
    //var d = minWidth(fileTxt.getPreferredSize(), 460);
    //fileTxt.setMinimumSize(d);
    //fileTxt.setPreferredSize(d);

    // Add text field and open button
    Box box = Box.createHorizontalBox();
    box.setAlignmentX(Component.LEFT_ALIGNMENT);
    box.add(fileTxt);
    box.add(searchBtn);
    searchBtn.addActionListener(this::onSelectSource);
    panel.add(box);
  }

  public Box panel() {
    return panel;
  }

  private void onSelectSource(ActionEvent l) {
    JFileChooser chooser = new JFileChooser(new File(fileTxt.getText()));
    chooser.setBackground(ViewUtils.BACKGROUND);
    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    chooser.setDialogTitle("Choose directory to search");
    chooser.setCurrentDirectory(new File(fileTxt.getText()));
    chooser.setApproveButtonToolTipText("Select the directory to search for OTP data sources");
    int status = chooser.showDialog(panel, "Search");

    if(status == JFileChooser.APPROVE_OPTION) {
      File dir = chooser.getSelectedFile();
      if(!dir.exists()) {
        dir = dir.getParentFile();
      }
      fileTxt.setText(dir.getAbsolutePath());
      rootDirChangedListener.accept(dir.getAbsolutePath());
    }
  }

  Dimension minWidth(Dimension d, int minWidth) {
    return new Dimension(Math.max(minWidth, d.width), d.height);
  }
}

/*
 * Copyright 2020 Patrik Karlström.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.trixon.nbpackager_core;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JButton;
import javax.swing.event.DocumentListener;
import org.apache.commons.lang3.SystemUtils;
import se.trixon.almond.util.GraphicsHelper;
import se.trixon.almond.util.icons.material.swing.MaterialIcon;
import se.trixon.almond.util.swing.dialogs.FileChooserPanel;
import static se.trixon.nbpackager_core.Options.*;

/**
 *
 * @author Patrik Karlström
 */
public class OptionsPanel extends javax.swing.JPanel {

    private final Options mOptions = Options.getInstance();

    /**
     * Creates new form OptionsPanel
     */
    public OptionsPanel() {
        MaterialIcon.setDefaultColor(GraphicsHelper.getBrightness(new JButton().getBackground()) < 128 ? Color.WHITE : Color.BLACK);
        initComponents();
        appImageFileChooserPanel.setEnabled(SystemUtils.IS_OS_LINUX);
        appimagetoolOptionsTextField.setEnabled(SystemUtils.IS_OS_LINUX);

    }

    public void load() {
        appImageFileChooserPanel.setPath(mOptions.get(OPT_APP_IMAGE_TOOL, "/path/to/appimagetool-x86_64.AppImage"));
        appimagetoolOptionsTextField.setText(mOptions.get(OPT_APP_IMAGE_OPTIONS, "-nv"));
    }

    public void save() {
        mOptions.put(OPT_APP_IMAGE_TOOL, appImageFileChooserPanel.getPath());
        mOptions.put(OPT_APP_IMAGE_OPTIONS, appimagetoolOptionsTextField.getText());
    }

    public void setDocumentListener(DocumentListener documentListener) {
        for (Component component : getComponents()) {
            if (component instanceof FileChooserPanel) {
                ((FileChooserPanel) component).getTextField().getDocument().addDocumentListener(documentListener);
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        appImageFileChooserPanel = new se.trixon.almond.util.swing.dialogs.FileChooserPanel();
        appimagetoolOptionsLabel = new javax.swing.JLabel();
        appimagetoolOptionsTextField = new javax.swing.JTextField();

        appImageFileChooserPanel.setHeader("AppImageTool");

        appimagetoolOptionsLabel.setText("AppImageTool options");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(appImageFileChooserPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 388, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(appimagetoolOptionsLabel)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(appimagetoolOptionsTextField))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(appImageFileChooserPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(appimagetoolOptionsLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(appimagetoolOptionsTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(179, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private se.trixon.almond.util.swing.dialogs.FileChooserPanel appImageFileChooserPanel;
    private javax.swing.JLabel appimagetoolOptionsLabel;
    private javax.swing.JTextField appimagetoolOptionsTextField;
    // End of variables declaration//GEN-END:variables
}

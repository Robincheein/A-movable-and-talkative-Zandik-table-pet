package com.group_finity.mascot;

import com.group_finity.mascot.chat.ChatManager;
import javax.swing.*;
import java.awt.*;

/** 桌宠外观和聊天窗口设置。 */
public class SettingsDialog extends JDialog {
    private final JSpinner widthSpinner;
    private final JSpinner heightSpinner;
    private final JSpinner scaleSpinner;
    private final JSpinner opacitySpinner;
    private final JComboBox<String> fontCombo;
    private final JSpinner fontSizeSpinner;

    public static void open(Mascot mascot) {
        SwingUtilities.invokeLater(() -> {
            Window owner = mascot == null ? null : mascot.getWindow().asJWindow();
            new SettingsDialog(owner).setVisible(true);
        });
    }

    private SettingsDialog(Window owner) {
        super(owner, "多托雷桌宠设置", ModalityType.APPLICATION_MODAL);
        SettingsManager s = SettingsManager.getInstance();
        widthSpinner = new JSpinner(new SpinnerNumberModel(s.getDialogWidth(), 280, 1000, 20));
        heightSpinner = new JSpinner(new SpinnerNumberModel(s.getDialogHeight(), 180, 800, 20));
        scaleSpinner = new JSpinner(new SpinnerNumberModel((int)Math.round(s.getCharacterScale()*100), 40, 150, 5));
        opacitySpinner = new JSpinner(new SpinnerNumberModel((int)Math.round(s.getCharacterOpacity()*100), 20, 100, 5));
        fontCombo = new JComboBox<>(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        fontCombo.setSelectedItem(s.getFontFamily());
        fontSizeSpinner = new JSpinner(new SpinnerNumberModel(s.getFontSize(), 12, 32, 1));
        initUi();
    }

    private void initUi() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setAlwaysOnTop(true);
        setResizable(false);
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(14,16,8,16));
        addRow(form,0,"对话框宽度",widthSpinner,"像素");
        addRow(form,1,"对话框高度",heightSpinner,"像素");
        addRow(form,2,"人物大小",scaleSpinner,"%");
        addRow(form,3,"人物透明度",opacitySpinner,"%");
        addRow(form,4,"聊天字体",fontCombo,"");
        addRow(form,5,"字体大小",fontSizeSpinner,"磅");
        JLabel note = new JLabel("保存后立即生效；设置会在下次启动时保留。");
        note.setBorder(BorderFactory.createEmptyBorder(4,16,4,16));
        JButton cancel = new JButton("取消"); cancel.addActionListener(e -> dispose());
        JButton save = new JButton("保存并应用"); save.addActionListener(e -> applySettings());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT)); buttons.add(cancel); buttons.add(save);
        JPanel bottom = new JPanel(new BorderLayout()); bottom.add(note,BorderLayout.NORTH); bottom.add(buttons,BorderLayout.SOUTH);
        add(form,BorderLayout.CENTER); add(bottom,BorderLayout.SOUTH);
        getRootPane().setDefaultButton(save); pack(); setLocationRelativeTo(getOwner());
    }

    private void addRow(JPanel panel,int row,String label,Component field,String unit) {
        GridBagConstraints c=new GridBagConstraints(); c.gridy=row; c.insets=new Insets(5,4,5,4); c.anchor=GridBagConstraints.WEST;
        c.gridx=0; panel.add(new JLabel(label),c);
        c.gridx=1; c.fill=GridBagConstraints.HORIZONTAL; c.weightx=1; panel.add(field,c);
        c.gridx=2; c.fill=GridBagConstraints.NONE; c.weightx=0; panel.add(new JLabel(unit),c);
    }

    private void applySettings() {
        try {
            int width=((Number)widthSpinner.getValue()).intValue();
            int height=((Number)heightSpinner.getValue()).intValue();
            double scale=((Number)scaleSpinner.getValue()).doubleValue()/100.0;
            float opacity=((Number)opacitySpinner.getValue()).floatValue()/100.0f;
            String font=String.valueOf(fontCombo.getSelectedItem());
            int size=((Number)fontSizeSpinner.getValue()).intValue();
            SettingsManager.getInstance().update(width,height,scale,opacity,font,size);
            Main.getInstance().applyVisualSettings();
            ChatManager.getInstance().applyUiSettings();
            dispose();
        } catch(Exception e) {
            JOptionPane.showMessageDialog(this,"保存设置失败："+e.getMessage(),"设置错误",JOptionPane.ERROR_MESSAGE);
        }
    }
}
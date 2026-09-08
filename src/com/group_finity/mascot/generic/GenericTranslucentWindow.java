/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.sun.jna.platform.WindowUtils
 */
package com.group_finity.mascot.generic;

import com.group_finity.mascot.generic.GenericNativeImage;
import com.group_finity.mascot.image.NativeImage;
import com.group_finity.mascot.image.TranslucentWindow;
import com.sun.jna.platform.WindowUtils;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Window;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JWindow;

class GenericTranslucentWindow
extends JWindow
implements TranslucentWindow {
    private static final long serialVersionUID = 1L;
    private GenericNativeImage image;
    private JPanel panel;
    private float alpha = 1.0f;

    public GenericTranslucentWindow() {
        super(WindowUtils.getAlphaCompatibleGraphicsConfiguration());
        this.init();
        this.panel = new JPanel(){
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                g.drawImage(GenericTranslucentWindow.this.getImage().getManagedImage(), 0, 0, null);
            }
        };
        this.setContentPane(this.panel);
    }

    private void init() {
        System.setProperty("sun.java2d.noddraw", "true");
        System.setProperty("sun.java2d.opengl", "true");
    }

    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);
        if (b) {
            WindowUtils.setWindowTransparent((Window)this, (boolean)true);
            this.setAlpha(this.alpha);
        }
    }

    @Override
    protected void addImpl(Component comp, Object constraints, int index) {
        super.addImpl(comp, constraints, index);
        if (comp instanceof JComponent) {
            JComponent jcomp = (JComponent)comp;
            jcomp.setOpaque(false);
        }
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
        WindowUtils.setWindowAlpha((Window)this, (float)alpha);
    }

    public float getAlpha() {
        return this.alpha;
    }

    @Override
    public JWindow asJWindow() {
        return this;
    }

    @Override
    public String toString() {
        return "LayeredWindow[hashCode=" + this.hashCode() + ",bounds=" + this.getBounds() + "]";
    }

    public GenericNativeImage getImage() {
        return this.image;
    }

    @Override
    public void setImage(NativeImage image) {
        this.image = (GenericNativeImage)image;
    }

    @Override
    public void updateImage() {
        WindowUtils.setWindowMask((Window)this, (Icon)this.getImage().getIcon());
        this.validate();
        this.repaint();
    }

    @Override
    public void setWindowOpacity(float opacity) {
        this.setAlpha(opacity);
    }
}


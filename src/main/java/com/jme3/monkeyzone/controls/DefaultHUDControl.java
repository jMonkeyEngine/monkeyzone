/*
 * Copyright (c) 2009-2011 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.monkeyzone.controls;

import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import de.lessvoid.nifty.elements.render.TextRenderer;
import de.lessvoid.nifty.screen.Screen;
import java.io.IOException;

/**
 * Default HUD control, updates UI with spatial data.
 * @author normenhansen
 */
public class DefaultHUDControl implements HUDControl {

    protected boolean enabled = true;
    protected Spatial spatial;
    protected Screen screen;
    protected float updateTime = 0.25f;
    protected float curTime = 1;
    protected TextRenderer hitPoints;
    protected TextRenderer speed;
    protected TextRenderer vehicle;

    public DefaultHUDControl(Screen screen) {
        this.screen = screen;
        if (screen == null) {
            throw new IllegalStateException("DefaultHUDControl nifty screen null!");
        }
        hitPoints = screen.findElementByName("layer").findElementById("panel_bottom").findElementById("bottom_panel_left").findElementById("status_text_01").getRenderer(TextRenderer.class);
        speed = screen.findElementByName("layer").findElementById("panel_bottom").findElementById("bottom_panel_left").findElementById("status_text_02").getRenderer(TextRenderer.class);
        vehicle = screen.findElementByName("layer").findElementById("panel_bottom").findElementById("bottom_panel_left").findElementById("status_text_03").getRenderer(TextRenderer.class);
    }

    public void setSpatial(Spatial spatial) {
        if (spatial == null) {
            this.spatial = spatial;
            return;
        }
        this.spatial = spatial;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void update(float tpf) {
        if (!enabled) {
            return;
        }
        curTime += tpf;
        if (curTime > updateTime) {
            curTime = 0;
            Float hitPoints = (Float) spatial.getUserData("HitPoints");
            Float speed = (Float) spatial.getUserData("Speed");
            if (hitPoints != null) {
                this.hitPoints.setText("HP:" + hitPoints);
            } else {
                this.hitPoints.setText("No HitPoints!");
            }
            if (speed != null) {
                this.speed.setText("Speed: " + speed);
            } else {
                this.speed.setText("No HitPoints!");
            }
        }
    }

    public void render(RenderManager rm, ViewPort vp) {
    }

    public Control cloneForSpatial(Spatial spatial) {
        throw new UnsupportedOperationException("Not supported.");
    }

    public void write(JmeExporter ex) throws IOException {
        throw new UnsupportedOperationException("Not supported.");
    }

    public void read(JmeImporter im) throws IOException {
        throw new UnsupportedOperationException("Not supported.");
    }
}

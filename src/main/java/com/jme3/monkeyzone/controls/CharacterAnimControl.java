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

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import java.io.IOException;

/**
 * Handles animation of character
 * @author normenhansen
 */
public class CharacterAnimControl implements Control {

    protected boolean enabled = true;
    protected Spatial spatial;
    protected AnimControl animControl;
    protected CharacterControl characterControl;
    protected AnimChannel torsoChannel;
    protected AnimChannel feetChannel;

    public CharacterAnimControl() {
    }

    public void setSpatial(Spatial spatial) {
        if (spatial == null) {
            return;
        }
        animControl = spatial.getControl(AnimControl.class);
        characterControl = spatial.getControl(CharacterControl.class);
        if (animControl != null && characterControl != null) {
            enabled = true;
            torsoChannel = animControl.createChannel();
            feetChannel = animControl.createChannel();
        }
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
        if(!characterControl.onGround()){
            if(!"JumpLoop".equals(torsoChannel.getAnimationName()))
                torsoChannel.setAnim("JumpLoop");
            if(!"JumpLoop".equals(feetChannel.getAnimationName()))
                feetChannel.setAnim("JumpLoop");
            return;
        }
        if (characterControl.getWalkDirection().length() > 0) {
            if(!"RunTop".equals(torsoChannel.getAnimationName()))
                torsoChannel.setAnim("RunTop");
            if(!"RunBase".equals(feetChannel.getAnimationName()))
                feetChannel.setAnim("RunBase");
        }else{
            if(!"IdleTop".equals(torsoChannel.getAnimationName()))
                torsoChannel.setAnim("IdleTop");
            if(!"IdleBase".equals(feetChannel.getAnimationName()))
                feetChannel.setAnim("IdleBase");
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

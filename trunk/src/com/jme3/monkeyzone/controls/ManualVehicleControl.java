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

import com.jme3.bullet.control.VehicleControl;
import com.jme3.math.FastMath;
import com.jme3.network.connection.Client;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;

/**
 * Manual vehicle control, implements ManualControl interface and controls
 * a vehicle if available on the Spatial.
 * @author normenhansen
 */
public class ManualVehicleControl extends NetworkedManualControl {

    private Spatial spatial;
    private VehicleControl control;
    private float speed = 800f;
    private float steer = 0;
    private float accelerate = 0;

    public ManualVehicleControl() {
    }

    public ManualVehicleControl(Client client, long entityId) {
        super(client, entityId);
    }

    @Override
    public void doSteerX(float amount) {
    }

    @Override
    public void doSteerY(float amount) {
    }

    @Override
    public void doMoveX(float amount) {
        steer = amount * FastMath.QUARTER_PI * 0.5f;
    }

    @Override
    public void doMoveY(float amount) {
    }

    @Override
    public void doMoveZ(float amount) {
        accelerate = amount * speed;
    }

    @Override
    public void setSpatial(Spatial spatial) {
        this.spatial = spatial;
        if (spatial == null) {
            return;
        }
        this.control = spatial.getControl(VehicleControl.class);
        if (this.control == null) {
            throw new IllegalStateException("Cannot add ManualCharacterControl to Spatial without CharacterControl");
        }
        Float spatialSpeed = (Float) spatial.getUserData("speed");
        if (spatialSpeed != null) {
            speed = spatialSpeed;
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void update(float tpf) {
        if (!enabled) {
            return;
        }
        control.accelerate(accelerate);
        control.steer(steer);
    }

    public void render(RenderManager rm, ViewPort vp) {
    }
}

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

import com.jme3.math.Vector3f;
import com.jme3.network.connection.Client;
import com.jme3.network.physicssync.PhysicsSyncManager;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;

/**
 * TODO: autonomous vehicle control
 * @author normenhansen
 */
public class AutonomousVehicleControl extends NetworkedAutonomousControl {

    public AutonomousVehicleControl() {
    }

    public AutonomousVehicleControl(Client client, long entityId) {
        super(client, entityId);
    }

    public void doAimAt(Vector3f direction) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void doMoveTo(Vector3f location) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void doPerformAction(int action, boolean activate) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Vector3f getAimDirection() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isMoving() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Vector3f getTargetLocation() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Vector3f getCurrentLocation() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setSpatial(Spatial spatial) {
        this.spatial = spatial;
        if (spatial == null) {
            return;
        }
    }

    public void update(float tpf) {
    }

    public void render(RenderManager rm, ViewPort vp) {
    }
}

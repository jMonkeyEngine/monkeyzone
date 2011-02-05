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

import com.jme3.bullet.control.CharacterControl;
import com.jme3.math.Vector3f;
import com.jme3.monkeyzone.Globals;
import com.jme3.network.connection.Client;
import com.jme3.network.physicssync.PhysicsSyncManager;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;

/**
 * Automomous character control, implements the AutonomousControl interface and
 * controls a character if available on the spatial.
 * @author normenhansen
 */
public class AutonomousCharacterControl extends NetworkedAutonomousControl {

    private float checkRadius = 2;
    private float speed = 10f * Globals.PHYSICS_FPS;
    private Vector3f targetLocation = new Vector3f();
    private Vector3f vector = new Vector3f();
    private Vector3f vector2 = new Vector3f();
    private boolean moving = false;
    private CharacterControl characterControl;
    private Vector3f aimDirection = new Vector3f(Vector3f.UNIT_Z);

    public AutonomousCharacterControl() {
    }

    public AutonomousCharacterControl(Client client, long entityId) {
        super(client, entityId);
    }

    public AutonomousCharacterControl(PhysicsSyncManager server, long entityId) {
        super(server, entityId);
    }

    @Override
    public void doAimAt(Vector3f direction) {
    }

    @Override
    public Vector3f getAimDirection() {
        return aimDirection;
    }

    @Override
    public void doMoveTo(Vector3f location) {
        targetLocation.set(location);
        moving = true;
    }

    @Override
    public void setSpatial(Spatial spatial) {
        this.spatial = spatial;
        if (spatial == null) {
            return;
        }
        NavigationControl navControl = spatial.getControl(NavigationControl.class);
        if (navControl != null) {
            checkRadius = navControl.getEntityRadius();
        }
        Float spatialSpeed = (Float) spatial.getUserData("Speed");
        if (spatialSpeed != null) {
            speed = spatialSpeed * Globals.PHYSICS_FPS;
        }
        characterControl = spatial.getControl(CharacterControl.class);
    }

    @Override
    public boolean isMoving() {
        return moving;
    }

    @Override
    public Vector3f getTargetLocation() {
        return targetLocation;
    }

    @Override
    public Vector3f getCurrentLocation() {
        return characterControl.getPhysicsLocation(vector);
    }

    @Override
    public void update(float tpf) {
        if (!moving || !enabled) {
            return;
        }
        characterControl.getPhysicsLocation(vector);
        vector2.set(targetLocation);
        vector2.subtractLocal(vector);
        float distance = vector2.length();
        if (distance <= checkRadius) {
            moving = false;
            characterControl.setWalkDirection(Vector3f.ZERO);
        } else {
            vector2.y = 0;
            vector2.normalizeLocal();
            characterControl.setViewDirection(vector2);
            vector2.multLocal(speed);
            characterControl.setWalkDirection(vector2);
        }
    }

    public void render(RenderManager rm, ViewPort vp) {
    }
}

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
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.monkeyzone.Globals;
import com.jme3.network.connection.Client;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;

/**
 * Manual character control, implements ManualControl interface and controls
 * a character if available on the Spatial.
 * @author normenhansen
 */
public class ManualCharacterControl extends NetworkedManualControl {

    private Spatial spatial;
    private CharacterControl control;
    private Vector3f walkDirection = new Vector3f(Vector3f.ZERO);
    private Vector3f directionLeft = new Vector3f(Vector3f.UNIT_X);
    private Vector3f directionForward = new Vector3f(Vector3f.UNIT_Z);
    private Quaternion directionQuat = new Quaternion();
    private Quaternion ROTATE_90 = new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y);
    private float rotAmount = 0;
    private float zAmount = 0;
    private float xAmount = 0;
    private float speed = 10f * Globals.PHYSICS_FPS;
//    private boolean backwards = false;
    private Vector3f lookAt = new Vector3f();
    private boolean gotInput = false;

    public ManualCharacterControl() {
    }

    public ManualCharacterControl(Client client, long entityId) {
        super(client, entityId);
    }

    @Override
    public void steerX(float amount) {
        super.steerX(amount);
        rotAmount = amount;
        gotInput = true;
    }

    @Override
    public void steerY(float amount) {
        super.steerY(amount);
    }

    @Override
    public void moveX(float amount) {
        super.moveX(amount);
        xAmount = amount;
        walkDirection.set(directionForward).multLocal(speed * zAmount);
        walkDirection.addLocal(directionLeft.mult(speed * xAmount));
        gotInput = true;
    }

    @Override
    public void moveY(float amount) {
        super.moveY(amount);
    }

    @Override
    public void moveZ(float amount) {
        super.moveZ(amount);
        zAmount = amount;
        walkDirection.set(directionForward).multLocal(speed * zAmount);
        walkDirection.addLocal(directionLeft.mult(speed * xAmount));
        gotInput = true;
    }

    @Override
    public void setSpatial(Spatial spatial) {
        this.spatial = spatial;
        if (spatial == null) {
            return;
        }
        this.control = spatial.getControl(CharacterControl.class);
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

        //when we didnt get any new input we get walkDirection from the control,
        //it might have been updated by network sync
        if (!gotInput) {
            walkDirection.set(control.getWalkDirection());
            if (walkDirection.length() != 0) {
                //try deriving directionForward..
                //TODO: fails sometimes
                directionForward.set(walkDirection);
                directionForward.addLocal(directionLeft.multLocal(speed * -xAmount)).multLocal(Math.copySign(1, zAmount)).normalizeLocal();
                directionLeft.set(directionForward).normalizeLocal();
                ROTATE_90.multLocal(directionLeft);
            }
        }

        if (rotAmount != 0) {
            //rotate all vectors around the rotation amount
            directionQuat.fromAngleAxis((FastMath.PI) * tpf * rotAmount, Vector3f.UNIT_Y);
            directionQuat.multLocal(walkDirection);
            directionQuat.multLocal(directionForward);
            directionQuat.multLocal(directionLeft);
        }

        control.setWalkDirection(walkDirection);
        //look in directionForward
        lookAt.set(spatial.getWorldTranslation()).addLocal(directionForward);
        spatial.lookAt(lookAt, Vector3f.UNIT_Y);
        gotInput = false;
    }

    public void render(RenderManager rm, ViewPort vp) {
    }
}

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
import com.jme3.math.Plane;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.monkeyzone.Globals;
import com.jme3.monkeyzone.messages.ActionMessage;
import com.jme3.network.Client;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;

/**
 * Automomous vehicle control, implements the AutonomousControl interface and
 * controls a vehicle if available on the spatial.
 * @author normenhansen
 */
public class AutonomousVehicleControl extends NetworkedAutonomousControl {

    private float checkRadius = 4;
    private float speed = 800f;
    private Vector3f targetLocation = new Vector3f();
    private Vector3f vector1 = new Vector3f();
    private Vector3f vector2 = new Vector3f();
    private Vector3f vector3 = new Vector3f();
    private Vector3f vector4 = new Vector3f();
    private boolean moving = false;
    private VehicleControl vehicle;
    private Vector3f aimDirection = new Vector3f(Vector3f.UNIT_Z);
    private Plane plane = new Plane();
//    private Plane plane2 = new Plane();
    static final Quaternion ROTATE_RIGHT = new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y);

    public AutonomousVehicleControl() {
    }

    public AutonomousVehicleControl(Client client, long entityId) {
        super(client, entityId);
    }

    @Override
    public void doAimAt(Vector3f direction) {
        aimDirection.set(direction);
    }

    @Override
    public Vector3f getAimDirection() {
        return aimDirection;
    }

    @Override
    public void doMoveTo(Vector3f location) {
        targetLocation.set(location);
        vehicle.getPhysicsLocation(location);
        vector1.set(targetLocation);
        vector1.subtractLocal(location);
        float distance = vector1.length();
        if (distance > checkRadius) {
            moving = true;
        }
    }

    @Override
    public void doPerformAction(int action, boolean activate) {
        if (activate && action == ActionMessage.JUMP_ACTION) {
//            characterControl.jump();
        }
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
            speed = spatialSpeed;
        }
        vehicle = spatial.getControl(VehicleControl.class);
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
    public Vector3f getLocation() {
        return vehicle.getPhysicsLocation(vector2);
    }

    @Override
    public void update(float tpf) {
        if (!moving || !enabled) {
            return;
        }
        vehicle.getPhysicsLocation(vector1);
        vector2.set(targetLocation);
        vector2.subtractLocal(vector1);
        float distance = vector2.length();
        if (distance <= checkRadius) {
            moving = false;
            vehicle.accelerate(0);
            vehicle.brake(10);
        } else {
            vector2.normalizeLocal();
            vehicle.getForwardVector(vector3).normalizeLocal();
            vector4.set(vector3);
            ROTATE_RIGHT.multLocal(vector4);
            plane.setOriginNormal(spatial.getWorldTranslation(), vector4);
            float angle = 1 - vector3.dot(vector2);
            System.out.println("angle:" + angle);

            float anglemult = FastMath.PI / 4.0f;
            float speedmult = 0.3f;

            //left or right
            if (plane.whichSide(targetLocation) == Plane.Side.Negative) {
                anglemult *= -1;
            }

            //backwards
            if (angle > 1) {
                speedmult *= -1;
                anglemult *= -1;
                angle = 1;
            }

            vehicle.steer(angle * anglemult);
            vehicle.accelerate(speed * speedmult);
            vehicle.brake(0);
        }
    }

    public void render(RenderManager rm, ViewPort vp) {
    }
}

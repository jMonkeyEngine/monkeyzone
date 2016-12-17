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

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.PhysicsTickListener;
import com.jme3.bullet.control.VehicleControl;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.network.Client;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;

/**
 * Manual vehicle control, implements ManualControl interface and controls
 * a vehicle if available on the Spatial.
 * @author normenhansen
 */
public class ManualVehicleControl extends NetworkedManualControl implements PhysicsTickListener {

    private Spatial spatial;
    private VehicleControl control;
    private float speed = 800f;
    private float steer = 0;
    private float accelerate = 0;
    private Vector3f tempVec1 = new Vector3f();
    private Vector3f tempVec2 = new Vector3f();
    private Vector3f tempVec3 = new Vector3f();
    private boolean hover = false;
    private boolean added = false;

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
        if (!hover) {
            steer = amount * FastMath.QUARTER_PI * 0.5f;
        } else {
            steer = amount;
        }
    }

    @Override
    public void doMoveY(float amount) {
    }

    @Override
    public void doMoveZ(float amount) {
        accelerate = amount * speed;
    }

    @Override
    public void doPerformAction(int button, boolean pressed) {
    }

    public Vector3f getAimDirection() {
        return control.getForwardVector(tempVec1);
    }

    public Vector3f getLocation() {
        return control.getPhysicsLocation(tempVec2);
    }

    @Override
    public void setSpatial(Spatial spatial) {
        this.spatial = spatial;
        if (spatial == null) {
            if (added) {
                control.getPhysicsSpace().removeTickListener(this);
            }
            return;
        }
        this.control = spatial.getControl(VehicleControl.class);
        if (this.control == null) {
            throw new IllegalStateException("Cannot add ManualCharacterControl to Spatial without CharacterControl");
        }
        Float spatialSpeed = (Float) spatial.getUserData("Speed");
        if (spatialSpeed != null) {
            speed = spatialSpeed;
        }
        Integer hoverControl = (Integer) spatial.getUserData("HoverControl");
        if (hoverControl != null && hoverControl == 1) {
            hover = true;
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
        if (hover) {
            if (!added) {
                control.getPhysicsSpace().addTickListener(this);
                added = true;
            }
            return;
        }
        control.accelerate(accelerate);
        control.steer(steer);
    }

    public void prePhysicsTick(PhysicsSpace space, float f) {
        if (!enabled || !hover) {
            return;
        }
        Vector3f angVel = control.getAngularVelocity();
        float rotationVelocity = angVel.getY();
        Vector3f dir = control.getForwardVector(tempVec2).multLocal(1, 0, 1).normalizeLocal();
        control.getLinearVelocity(tempVec3);
        Vector3f linearVelocity = tempVec3.multLocal(1, 0, 1);

        if (steer != 0) {
            if (rotationVelocity < 1 && rotationVelocity > -1) {
                control.applyTorque(tempVec1.set(0, steer * control.getMass() * 20, 0));
            }
        } else {
            // counter the steering value!
            if (rotationVelocity > 0.2f) {
                control.applyTorque(tempVec1.set(0, -control.getMass() * 20, 0));
            } else if (rotationVelocity < -0.2f) {
                control.applyTorque(tempVec1.set(0, control.getMass() * 20, 0));
            }
        }
        if (accelerate > 0) {
            // counter force that will adjust velocity
            // if we are not going where we want to go.
            // this will prevent "drifting" and thus improve control
            // of the vehicle
            float d = dir.dot(linearVelocity.normalize());
            Vector3f counter = dir.project(linearVelocity).normalizeLocal().negateLocal().multLocal(1 - d);
            control.applyForce(counter.multLocal(control.getMass() * 10), Vector3f.ZERO);

            if (linearVelocity.length() < 30) {
                control.applyForce(dir.multLocal(accelerate), Vector3f.ZERO);
            }
        } else {
            // counter the acceleration value
            if (linearVelocity.length() > FastMath.ZERO_TOLERANCE) {
                linearVelocity.normalizeLocal().negateLocal();
                control.applyForce(linearVelocity.mult(control.getMass() * 10), Vector3f.ZERO);
            }
        }
    }

    public void physicsTick(PhysicsSpace space, float f) {
    }

    public void render(RenderManager rm, ViewPort vp) {
    }
}

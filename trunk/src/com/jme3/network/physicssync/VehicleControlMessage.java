/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.network.physicssync;

import com.jme3.bullet.objects.PhysicsVehicle;
import com.jme3.network.serializing.Serializable;

/**
 *
 * @author normenhansen
 */
@Serializable()
public class VehicleControlMessage extends AbstractPhysicsSyncMessage {

    public float steer;
    public float accelerate;
    public float brake;

    public VehicleControlMessage() {
    }

    public VehicleControlMessage(float steer, float accelerate, float brake) {
        this.steer = steer;
        this.accelerate = accelerate;
    }

    public void applyData(Object vehicle) {
        ((PhysicsVehicle) vehicle).steer(steer);
        ((PhysicsVehicle) vehicle).accelerate(accelerate);
        ((PhysicsVehicle) vehicle).brake(brake);
    }
}

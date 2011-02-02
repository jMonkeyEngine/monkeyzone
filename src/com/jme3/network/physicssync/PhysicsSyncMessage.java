/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.network.physicssync;

import com.jme3.network.message.Message;
import com.jme3.network.serializing.Serializable;

/**
 *
 * @author normenhansen
 */
@Serializable()
public abstract class PhysicsSyncMessage extends Message {

    public long syncId = -1;
    public double time;
    //TODO: dont dync delayTime somehow? -> not needed
    public double delayTime = 0;

    public PhysicsSyncMessage() {
        setReliable(false);
    }

    public PhysicsSyncMessage(long id) {
        this.syncId = id;
        setReliable(false);
    }

    public abstract void applyData(Object object);
}

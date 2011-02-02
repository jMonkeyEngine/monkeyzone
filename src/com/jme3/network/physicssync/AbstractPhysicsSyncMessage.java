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
public abstract class AbstractPhysicsSyncMessage extends Message {

    public long id = -1;
    public double time;
    //TODO: dont dync delayTime somehow? -> not needed
    public double delayTime = 0;

    public AbstractPhysicsSyncMessage() {
    }

    public AbstractPhysicsSyncMessage(long id) {
        this.id = id;
    }

    public abstract void applyData(Object object);
}

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
package com.jme3.monkeyzone.messages;

import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.monkeyzone.controls.NetworkedManualControl;
import com.jme3.network.physicssync.PhysicsSyncMessage;
import com.jme3.network.serializing.Serializable;
import com.jme3.scene.Spatial;

/**
 * Manual (human) control message, used bidirectional
 * @author normenhansen
 */
@Serializable()
public class ManualControlMessage extends PhysicsSyncMessage {

    public float aimX;
    public float aimY;
    public float moveX;
    public float moveY;
    public float moveZ;

    public ManualControlMessage() {
    }

    public ManualControlMessage(ManualControlMessage msg) {
        this.syncId = msg.syncId;
        this.aimX = msg.aimX;
        this.aimY = msg.aimY;
        this.moveX = msg.moveX;
        this.moveY = msg.moveY;
        this.moveZ = msg.moveZ;
    }

    public ManualControlMessage(long id, float aimX, float aimY, float moveX, float moveY, float moveZ) {
        this.syncId = id;
        this.aimX = aimX;
        this.aimY = aimY;
        this.moveX = moveX;
        this.moveY = moveY;
        this.moveZ = moveZ;
    }

    @Override
    public void applyData(Object object) {
        NetworkedManualControl netControl = ((Spatial) ((PhysicsCollisionObject) object).getUserObject()).getControl(NetworkedManualControl.class);
        if (netControl != null) {
            if (netControl.getSyncManager().getServer() != null) {
                netControl.getSyncManager().broadcast(this);
            }
            netControl.doMoveX(moveX);
            netControl.doMoveY(moveY);
            netControl.doMoveZ(moveZ);
            netControl.doSteerX(aimX);
            netControl.doSteerY(aimY);
        }
    }
}

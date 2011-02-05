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
import com.jme3.network.physicssync.PhysicsSyncMessage;
import com.jme3.network.serializing.Serializable;
import com.jme3.scene.Spatial;

/**
 * sets userdata in a client-side entity
 * @author normenhansen
 */
@Serializable()
public class ServerEntityDataMessage extends PhysicsSyncMessage {

    public String name;
    public byte type;
    public int intData;
    public float floatData;
    public long longData;
    public boolean booleanData;
    public String stringData;

    public ServerEntityDataMessage() {
    }

    public ServerEntityDataMessage(long id, String name, Object value) {
        this.name = name;
        syncId = id;
        type = getObjectType(value);
        switch (type) {
            case 0:
                intData = (Integer) value;
                break;
            case 1:
                floatData = (Float) value;
                break;
            case 2:
                booleanData = (Boolean) value;
                break;
            case 3:
                stringData = (String) value;
                break;
            case 4:
                longData = (Long) value;
                break;
            default:
                throw new UnsupportedOperationException("Cannot apply wrong userdata type.");
        }
    }

    @Override
    public void applyData(Object object) {
        Spatial spat = ((Spatial) ((PhysicsCollisionObject) object).getUserObject());
        switch (type) {
            case 0:
                spat.setUserData(name, intData);
                break;
            case 1:
                spat.setUserData(name, floatData);
                break;
            case 2:
                spat.setUserData(name, booleanData);
                break;
            case 3:
                spat.setUserData(name, stringData);
                break;
            case 4:
                spat.setUserData(name, longData);
                break;
            default:
                throw new UnsupportedOperationException("Cannot apply wrong userdata type.");
        }
    }

    private static byte getObjectType(Object type) {
        if (type instanceof Integer) {
            return 0;
        } else if (type instanceof Float) {
            return 1;
        } else if (type instanceof Boolean) {
            return 2;
        } else if (type instanceof String) {
            return 3;
        } else if (type instanceof Long) {
            return 4;
        } else {
            throw new IllegalArgumentException("Unsupported type: " + type.getClass().getName());
        }
    }
}

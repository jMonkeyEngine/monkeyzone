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
package com.jme3.monkeyzone;

import com.jme3.monkeyzone.messages.ClientActionMessage;
import com.jme3.monkeyzone.messages.AutoControlMessage;
import com.jme3.monkeyzone.messages.ChatMessage;
import com.jme3.monkeyzone.messages.ClientJoinMessage;
import com.jme3.monkeyzone.messages.HandshakeMessage;
import com.jme3.monkeyzone.messages.ManualControlMessage;
import com.jme3.monkeyzone.messages.ServerAddEntityMessage;
import com.jme3.monkeyzone.messages.ServerAddPlayerMessage;
import com.jme3.network.physicssync.SyncCharacterMessage;
import com.jme3.monkeyzone.messages.ServerEffectMessage;
import com.jme3.monkeyzone.messages.ServerEnterEntityMessage;
import com.jme3.monkeyzone.messages.ServerEntityDataMessage;
import com.jme3.monkeyzone.messages.ServerJoinMessage;
import com.jme3.network.physicssync.SyncRigidBodyMessage;
import com.jme3.monkeyzone.messages.ServerPlayerDataMessage;
import com.jme3.monkeyzone.messages.ServerRemoveEntityMessage;
import com.jme3.monkeyzone.messages.ServerRemovePlayerMessage;
import com.jme3.monkeyzone.messages.StartGameMessage;
import com.jme3.network.serializing.Serializer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author normenhansen
 */
public class Util {

    public static void setLogLevels(boolean debug) {
        if (debug) {
            Logger.getLogger("de.lessvoid.nifty").setLevel(Level.SEVERE);
            Logger.getLogger("de.lessvoid.nifty.effects.EffectProcessor").setLevel(Level.SEVERE);
            Logger.getLogger("org.lwjgl").setLevel(Level.WARNING);
            Logger.getLogger("com.jme3").setLevel(Level.FINEST);
            Logger.getLogger("com.jme3.monkeyzone").setLevel(Level.FINEST);
        } else {
            Logger.getLogger("de.lessvoid").setLevel(Level.WARNING);
            Logger.getLogger("de.lessvoid.nifty.effects.EffectProcessor").setLevel(Level.WARNING);
            Logger.getLogger("org.lwjgl").setLevel(Level.WARNING);
            Logger.getLogger("com.jme3").setLevel(Level.WARNING);
            Logger.getLogger("com.jme3.monkeyzone").setLevel(Level.WARNING);
        }
    }

    public static void registerSerializers() {
        Serializer.registerClass(ClientActionMessage.class);
        Serializer.registerClass(AutoControlMessage.class);
        Serializer.registerClass(ChatMessage.class);
        Serializer.registerClass(ClientJoinMessage.class);
        Serializer.registerClass(HandshakeMessage.class);
        Serializer.registerClass(ManualControlMessage.class);
        Serializer.registerClass(ServerAddEntityMessage.class);
        Serializer.registerClass(ServerAddPlayerMessage.class);
        Serializer.registerClass(SyncCharacterMessage.class);
        Serializer.registerClass(ServerEffectMessage.class);
        Serializer.registerClass(ServerEnterEntityMessage.class);
        Serializer.registerClass(ServerEntityDataMessage.class);
        Serializer.registerClass(ServerJoinMessage.class);
        Serializer.registerClass(SyncRigidBodyMessage.class);
        Serializer.registerClass(ServerPlayerDataMessage.class);
        Serializer.registerClass(ServerRemoveEntityMessage.class);
        Serializer.registerClass(ServerRemovePlayerMessage.class);
        Serializer.registerClass(StartGameMessage.class);
    }

}

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

import com.jme3.network.message.Message;
import com.jme3.network.serializing.Serializable;

/**
 * perform action for player (human and AI), used bidirectional
 * @author normenhansen
 */
@Serializable()
public class ClientActionMessage extends Message {

    public final static int NULL_ACTION = 0;
    public final static int SPACE_ACTION = 1;
    public final static int ENTER_ACTION = 2;
//    public int NULL_ACTION = 3;
//    public int NULL_ACTION = 4;
//    public int NULL_ACTION = 5;
//    public int NULL_ACTION = 6;
//    public int NULL_ACTION = 7;
//    public int NULL_ACTION = 8;
//    public int NULL_ACTION = 9;
//    public int NULL_ACTION = 10;
    
    public long id;
    public int action;
    public boolean pressed;

    public ClientActionMessage() {
    }

    public ClientActionMessage(long id, int action, boolean pressed) {
        this.id = id;
        this.action = action;
        this.pressed = pressed;
    }
}

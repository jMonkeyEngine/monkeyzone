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

/**
 * contains version info and various global variables
 * @author normenhansen
 */
public class Globals {

    public static final String VERSION = "MonkeyZone v0.1";
//    public static final String DEFAULT_SERVER = "192.168.1.24";
    public static final String DEFAULT_SERVER = "127.0.0.1";
//    public static final String DEFAULT_SERVER = "bitwaves.de";
    public static final int PROTOCOL_VERSION = 1;
    public static final int CLIENT_VERSION = 1;
    public static final int SERVER_VERSION = 1;

    public static final float NETWORK_SYNC_FREQUENCY = 0.25f;
    public static final float NETWORK_MAX_PHYSICS_DELAY = 0.25f;
    public static final int SCENE_FPS = 60;
    public static final float PHYSICS_FPS = 1f / 30f;
    public static final boolean PHYSICS_DETERMINISTIC = false;
    public static final boolean PHYSICS_THREADED = true; //only applies for client, server doesnt render anyway
    public static final int DEFAULT_PORT_TCP = 6143;
    public static final int DEFAULT_PORT_UDP = 6143;
}

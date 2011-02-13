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

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.renderer.RenderManager;
import com.jme3.system.AppSettings;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The server Main class, basically just starts the application with a
 * null renderer and then starts the managers.
 * @author normenhansen
 */
public class ServerMain extends SimpleApplication {

    private static com.jme3.network.connection.Server server;
    private static ServerMain app;

    public static void main(String[] args) {
        AppSettings settings = new AppSettings(true);
        settings.setFrameRate(Globals.SCENE_FPS);
        settings.setRenderer(AppSettings.NULL);
        //FIXME: strange way of setting null audio renderer..
        settings.setAudioRenderer(null);
        for (int i = 0; i < args.length; i++) {
            String string = args[i];
            if ("-display".equals(string)) {
                settings.setRenderer(AppSettings.LWJGL_OPENGL2);
            }
        }
        Util.registerSerializers();
        Util.setLogLevels(true);
        app = new ServerMain();
        app.setShowSettings(false);
        app.setPauseOnLostFocus(false);
        app.setSettings(settings);
        app.start();
    }
    private WorldManager worldManager;
    private ServerGameManager gameManager;
    private ServerNetListener listenerManager;
    private BulletAppState bulletState;

    @Override
    public void simpleInitApp() {
        try {
            server = new com.jme3.network.connection.Server(Globals.DEFAULT_PORT_TCP, Globals.DEFAULT_PORT_UDP);
            server.start();
        } catch (IOException ex) {
            Logger.getLogger(ServerMain.class.getName()).log(Level.SEVERE, "Cannot start server: {0}", ex);
            return;
        }
        bulletState = new BulletAppState();
        getStateManager().attach(bulletState);
        bulletState.getPhysicsSpace().setDeterministic(Globals.PHYSICS_DETERMINISTIC);
        bulletState.getPhysicsSpace().setAccuracy(Globals.PHYSICS_FPS);
        worldManager = new WorldManager(this, rootNode, bulletState.getPhysicsSpace(), server);
        gameManager = new ServerGameManager(worldManager);
        listenerManager = new ServerNetListener(this, server, worldManager, gameManager);
    }

    @Override
    public void simpleUpdate(float tpf) {
        worldManager.update(tpf);
    }

    @Override
    public void simpleRender(RenderManager rm) {
    }

    @Override
    public void destroy() {
        super.destroy();
        try {
            server.stop();
        } catch (IOException ex) {
            Logger.getLogger(ClientMain.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

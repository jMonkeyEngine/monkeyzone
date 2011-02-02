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

import com.jme3.monkeyzone.messages.ChatMessage;
import com.jme3.monkeyzone.messages.ClientActionMessage;
import com.jme3.monkeyzone.messages.ClientJoinMessage;
import com.jme3.monkeyzone.messages.HandshakeMessage;
import com.jme3.monkeyzone.messages.ServerJoinMessage;
import com.jme3.monkeyzone.messages.StartGameMessage;
import com.jme3.network.connection.Client;
import com.jme3.network.events.ConnectionListener;
import com.jme3.network.events.MessageListener;
import com.jme3.network.message.Message;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles the network message transfer for the client in a threadsafe way
 * TODO: time stamping/late applying
 * @author normenhansen
 */
public class ClientNetListener implements MessageListener, ConnectionListener {

    private ClientMain app;
    private Client client;
    private String name = "";
    private String pass = "";
    private WorldManager worldManager;

    public ClientNetListener(ClientMain app, Client client, WorldManager worldManager, ClientEffectsManager effectsManager) {
        this.app = app;
        this.client = client;
        this.worldManager = worldManager;
        client.addConnectionListener(this);
        client.addMessageListener(this, HandshakeMessage.class, ServerJoinMessage.class, StartGameMessage.class, ChatMessage.class);
    }

    public void clientConnected(Client clienst) {
        setStatusText("Requesting login..");
        //FIXME: (SpiderMonkey) cannot call client.send() from connectionListener, doing via opengl queue..
        app.enqueue(new Callable<Void>() {

            public Void call() throws Exception {
                HandshakeMessage msg = new HandshakeMessage(Globals.PROTOCOL_VERSION, Globals.CLIENT_VERSION, -1);
                try {
                    client.send(msg);
                    Logger.getLogger(ClientNetListener.class.getName()).log(Level.INFO, "Sent handshake message");
                } catch (IOException ex) {
                    setStatusText("Error sending handshake!");
                    Logger.getLogger(ClientNetListener.class.getName()).log(Level.SEVERE, "While HandShake: {0}", ex);
                }
                return null;
            }
        });
    }

    public void clientDisconnected(Client clienst) {
        setStatusText("Server connection failed!");
    }

    public void messageReceived(Message message) {
        if (message instanceof HandshakeMessage) {
            HandshakeMessage msg = (HandshakeMessage) message;
            Logger.getLogger(ClientNetListener.class.getName()).log(Level.INFO, "Got handshake message back");
            if (msg.protocol_version != Globals.PROTOCOL_VERSION) {
                setStatusText("Protocol mismatch - update client!");
                Logger.getLogger(ClientNetListener.class.getName()).log(Level.INFO, "Client protocol mismatch, disconnecting");
            }
            try {
                client.send(new ClientJoinMessage(name, pass));
            } catch (IOException ex) {
                setStatusText("Error sending join!");
                Logger.getLogger(ClientNetListener.class.getName()).log(Level.SEVERE, "While Join: {0}", ex);
            }
        } else if (message instanceof ServerJoinMessage) {
            final ServerJoinMessage msg = (ServerJoinMessage) message;
            if (!msg.rejected) {
                Logger.getLogger(ClientNetListener.class.getName()).log(Level.INFO, "Got login message back, we're in");
                setStatusText("Connected!");
                app.enqueue(new Callable<Void>() {

                    public Void call() throws Exception {
                        worldManager.setMyPlayerId(msg.id);
                        worldManager.setMyGroupId(msg.group_id);
                        app.lobby();
                        return null;
                    }
                });
            } else {
                Logger.getLogger(ClientNetListener.class.getName()).log(Level.INFO, "Server ditched us! Cant login.");
                setStatusText("Server rejected login!");
            }
        } else if (message instanceof StartGameMessage) {
            final StartGameMessage msg = (StartGameMessage) message;
            //loadlevel is threaded and cares for OpenGL thread itself
            app.loadLevel(msg.levelName, msg.modelNames);
        } else if (message instanceof ChatMessage) {
            final ChatMessage msg = (ChatMessage) message;
            app.addChat(msg.name + ": " + msg.text);
        }
    }

    public void messageSent(Message message) {
    }

    public void objectReceived(Object object) {
    }

    public void objectSent(Object object) {
    }

    private void setStatusText(String text) {
        app.setStatusText(text);
    }

    public String getName() {
        return name;
    }

    /**
     * sets the login name
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    public String getPass() {
        return pass;
    }

    /**
     * sets the login password
     * @param pass
     */
    public void setPass(String pass) {
        this.pass = pass;
    }
}

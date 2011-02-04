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
import com.jme3.monkeyzone.messages.ServerAddPlayerMessage;
import com.jme3.monkeyzone.messages.ServerJoinMessage;
import com.jme3.monkeyzone.messages.StartGameMessage;
import com.jme3.network.connection.Client;
import com.jme3.network.connection.Server;
import com.jme3.network.events.ConnectionListener;
import com.jme3.network.events.MessageListener;
import com.jme3.network.message.Message;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles the network message transfer for the server in a threadsafe way
 * @author normenhansen
 */
public class ServerNetListener implements MessageListener, ConnectionListener {

    ServerMain app;
    com.jme3.network.connection.Server server;
    WorldManager worldManager;
    ServerGameManager gameManager;

    public ServerNetListener(ServerMain app, Server server, WorldManager worldManager, ServerGameManager gameManager) {
        this.server = server;
        this.worldManager = worldManager;
        this.app = app;
        this.gameManager = gameManager;
        server.addConnectionListener(this);
        server.addMessageListener(this, HandshakeMessage.class, ClientJoinMessage.class, ChatMessage.class, StartGameMessage.class, ClientActionMessage.class);
    }

    public void clientConnected(Client client) {
        int clientId = client.getClientID();
        if (!ServerClientData.exsists(clientId)) {
            ServerClientData.add(clientId);
        } else {
            Logger.getLogger(ServerNetListener.class.getName()).log(Level.SEVERE, "Client ID exists!");
            return;
        }
    }

    public void clientDisconnected(Client client) {
        final int clientId = client.getClientID();
        final long playerId = ServerClientData.getPlayerId(clientId);
        ServerClientData.remove(clientId);
        app.enqueue(new Callable<Void>() {

            public Void call() throws Exception {
                String name = PlayerData.getStringData(playerId, "name");
                worldManager.removePlayer(playerId);
                try {
                    server.broadcast(new ChatMessage("Server", name + " left the game"));
                    Logger.getLogger(ServerNetListener.class.getName()).log(Level.INFO, "Broadcast player left message");
                } catch (IOException ex) {
                    Logger.getLogger(ServerNetListener.class.getName()).log(Level.SEVERE, "{0}", ex);
                }
                if (PlayerData.getPlayers().isEmpty()) {
                    gameManager.stopGame();
                }
                return null;
            }
        });
    }

    public void messageReceived(Message message) {
        if (message instanceof HandshakeMessage) {
            HandshakeMessage msg = (HandshakeMessage) message;
            Logger.getLogger(ServerNetListener.class.getName()).log(Level.INFO, "Got handshake message");
            if (msg.protocol_version != Globals.PROTOCOL_VERSION) {
                try {
                    msg.getClient().kick("Connection Protocol Mismatch - Update Client");
                    Logger.getLogger(ServerNetListener.class.getName()).log(Level.INFO, "Client protocol mismatch, disconnecting");
                    return;
                } catch (IOException ex) {
                    Logger.getLogger(ServerNetListener.class.getName()).log(Level.SEVERE, "Error kickig client with bad protocol version {0}", ex);
                }
            }
            msg.server_version = Globals.SERVER_VERSION;
            try {
                msg.getClient().send(msg);
                Logger.getLogger(ServerNetListener.class.getName()).log(Level.INFO, "Sent back handshake message");
            } catch (IOException ex) {
                Logger.getLogger(ServerNetListener.class.getName()).log(Level.SEVERE, "Error sending back handshake message {0}", ex);
            }
        } else if (message instanceof ClientJoinMessage) {
            final ClientJoinMessage msg = (ClientJoinMessage) message;
            Logger.getLogger(ServerNetListener.class.getName()).log(Level.INFO, "Got client join message");
            final int clientId = msg.getClient().getClientID();
            //TODO: login user/pass check
            if (!ServerClientData.exsists(clientId)) {
                Logger.getLogger(ServerNetListener.class.getName()).log(Level.WARNING, "Receiving join message from unknown client");
                return;
            }
            //TODO: creates new player, reuse old on drop possible?
            final long newPlayerId = PlayerData.getNew(msg.name);
            Logger.getLogger(ServerNetListener.class.getName()).log(Level.INFO, "Created new played ID {0}", newPlayerId);
            ServerClientData.setConnected(clientId, true);
            ServerClientData.setPlayerId(clientId, newPlayerId);
            ServerJoinMessage serverJoinMessage = new ServerJoinMessage(newPlayerId, clientId, msg.name, false);
            try {
                server.broadcast(new ChatMessage("Server", msg.name + " joined the game"));
                message.getClient().send(serverJoinMessage);
                Logger.getLogger(ServerNetListener.class.getName()).log(Level.INFO, "Login succesful - sent back join message");
            } catch (IOException ex) {
                Logger.getLogger(ServerNetListener.class.getName()).log(Level.SEVERE, "{0}", ex);
            }
            //add the player
            app.enqueue(new Callable<Void>() {

                public Void call() throws Exception {
                    //TODO: client id as group id
                    worldManager.addPlayer(newPlayerId, clientId, msg.name, -1);
                    PlayerData.setData(newPlayerId, "client_id", clientId);
                    for (Iterator<PlayerData> it = PlayerData.getPlayers().iterator(); it.hasNext();) {
                        PlayerData playerData = it.next();
                        if (playerData.getId() != newPlayerId) {
                            worldManager.getSyncManager().send(msg.getClient(), new ServerAddPlayerMessage(playerData.getId(), playerData.getStringData("name"), playerData.getIntData("group_id"), playerData.getAiControl()));
                            Logger.getLogger(ServerNetListener.class.getName()).log(Level.INFO, "Send player {0} to client {1}", new Object[]{playerData.getId(), newPlayerId});
                        }
                    }

                    return null;
                }
            });
        } else if (message instanceof ChatMessage) {
            ChatMessage msg = (ChatMessage) message;
            int clientId = message.getClient().getClientID();
            if (!checkClient(message)) {
                return;
            }
            try {
                msg.name = PlayerData.getStringData(ServerClientData.getPlayerId(clientId), "name");
                server.broadcast(msg);
            } catch (Exception ex) {
                Logger.getLogger(ServerNetListener.class.getName()).log(Level.SEVERE, "Error broadcasting chat: {0}", ex);
            }
        } else if (message instanceof StartGameMessage) {
            StartGameMessage msg = (StartGameMessage) message;
            final String levelName = msg.levelName;
            app.enqueue(new Callable<Void>() {

                public Void call() throws Exception {
                    gameManager.startGame(levelName);
                    return null;
                }
            });
        } else if (message instanceof ClientActionMessage) {
            final ClientActionMessage msg = (ClientActionMessage) message;
            app.enqueue(new Callable<Void>() {

                public Void call() throws Exception {
                    gameManager.performAction(msg.id, msg.action, msg.pressed);
                    return null;
                }
            });
        }
    }

    public void messageSent(Message message) {
    }

    public void objectReceived(Object object) {
    }

    public void objectSent(Object object) {
    }

    /**
     * checks if the message client is valid, meaning logged in
     * @param message
     * @return
     */
    private boolean checkClient(Message message) {
        int clientId = message.getClient().getClientID();
        if (ServerClientData.exsists(clientId) && ServerClientData.isConnected(clientId)) {
            return true;
        } else {
            Logger.getLogger(ServerNetListener.class.getName()).log(Level.WARNING, "Invalid client, reject data from client {0}", message.getClient());
            return false;
        }
    }
}

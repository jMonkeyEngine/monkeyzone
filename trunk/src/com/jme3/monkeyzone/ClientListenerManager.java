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

import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.control.VehicleControl;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.monkeyzone.messages.AutoControlMessage;
import com.jme3.monkeyzone.messages.ChatMessage;
import com.jme3.monkeyzone.messages.ClientJoinMessage;
import com.jme3.monkeyzone.messages.HandshakeMessage;
import com.jme3.monkeyzone.messages.ManualControlMessage;
import com.jme3.monkeyzone.messages.ServerAddEntityMessage;
import com.jme3.monkeyzone.messages.ServerAddPlayerMessage;
import com.jme3.monkeyzone.messages.ServerEffectMessage;
import com.jme3.monkeyzone.messages.ServerSyncCharacterMessage;
import com.jme3.monkeyzone.messages.ServerEnterEntityMessage;
import com.jme3.monkeyzone.messages.ServerEntityDataMessage;
import com.jme3.monkeyzone.messages.ServerJoinMessage;
import com.jme3.monkeyzone.messages.ServerSyncRigidBodyMessage;
import com.jme3.monkeyzone.messages.ServerPlayerDataMessage;
import com.jme3.monkeyzone.messages.ServerRemoveEntityMessage;
import com.jme3.monkeyzone.messages.ServerRemovePlayerMessage;
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
 * @author normenhansen
 */
public class ClientListenerManager {

    private ClientMain app;
    private Client client;
    private String name = "";
    private String pass = "";
    private WorldManager worldManager;
    private ClientEffectsManager effectsManager;

    public ClientListenerManager(ClientMain app, Client client, WorldManager worldManager, ClientEffectsManager effectsManager) {
        this.app = app;
        this.client = client;
        this.worldManager = worldManager;
        this.effectsManager = effectsManager;
        createLoginListeners();
        createWorldListeners();
        createControlListeners();
        createSyncListeners();
        createEffectListeners();
    }

    /**
     * creates the listeners for login
     */
    private void createLoginListeners() {
        client.addConnectionListener(new ConnectionListener() {

            public void clientConnected(Client clienst) {
                setStatusText("Requesting login..");
                //FIXME: (SpiderMonkey) cannot call client.send() from connectionListener, doing via opengl queue..
                app.enqueue(new Callable<Void>() {

                    public Void call() throws Exception {
                        HandshakeMessage msg = new HandshakeMessage(Globals.PROTOCOL_VERSION, Globals.CLIENT_VERSION, -1);
                        try {
                            client.send(msg);
                            Logger.getLogger(ClientListenerManager.class.getName()).log(Level.INFO, "Sent handshake message");
                        } catch (IOException ex) {
                            setStatusText("Error sending handshake!");
                            Logger.getLogger(ClientListenerManager.class.getName()).log(Level.SEVERE, "While HandShake: {0}", ex);
                        }
                        return null;
                    }
                });
            }

            public void clientDisconnected(Client clienst) {
                setStatusText("Server connection failed!");
            }
        });
        client.addMessageListener(new MessageListener() {

            public void messageReceived(Message message) {
                if (message instanceof HandshakeMessage) {
                    HandshakeMessage msg = (HandshakeMessage) message;
                    Logger.getLogger(ClientListenerManager.class.getName()).log(Level.INFO, "Got handshake message back");
                    if (msg.protocol_version != Globals.PROTOCOL_VERSION) {
                        setStatusText("Protocol mismatch - update client!");
                        Logger.getLogger(ClientListenerManager.class.getName()).log(Level.INFO, "Client protocol mismatch, disconnecting");
                    }
                    try {
                        client.send(new ClientJoinMessage(name, pass));
                    } catch (IOException ex) {
                        setStatusText("Error sending join!");
                        Logger.getLogger(ClientListenerManager.class.getName()).log(Level.SEVERE, "While Join: {0}", ex);
                    }
                } else if (message instanceof ServerJoinMessage) {
                    final ServerJoinMessage msg = (ServerJoinMessage) message;
                    if (!msg.rejected) {
                        Logger.getLogger(ClientListenerManager.class.getName()).log(Level.INFO, "Got login message back, we're in");
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
                        Logger.getLogger(ClientListenerManager.class.getName()).log(Level.INFO, "Server ditched us! Cant login.");
                        setStatusText("Server rejected login!");
                    }
                }
            }

            public void messageSent(Message message) {
            }

            public void objectReceived(Object object) {
            }

            public void objectSent(Object object) {
            }
        }, HandshakeMessage.class, ServerJoinMessage.class);
        /*
         * chat data
         */
        client.addMessageListener(new MessageListener() {

            public void messageReceived(Message message) {
                ChatMessage msg = (ChatMessage) message;
                app.addChat(msg.name + ": " + msg.text);
            }

            public void messageSent(Message message) {
            }

            public void objectReceived(Object object) {
            }

            public void objectSent(Object object) {
            }
        }, ChatMessage.class);
    }

    /**
     * creates the listeners for world load and add/remove/data messages
     */
    private void createWorldListeners() {
        client.addMessageListener(new MessageListener() {

            public void messageReceived(Message message) {
                /*
                 * start game / load level
                 */
                if (message instanceof StartGameMessage) {
                    final StartGameMessage msg = (StartGameMessage) message;
                    //loadlevel is threaded and cares for OpenGL thread itself
                    app.loadLevel(msg.levelName, msg.modelNames);
                } /*
                 * add new player (human or AI)
                 */ else if (message instanceof ServerAddPlayerMessage) {
                    final ServerAddPlayerMessage msg = (ServerAddPlayerMessage) message;
                    Logger.getLogger(ClientListenerManager.class.getName()).log(Level.INFO, "Got add player message");
                    app.enqueue(new Callable<Void>() {

                        public Void call() throws Exception {
                            worldManager.addPlayer(msg.id, msg.group_id, msg.name, msg.ai_id);
                            return null;
                        }
                    });
                    app.updatePlayerData();
                } /*
                 * add entity
                 */ else if (message instanceof ServerAddEntityMessage) {
                    final ServerAddEntityMessage msg = (ServerAddEntityMessage) message;
                    Logger.getLogger(ClientListenerManager.class.getName()).log(Level.INFO, "Got add entity message");
                    app.enqueue(new Callable<Void>() {

                        public Void call() throws Exception {
                            worldManager.addEntity(msg.id, msg.modelIdentifier, msg.location, msg.rotation);
                            return null;
                        }
                    });
                } /*
                 * remove entity
                 */ else if (message instanceof ServerRemoveEntityMessage) {
                    final ServerRemoveEntityMessage msg = (ServerRemoveEntityMessage) message;
                    Logger.getLogger(ClientListenerManager.class.getName()).log(Level.INFO, "Got remove entity message");
                    app.enqueue(new Callable<Void>() {

                        public Void call() throws Exception {
                            worldManager.removeEntity(msg.id);
                            return null;
                        }
                    });
                } /*
                 * remove player
                 */ else if (message instanceof ServerRemovePlayerMessage) {
                    final ServerRemovePlayerMessage msg = (ServerRemovePlayerMessage) message;
                    Logger.getLogger(ClientListenerManager.class.getName()).log(Level.INFO, "Got remove player message");
                    app.enqueue(new Callable<Void>() {

                        public Void call() throws Exception {
                            worldManager.removePlayer(msg.id);
                            return null;
                        }
                    });
                    app.updatePlayerData();
                } /*
                 * set player data
                 */ else if (message instanceof ServerPlayerDataMessage) {
                    Logger.getLogger(ClientListenerManager.class.getName()).log(Level.INFO, "Got player data message");
                    final ServerPlayerDataMessage msg = (ServerPlayerDataMessage) message;
                    app.enqueue(new Callable<Void>() {

                        public Void call() throws Exception {
                            //TODO: player data
                            return null;
                        }
                    });
                } /*
                 * set entity data
                 */ else if (message instanceof ServerEntityDataMessage) {
                    Logger.getLogger(ClientListenerManager.class.getName()).log(Level.INFO, "Got entity data message");
                    final ServerEntityDataMessage msg = (ServerEntityDataMessage) message;
                    app.enqueue(new Callable<Void>() {

                        public Void call() throws Exception {
                            //TODO: entity data
                            return null;
                        }
                    });
                } /*
                 * enter entity
                 */ else if (message instanceof ServerEnterEntityMessage) {
                    Logger.getLogger(ClientListenerManager.class.getName()).log(Level.INFO, "Got enter entity message");
                    ServerEnterEntityMessage msg = (ServerEnterEntityMessage) message;
                    final long playerId = msg.player_id;
                    final long entityId = msg.entity_id;
                    app.enqueue(new Callable<Void>() {

                        public Void call() throws Exception {
                            worldManager.enterEntity(playerId, entityId);
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
        }, StartGameMessage.class, ServerAddPlayerMessage.class, ServerAddEntityMessage.class, ServerRemoveEntityMessage.class, ServerRemovePlayerMessage.class, ServerPlayerDataMessage.class, ServerEntityDataMessage.class, ServerEnterEntityMessage.class);
    }

    /**
     * listens to entity control messages
     */
    private void createControlListeners() {
        client.addMessageListener(new MessageListener() {

            public void messageReceived(Message message) {
                /*
                 * manual keypress/control
                 */
                if (message instanceof ManualControlMessage) {
                    final ManualControlMessage msg = (ManualControlMessage) message;
                    app.enqueue(new Callable<Void>() {

                        public Void call() throws Exception {
                            worldManager.applyManualControl(msg);
                            return null;
                        }
                    });
                }/*
                 * autonomous control message
                 */ else if (message instanceof AutoControlMessage) {
                    final AutoControlMessage msg = (AutoControlMessage) message;
                    app.enqueue(new Callable<Void>() {

                        public Void call() throws Exception {
                            worldManager.applyAutoControl(msg);
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
        }, ManualControlMessage.class, AutoControlMessage.class);
    }

    /**
     * listens sync messages
     */
    private void createSyncListeners() {
        client.addMessageListener(new MessageListener() {

            public void messageReceived(Message message) {
                /*
                 * character sync
                 */
                if (message instanceof ServerSyncCharacterMessage) {
                    final ServerSyncCharacterMessage msg = (ServerSyncCharacterMessage) message;
                    app.enqueue(new Callable<Void>() {

                        public Void call() throws Exception {
                            CharacterControl control = worldManager.getEntity(msg.id).getControl(CharacterControl.class);
                            msg.applyData(control);
                            return null;
                        }
                    });
                }/*
                 * physics sync
                 */ else if (message instanceof ServerSyncRigidBodyMessage) {
                    final ServerSyncRigidBodyMessage msg = (ServerSyncRigidBodyMessage) message;
                    app.enqueue(new Callable<Void>() {

                        public Void call() throws Exception {
                            PhysicsRigidBody control = worldManager.getEntity(msg.id).getControl(RigidBodyControl.class);
                            if (control == null) {
                                control = worldManager.getEntity(msg.id).getControl(VehicleControl.class);
                            }
                            msg.applyData(control);
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
        }, ServerSyncRigidBodyMessage.class, ServerSyncCharacterMessage.class);
    }

    /**
     * listens to effect messages
     */
    private void createEffectListeners() {
        client.addMessageListener(new MessageListener() {

            public void messageReceived(Message message) {
                final ServerEffectMessage msg = (ServerEffectMessage) message;
                app.enqueue(new Callable<Void>() {

                    public Void call() throws Exception {
                        effectsManager.playEffect(msg.id, msg.name, msg.location, msg.endLocation, msg.rotation, msg.endRotation, msg.time);
                        return null;
                    }
                });
            }

            public void messageSent(Message message) {
            }

            public void objectReceived(Object object) {
            }

            public void objectSent(Object object) {
            }
        }, ServerEffectMessage.class);
    }

    /**
     * sets the status text in the login window
     * @param text
     */
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

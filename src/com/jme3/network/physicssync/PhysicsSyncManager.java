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
package com.jme3.network.physicssync;

import com.jme3.app.Application;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.control.VehicleControl;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.network.connection.Server;
import com.jme3.network.connection.Client;
import com.jme3.network.events.MessageListener;
import com.jme3.network.message.Message;
import com.jme3.scene.Spatial;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>Handles syncing of physics enabled server/client games. Puts messages in a queue
 * and executes them based on server time stamp plus an offset on the client.
 * The offset is calculated for each arriving message, if the time offset change
 * is bigger than maxDelay or smaller than zero (the message would be played either
 * very late or has happened already) then the offset time is adjusted.</p>
 * <p></p>
 * @author normenhansen
 */
public class PhysicsSyncManager implements MessageListener {

    private Server server;
    private Client client;
    private float syncFrequency = 0.25f;
    LinkedList<SyncMessageValidator> validators = new LinkedList<SyncMessageValidator>();
    HashMap<Long, Object> syncObjects = new HashMap<Long, Object>();
    double time = 0;
    double offset = Double.MIN_VALUE;
    private double maxDelay = 0.50;
    float syncTimer = 0;
    LinkedList<PhysicsSyncMessage> messageQueue = new LinkedList<PhysicsSyncMessage>();
    Application app;

    public PhysicsSyncManager(Application app, Server server) {
        this.app = app;
        this.server = server;
    }

    public PhysicsSyncManager(Application app, Client client) {
        this.app = app;
        this.client = client;
    }

    /**
     * updates the PhysicsSyncManager, executes messages on client and sends
     * sync info on the server.
     * @param tpf
     */
    public void update(float tpf) {
        time += tpf;
        if (time < 0) {
            //TODO: overflow
            time = 0;
        }
        if (client != null) {
            for (Iterator<PhysicsSyncMessage> it = messageQueue.iterator(); it.hasNext();) {
                PhysicsSyncMessage message = it.next();
                if (message.time >= time + offset) {
                    doMessage(message);
                    it.remove();
                }
            }
        } else if (server != null) {
            syncTimer += tpf;
            if (syncTimer >= syncFrequency) {
                sendSyncData();
                syncTimer = 0;
            }
        }
    }

    /**
     * add an object to the list of objects managed by this sync manager
     * @param id
     * @param object
     */
    public void addObject(long id, Object object) {
        syncObjects.put(id, object);
    }

    /**
     * removes an object from the list of objects managed by this sync manager
     * @param object
     */
    public void removeObject(Object object) {
        for (Iterator<Entry<Long, Object>> it = syncObjects.entrySet().iterator(); it.hasNext();) {
            Entry<Long, Object> entry = it.next();
            if (entry.getValue() == object) {
                it.remove();
                return;
            }
        }
    }

    /**
     * removes an object from the list of objects managed by this sync manager
     * @param id
     */
    public void removeObject(long id) {
        syncObjects.remove(id);
    }

    public void clearObjects() {
        syncObjects.clear();
    }

    /**
     * executes a message immediately
     * @param message
     */
    protected void doMessage(PhysicsSyncMessage message) {
        Object object = syncObjects.get(message.syncId);
        if (object != null) {
            message.applyData(object);
        } else {
            Logger.getLogger(PhysicsSyncManager.class.getName()).log(Level.WARNING, "Cannot find physics object for: ({0}){1}", new Object[]{message.syncId, message});
        }
    }

    /**
     * enqueues the message and updates the offset of the sync manager based on the
     * time stamp
     * @param message
     */
    protected void enqueueMessage(PhysicsSyncMessage message) {
        if (offset == Double.MIN_VALUE) {
            offset = this.time - message.time;
            Logger.getLogger(PhysicsSyncManager.class.getName()).log(Level.INFO, "Initial offset {0}", offset);
        }
        double delayTime = (message.time + offset) - time;
        if (delayTime > maxDelay) {
            offset -= delayTime - maxDelay;
            Logger.getLogger(PhysicsSyncManager.class.getName()).log(Level.INFO, "Decrease offset due to high delaytime ({0})", delayTime);
        } else if (delayTime < 0) {
            offset -= delayTime;
            Logger.getLogger(PhysicsSyncManager.class.getName()).log(Level.INFO, "Increase offset due to low delaytime ({0})", delayTime);
        }
        messageQueue.add(message);
    }

    /**
     * sends sync data for all active physics objects
     */
    protected void sendSyncData() {
        for (Iterator<Entry<Long, Object>> it = syncObjects.entrySet().iterator(); it.hasNext();) {
            Entry<Long, Object> entry = it.next();
            if (entry.getValue() instanceof Spatial) {
                Spatial spat = (Spatial) entry.getValue();
                PhysicsRigidBody body = spat.getControl(RigidBodyControl.class);
                if (body == null) {
                    body = spat.getControl(VehicleControl.class);
                }
                if (body != null && body.isActive()) {
                    SyncRigidBodyMessage msg = new SyncRigidBodyMessage(entry.getKey(), body);
                    broadcast(msg);
                    continue;
                }
                CharacterControl control = spat.getControl(CharacterControl.class);
                if (control != null) {
                    SyncCharacterMessage msg = new SyncCharacterMessage(entry.getKey(), control);
                    broadcast(msg);
                }
            }
        }
    }

    /**
     * use to broadcast physics control messages if server, applies timestamp to
     * PhysicsSyncMessage, call from OpenGL thread!
     * @param msg
     */
    public void broadcast(PhysicsSyncMessage msg) {
        if (server == null) {
            Logger.getLogger(PhysicsSyncManager.class.getName()).log(Level.SEVERE, "Broadcasting message on client {0}", msg);
            return;
        }
        msg.time = time;
        try {
            server.broadcast(msg);
        } catch (IOException ex) {
            Logger.getLogger(PhysicsSyncManager.class.getName()).log(Level.SEVERE, "Cannot broadcast message: {0}", ex);
        }
    }

    /**
     * send data to a specific client
     * @param client
     * @param msg
     */
    public void send(int client, PhysicsSyncMessage msg) {
        if (server == null) {
            Logger.getLogger(PhysicsSyncManager.class.getName()).log(Level.SEVERE, "Broadcasting message on client {0}", msg);
            return;
        }
        send(server.getClientByID(client), msg);
    }

    /**
     * send data to a specific client
     * @param client
     * @param msg
     */
    public void send(Client client, PhysicsSyncMessage msg) {
        msg.time = time;
        try {
            if (client == null) {
                Logger.getLogger(PhysicsSyncManager.class.getName()).log(Level.SEVERE, "Client null when sending: {0}", client);
                return;
            }
            client.send(msg);
        } catch (IOException ex) {
            Logger.getLogger(PhysicsSyncManager.class.getName()).log(Level.SEVERE, "Cannot broadcast message: {0}", ex);
        }
    }

    /**
     * registers the types of messages this PhysicsSyncManager listens to
     * @param classes
     */
    public void setMessageTypes(Class... classes) {
        if (server != null) {
            server.removeMessageListener(this);
            server.addMessageListener(this, classes);
        } else if (client != null) {
            client.removeMessageListener(this);
            client.addMessageListener(this, classes);
        }
    }

    public void messageSent(Message message) {
    }

    public void objectReceived(Object object) {
    }

    public void objectSent(Object object) {
    }

    public void messageReceived(final Message message) {
        assert (message instanceof PhysicsSyncMessage);
        if (client != null) {
            app.enqueue(new Callable<Void>() {

                public Void call() throws Exception {
                    enqueueMessage((PhysicsSyncMessage) message);
                    return null;
                }
            });
        } else if (server != null) {
            app.enqueue(new Callable<Void>() {

                public Void call() throws Exception {
                    for (Iterator<SyncMessageValidator> it = validators.iterator(); it.hasNext();) {
                        SyncMessageValidator syncMessageValidator = it.next();
                        if (!syncMessageValidator.checkMessage((PhysicsSyncMessage) message)) {
                            return null;
                        }
                    }
                    broadcast((PhysicsSyncMessage) message);
                    doMessage((PhysicsSyncMessage) message);
                    return null;
                }
            });
        }
    }

    public void addMessageValidator(SyncMessageValidator validator) {
        validators.add(validator);
    }

    public void removeMessageValidator(SyncMessageValidator validator) {
        validators.remove(validator);
    }

    public Server getServer() {
        return server;
    }

    public Client getClient() {
        return client;
    }

    public double getMaxDelay() {
        return maxDelay;
    }

    public void setMaxDelay(double maxDelay) {
        this.maxDelay = maxDelay;
    }

    public float getSyncFrequency() {
        return syncFrequency;
    }

    public void setSyncFrequency(float syncFrequency) {
        this.syncFrequency = syncFrequency;
    }
}

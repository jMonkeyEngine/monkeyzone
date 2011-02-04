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
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.network.connection.Server;
import com.jme3.network.connection.Client;
import com.jme3.network.events.MessageListener;
import com.jme3.network.message.Message;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles syncing of physics enabled server/client games.
 * @author normenhansen
 */
public class PhysicsSyncManager implements MessageListener {

    private Server server;
    private Client client;
    private float syncFrequency = 0.25f;
    HashMap<Long, Object> syncObjects = new HashMap<Long, Object>();
    double time = 0;
    double offset = Double.MIN_VALUE;
    private double maxDelay = 0.25;
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
        client.addMessageListener(this, SyncCharacterMessage.class, SyncRigidBodyMessage.class, RigidBodyControlMessage.class, CharacterControlMessage.class);
    }

    //TODO: run on physics tick..?
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

    public long addObject(PhysicsCollisionObject object) {
        long id = 0;
        while (syncObjects.containsKey(id)) {
            id++;
        }
        syncObjects.put(id, object);
        return id;
    }

    public void addObject(long id, Object object) {
        syncObjects.put(id, object);
    }

    public void removeObject(Object object) {
        for (Iterator<Entry<Long, Object>> it = syncObjects.entrySet().iterator(); it.hasNext();) {
            Entry<Long, Object> entry = it.next();
            if (entry.getValue() == object) {
                it.remove();
                return;
            }
        }
    }

    public void removeObject(long id) {
        syncObjects.remove(id);
    }

    protected void doMessage(PhysicsSyncMessage message) {
        Object object = syncObjects.get(message.syncId);
        if (object != null) {
            message.applyData(object);
        } else {
            Logger.getLogger(PhysicsSyncManager.class.getName()).log(Level.WARNING, "Cannot find physics object for: ({0}){1}", new Object[]{message.syncId, message});
        }
    }

    protected void delayMessage(PhysicsSyncMessage message) {
        if (offset == Double.MIN_VALUE) {
            offset = this.time - message.time;
            Logger.getLogger(PhysicsSyncManager.class.getName()).log(Level.INFO, "Initial offset {0}", offset);
        }
        double delayTime = (message.time + offset) - time;
        if (delayTime > maxDelay) {
            offset -= delayTime - maxDelay;
            Logger.getLogger(PhysicsSyncManager.class.getName()).log(Level.INFO, "Decrease offset due to high delaytime ({0})",  delayTime);
        } else if (delayTime < 0) {
            offset -= delayTime;
            Logger.getLogger(PhysicsSyncManager.class.getName()).log(Level.INFO, "Increase offset due to low delaytime ({0})",  delayTime);
        }
        messageQueue.add(message);
    }

    protected void sendSyncData() {
        for (Iterator<Entry<Long, Object>> it = syncObjects.entrySet().iterator(); it.hasNext();) {
            Entry<Long, Object> entry = it.next();
            if (entry.getValue() instanceof PhysicsRigidBody) {
                PhysicsRigidBody control = (PhysicsRigidBody) entry.getValue();
                if (control.isActive()) {
                    SyncRigidBodyMessage msg = new SyncRigidBodyMessage(entry.getKey(), control);
                    broadcast(msg);
                }
            } else if (entry.getValue() instanceof CharacterControl) {
                CharacterControl control = (CharacterControl) entry.getValue();
                SyncCharacterMessage msg = new SyncCharacterMessage(entry.getKey(), control);
                broadcast(msg);
            }
        }
    }

    /**
     * use to broadcast physics control messages if server, call from OpenGL thread!
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
        if (client != null) {
            app.enqueue(new Callable<Void>() {

                public Void call() throws Exception {
                    delayMessage((PhysicsSyncMessage) message);
//                    doMessage((PhysicsSyncMessage) message);
                    return null;
                }
            });
        } else if (server != null) {
            app.enqueue(new Callable<Void>() {

                public Void call() throws Exception {
                    doMessage((PhysicsSyncMessage) message);
                    return null;
                }
            });
        }
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

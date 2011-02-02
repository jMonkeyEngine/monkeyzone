/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
 *
 * @author normenhansen
 */
public class PhysicsSyncManager implements MessageListener {

    Server server;
    Client client;
    float syncFrequency = 0.25f;
    HashMap<Long, PhysicsCollisionObject> physicsObjects = new HashMap<Long, PhysicsCollisionObject>();
    double time = 0;
    double offset = Double.MIN_VALUE;
    double latency = 0;
    float syncTimer = 0;
    List<AbstractPhysicsSyncMessage> messageQueue = new LinkedList<AbstractPhysicsSyncMessage>();
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

    //TODO: run on physics tick..
    public void update(float tpf) {
        time += tpf;
        if (time < 0) {
            //overflow
            time = 0;
        }
        if (client != null) {
            for (Iterator<AbstractPhysicsSyncMessage> it = messageQueue.iterator(); it.hasNext();) {
                AbstractPhysicsSyncMessage message = it.next();
                if (message.delayTime <= 0) {
                    doMessage(message);
                    it.remove();
                } else {
                    message.delayTime -= tpf;
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

    public void addObject(PhysicsCollisionObject object) {
        long id = 0;
        while (physicsObjects.containsKey(id)) {
            id++;
        }
        physicsObjects.put(id, object);
    }

    public void addObject(long id, PhysicsCollisionObject object) {
        physicsObjects.put(id, object);
    }

    public void removeObject(PhysicsCollisionObject object) {
        for (Iterator<Entry<Long, PhysicsCollisionObject>> it = physicsObjects.entrySet().iterator(); it.hasNext();) {
            Entry<Long, PhysicsCollisionObject> entry = it.next();
            if (entry.getValue() == object) {
                it.remove();
                return;
            }
        }
    }

    public void removeObject(long id) {
        physicsObjects.remove(id);
    }

    protected void doMessage(AbstractPhysicsSyncMessage message) {
        PhysicsCollisionObject control = physicsObjects.get(message.id);
        if (control != null) {
            message.applyData(control);
        } else {
            Logger.getLogger(PhysicsSyncManager.class.getName()).log(Level.WARNING, "Cannot find physics object for: {0}", message.id);
        }
    }

    protected void applyMessageDelay(AbstractPhysicsSyncMessage msg) {
        double thisoffset = msg.time - this.time;
        if (thisoffset > offset) {
            offset = thisoffset;
            doMessage(msg);
            return;
        } else {
            offset = offset - .1;
        }
        double delayTime = time - (msg.time - offset);
        msg.delayTime = delayTime;
        messageQueue.add(msg);
    }

    protected void sendSyncData() {
        for (Iterator<Entry<Long, PhysicsCollisionObject>> it = physicsObjects.entrySet().iterator(); it.hasNext();) {
            Entry<Long, PhysicsCollisionObject> entry = it.next();
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
     * use to broadcast physics control messages, call from OpenGL thread!
     * @param msg
     */
    public void broadcast(AbstractPhysicsSyncMessage msg) {
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

    protected void broadcastExcept(Client client, AbstractPhysicsSyncMessage msg) {
        msg.time = time;
        try {
            server.broadcastExcept(client, msg);
        } catch (IOException ex) {
            Logger.getLogger(PhysicsSyncManager.class.getName()).log(Level.SEVERE, "Cannot broadcast message: {0}", ex);
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
                    applyMessageDelay((AbstractPhysicsSyncMessage) message);
                    return null;
                }
            });
        } else if (server != null) {
            app.enqueue(new Callable<Void>() {

                public Void call() throws Exception {
                    doMessage((AbstractPhysicsSyncMessage) message);
                    return null;
                }
            });
        }
    }
}

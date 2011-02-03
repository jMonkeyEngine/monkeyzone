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

    private Server server;
    private Client client;
    float syncFrequency = 0.25f;
    HashMap<Long, Object> syncObjects = new HashMap<Long, Object>();
    double time = 0;
    double offset = Double.MIN_VALUE;
    private double maxDelay = .075;
    double offsetChangeValue = .06;
    float syncTimer = 0;
    List<PhysicsSyncMessage> messageQueue = new LinkedList<PhysicsSyncMessage>();
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
            for (Iterator<PhysicsSyncMessage> it = messageQueue.iterator(); it.hasNext();) {
                PhysicsSyncMessage message = it.next();
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
        double thisoffset = this.time - message.time;
        if (thisoffset > offset) {
            offset = thisoffset;
//            Logger.getLogger(PhysicsSyncManager.class.getName()).log(Level.INFO, "upping offset {0}", thisoffset);
        }
        double delayTime = (message.time + offset) - time;
        if (delayTime > maxDelay) {
            offset -= delayTime - maxDelay;
            delayTime = 0;
            Logger.getLogger(PhysicsSyncManager.class.getName()).log(Level.INFO, "downing high delaytime", delayTime);
        }
        message.delayTime = delayTime;
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
        if(server==null){
            Logger.getLogger(PhysicsSyncManager.class.getName()).log(Level.SEVERE, "Broadcasting message on client {0}",msg);
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
}

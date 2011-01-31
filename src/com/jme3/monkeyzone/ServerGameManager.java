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

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsRayTestResult;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.monkeyzone.messages.ClientActionMessage;
import com.jme3.monkeyzone.messages.ServerEffectMessage;
import com.jme3.monkeyzone.messages.StartGameMessage;
import com.jme3.network.connection.Server;
import com.jme3.scene.Spatial;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the actual gameplay on the server side
 * @author normenhansen
 */
public class ServerGameManager {

    Server server;
    WorldManager worldManager;
    private boolean running;
    String mapName;
    String[] modelNames;
    float syncTimer = 0;
    PhysicsSpace space;

    public ServerGameManager(Server server, WorldManager worldManager, PhysicsSpace space) {
        this.server = server;
        this.worldManager = worldManager;
        this.space = space;
    }

    /**
     * starts the game
     */
    public synchronized boolean startGame(String map) {
        if (running) {
            return false;
        }
        running = true;
        //TODO: parse client side string, create model list automatically
        mapName = map;
        modelNames = new String[]{"Models/HoverTank/HoverTank.j3o", "Models/Sinbad/Sinbad.j3o", "Models/Ferrari/Car.j3o", "Models/Buggy/Buggy.j3o"};
        try {
            server.broadcast(new StartGameMessage(mapName, modelNames));
        } catch (IOException ex) {
            Logger.getLogger(ServerGameManager.class.getName()).log(Level.SEVERE, "{0}", ex);
        }
        worldManager.loadLevel(mapName);
        worldManager.createNavMesh();
        worldManager.preloadModels(modelNames);
        worldManager.attachLevel();
        //add all players and create character entites for them, then enter the entites
        int i = 0;
        for (Iterator<PlayerData> it = PlayerData.getPlayers().iterator(); it.hasNext();) {
            PlayerData playerData = it.next();
//            long entityId = worldManager.addNewEntity("Models/Ferrari/Car.j3o", new Vector3f(i*3, 3, 0), new Quaternion());
//            long entityId = worldManager.addNewEntity("Models/Buggy/Buggy.j3o", new Vector3f(i*3, 3, 0), new Quaternion());
            long entityId = worldManager.addNewEntity("Models/Sinbad/Sinbad.j3o", new Vector3f(i * 3, 3, 0), new Quaternion());
            worldManager.enterEntity(playerData.getId(), entityId);
            i++;
        }
        return true;
    }

    /**
     * stops the game
     */
    public synchronized boolean stopGame() {
        if (!running) {
            return false;
        }
        mapName = "null";
        modelNames = new String[]{};
        try {
            server.broadcast(new StartGameMessage(mapName, modelNames));
        } catch (IOException ex) {
            Logger.getLogger(ServerGameManager.class.getName()).log(Level.SEVERE, "{0}", ex);
        }
        worldManager.closeLevel();
        running = false;
        return true;
    }

    /**
     * checks if the game is running
     * @return
     */
    public synchronized boolean isRunning() {
        return running;
    }

    /**
     * called when an entity (human or AI) performs an action
     * @param entity
     * @param action
     * @param pressed
     */
    public void performAction(long entity, int action, boolean pressed) {
        Spatial myEntity = worldManager.getEntity(entity);
        if (myEntity == null) {
            Logger.getLogger(ServerGameManager.class.getName()).log(Level.SEVERE, "Cannot find entity peroforming action!");
            return;
        }
        long player_id = (Long) myEntity.getUserData("player_id");
        if (player_id == -1) {
            Logger.getLogger(ServerGameManager.class.getName()).log(Level.SEVERE, "Cannot find player id for entity peroforming action!");
            return;
        }
        //switch car and character on pressing enter
        if (action == ClientActionMessage.ENTER_ACTION && pressed) {
            if (myEntity.getControl(CharacterControl.class) != null) {
                long entity_id = worldManager.addNewEntity("Models/Ferrari/Car.j3o", myEntity.getWorldTranslation().add(Vector3f.UNIT_Y), myEntity.getWorldRotation());
                worldManager.enterEntity(player_id, entity_id);
                worldManager.removeEntity(entity);
            } else {
                long entity_id = worldManager.addNewEntity("Models/Sinbad/Sinbad.j3o", myEntity.getWorldTranslation().add(Vector3f.UNIT_Y), myEntity.getWorldRotation());
                worldManager.enterEntity(player_id, entity_id);
                worldManager.removeEntity(entity);
            }
        } //shoot on space
        else if (action == ClientActionMessage.SPACE_ACTION && pressed) {
            CharacterControl control = myEntity.getControl(CharacterControl.class);
            if (control == null) {
                Logger.getLogger(ServerGameManager.class.getName()).log(Level.WARNING, "Cannot shoot when not character!");
                return;
            }
            System.out.println("shoot!");
            try {
                //FIXME: client crashes with NPE in ParticleEmitter.render() when using particles..
                //TODO: use generated id for effect, not entity id..
                server.broadcast(new ServerEffectMessage(entity, "Effects/ExplosionB.j3o", myEntity.getWorldTranslation(), myEntity.getWorldRotation(), myEntity.getWorldTranslation(), myEntity.getWorldRotation(), 2.0f));
            } catch (IOException ex) {
                Logger.getLogger(ServerGameManager.class.getName()).log(Level.SEVERE, null, ex);
            }
            //TODO: doing raytest for shooting.. 
            List<PhysicsRayTestResult> list = space.rayTest(control.getPhysicsLocation(), control.getPhysicsLocation().add(control.getWalkDirection().mult(10)));
            for (Iterator<PhysicsRayTestResult> it = list.iterator(); it.hasNext();) {
                PhysicsRayTestResult physicsRayTestResult = it.next();
                Object obj = physicsRayTestResult.getCollisionObject().getUserObject();
                System.out.println("collide!");
                if (obj instanceof Spatial) {
                    System.out.println("spatial!");
                    Spatial spatial = (Spatial) obj;
                    long entityId = worldManager.getEntityId(spatial);
                    if (entityId != -1 && entityId != entity) {
                        System.out.println("entity!");
                        //TODO: we hit something!
                    }
                }
            }
        }
    }

    /**
     * the update loop, mainly sends out sync infos in intervals
     * @param tpf
     */
    public synchronized void update(float tpf) {
        if (!running) {
            return;
        }
        syncTimer += tpf;
        if (syncTimer >= Globals.NETWORK_SYNC_FREQUENCY) {
            worldManager.sendSyncData();
            syncTimer = 0;
        }
    }
}

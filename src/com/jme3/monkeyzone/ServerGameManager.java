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
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.monkeyzone.messages.ActionMessage;
import com.jme3.monkeyzone.messages.StartGameMessage;
import com.jme3.network.physicssync.PhysicsSyncManager;
import com.jme3.scene.Spatial;
import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the actual gameplay on the server side
 * @author normenhansen
 */
public class ServerGameManager {

    PhysicsSyncManager server;
    WorldManager worldManager;
    private boolean running;
    String mapName;
    String[] modelNames;

    public ServerGameManager(WorldManager worldManager) {
        this.worldManager = worldManager;
        this.server = worldManager.getSyncManager();
    }

    /**
     * starts the game
     */
    public synchronized boolean startGame(String map) {
        if (running) {
            return false;
        }
        running = true;
        mapName = map;
        //TODO: parse client side string, create preload model list automatically
        modelNames = new String[]{"Models/HoverTank/HoverTank.j3o", "Models/Sinbad/Sinbad.j3o", "Models/Ferrari/Car.j3o", "Models/Buggy/Buggy.j3o"};
        try {
            server.getServer().broadcast(new StartGameMessage(mapName, modelNames));
        } catch (IOException ex) {
            Logger.getLogger(ServerGameManager.class.getName()).log(Level.SEVERE, "Cannot broadcast startgame: {0}", ex);
        }
        worldManager.loadLevel(mapName);
        worldManager.createNavMesh();
        worldManager.preloadModels(modelNames);
        worldManager.attachLevel();
        
        //create character entities for all players, then enter the entites
        int i = 0;
        for (Iterator<PlayerData> it = PlayerData.getPlayers().iterator(); it.hasNext();) {
            PlayerData playerData = it.next();
            long entityId = worldManager.addNewEntity("Models/Sinbad/Sinbad.j3o", new Vector3f(i * 3, 3, 0), new Quaternion());
            playerData.setData("character_entity_id", entityId);
            worldManager.enterEntity(playerData.getId(), entityId);

            //create new ai player for user
            long playearId = worldManager.addNewPlayer(PlayerData.getIntData(playerData.getId(), "group_id"), "AI", 0);
            long entitayId = worldManager.addNewEntity("Models/Sinbad/Sinbad.j3o", new Vector3f(i * 3, 3, 3), new Quaternion());
            PlayerData.setData(playearId, "character_entity_id", entitayId);
            worldManager.enterEntity(playearId, entitayId);

            //create a vehicle
            worldManager.addNewEntity("Models/HoverTank/HoverTank.j3o", new Vector3f(i * 3, 3, -3), new Quaternion());
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
            server.getServer().broadcast(new StartGameMessage(mapName, modelNames));
        } catch (IOException ex) {
            Logger.getLogger(ServerGameManager.class.getName()).log(Level.SEVERE, null, ex);
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
     * @param entityId
     * @param action
     * @param pressed
     */
    public void performAction(long entityId, int action, boolean pressed) {
        Spatial myEntity = worldManager.getEntity(entityId);
        if (myEntity == null) {
            Logger.getLogger(ServerGameManager.class.getName()).log(Level.WARNING, "Cannot find entity performing action!");
            return;
        }
        long player_id = (Long) myEntity.getUserData("player_id");
        if (player_id == -1) {
            Logger.getLogger(ServerGameManager.class.getName()).log(Level.WARNING, "Cannot find player id for entity performing action!");
            return;
        }
        //enter entity
        if (action == ActionMessage.ENTER_ACTION && !pressed) {
            performEnterEntity(player_id, myEntity);
        } else if (action == ActionMessage.SHOOT_ACTION && pressed) {
            performShoot(myEntity);
        }
    }

    /**
     * handle player performing "enter entity" action
     * @param player_id
     * @param myEntity
     */
    private void performEnterEntity(long player_id, Spatial myEntity) {
        long characterId = PlayerData.getLongData(player_id, "character_entity_id");
        long curEntityId = (Long) myEntity.getUserData("entity_id");
        Spatial entity = worldManager.doRayTest(myEntity, 4, null);
        if (entity != null && (Long) entity.getUserData("player_id") == -1l) {
            if (curEntityId == characterId) {
                worldManager.disableEntity(characterId);
                worldManager.enterEntity(player_id, (Long) entity.getUserData("entity_id"));
            } else {
                worldManager.enterEntity(player_id, characterId);
                worldManager.enableEntity(characterId, myEntity.getWorldTranslation().add(Vector3f.UNIT_Y), myEntity.getWorldRotation());
            }
        } else {
            if (curEntityId != characterId) {
                worldManager.enterEntity(player_id, characterId);
                worldManager.enableEntity(characterId, myEntity.getWorldTranslation().add(Vector3f.UNIT_Y), myEntity.getWorldRotation());
            }
        }
    }

    /**
     * handle entity shooting
     * @param myEntity
     */
    private void performShoot(Spatial myEntity) {
        CharacterControl control = myEntity.getControl(CharacterControl.class);
        if (control == null) {
            Logger.getLogger(ServerGameManager.class.getName()).log(Level.WARNING, "Cannot shoot when not character!");
            return;
        }
        worldManager.playWorldEffect("Effects/GunShotA.j3o", myEntity.getWorldTranslation(), 0.1f);
        Vector3f hitLocation = new Vector3f();
        Spatial hitEntity = worldManager.doRayTest(myEntity, 10, hitLocation);
        if (hitEntity != null) {
            long targetId = (Long) hitEntity.getUserData("entity_id");
            Float hp = (Float) hitEntity.getUserData("HitPoints");
            if (hp != null) {
                hp -= 10;
                worldManager.playWorldEffect("Effects/ExplosionA.j3o", hitLocation, 2.0f);
                worldManager.setEntityUserData(targetId, "HitPoints", hp);
                if (hp <= 0) {
                    worldManager.removeEntity(targetId);
                    worldManager.playWorldEffect("Effects/ExplosionB.j3o", hitEntity.getWorldTranslation(), 2.0f);
                }
            }
        }
    }
}

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

import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.PhysicsControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.control.VehicleControl;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.monkeyzone.controls.AIControl;
import com.jme3.monkeyzone.controls.AutonomousCharacterControl;
import com.jme3.monkeyzone.controls.AutonomousControl;
import com.jme3.monkeyzone.controls.AutonomousVehicleControl;
import com.jme3.monkeyzone.controls.CharacterAnimControl;
import com.jme3.monkeyzone.controls.ManualCharacterControl;
import com.jme3.monkeyzone.controls.ManualControl;
import com.jme3.monkeyzone.controls.ManualVehicleControl;
import com.jme3.monkeyzone.messages.AutoControlMessage;
import com.jme3.monkeyzone.messages.ManualControlMessage;
import com.jme3.monkeyzone.messages.ServerAddEntityMessage;
import com.jme3.monkeyzone.messages.ServerAddPlayerMessage;
import com.jme3.monkeyzone.messages.ServerSyncCharacterMessage;
import com.jme3.monkeyzone.messages.ServerEnterEntityMessage;
import com.jme3.monkeyzone.messages.ServerSyncRigidBodyMessage;
import com.jme3.monkeyzone.messages.ServerRemoveEntityMessage;
import com.jme3.network.connection.Client;
import com.jme3.network.connection.Server;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3tools.navmesh.NavMesh;
import jme3tools.navmesh.util.NavMeshGenerator;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import jme3tools.optimize.GeometryBatchFactory;

/**
 * Base game entity managing class, stores and loads the entities,
 * used on server and on client. Automatically sends changes via network when
 * running on server, used to apply network data on client and server.
 * @author normenhansen
 */
public class WorldManager {

    private Server server;
    private Client client;
    private long myPlayerId = -1;
    private long myGroupId = -1;
    private NavMesh navMesh = new NavMesh();
    private Node rootNode;
    private Node worldRoot;
    private HashMap<Long, Spatial> entities = new HashMap<Long, Spatial>();
    private AssetManager assetManager;
    private NavMeshGenerator generator = new NavMeshGenerator();
    private PhysicsSpace space;
    private List<Control> userControls = new LinkedList<Control>();

    public WorldManager(Node rootNode, AssetManager manager, PhysicsSpace space, Server server) {
        this.rootNode = rootNode;
        this.assetManager = manager;
        this.space = space;
        this.server = server;
    }

    public WorldManager(Node rootNode, AssetManager manager, PhysicsSpace space, Client client) {
        this.rootNode = rootNode;
        this.assetManager = manager;
        this.space = space;
        this.client = client;
    }

    public boolean isServer() {
        return server != null;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    /**
     * adds a control to the list of controls that are added to the spatial
     * currently controlled by the user (chasecam, ui control etc.)
     * @param control
     */
    public void addUserControl(Control control) {
        userControls.add(control);
    }

    public long getMyPlayerId() {
        return myPlayerId;
    }

    public void setMyPlayerId(long myPlayerId) {
        this.myPlayerId = myPlayerId;
    }

    public long getMyGroupId() {
        return myGroupId;
    }

    public void setMyGroupId(long myGroupId) {
        this.myGroupId = myGroupId;
    }

    /**
     * loads the specified level node
     * @param name
     */
    public void loadLevel(String name) {
        worldRoot = (Node) assetManager.loadModel(name);
    }

    /**
     * detaches the level and clears the cache
     */
    public void closeLevel() {
        //TODO: remove AI players
        removeUserControls(myPlayerId);
        space.removeAll(worldRoot);
        rootNode.detachChild(worldRoot);
        entities.clear();
        ((DesktopAssetManager) assetManager).clearCache();
    }

    /**
     * preloads the models with the given names
     * @param modelNames
     */
    public void preloadModels(String[] modelNames) {
        for (int i = 0; i < modelNames.length; i++) {
            String string = modelNames[i];
            assetManager.loadModel(string);
        }
    }

    /**
     * creates the nav mesh for the loaded level
     */
    public void createNavMesh() {

        Mesh mesh = new Mesh();

        //version a: from mesh
        GeometryBatchFactory.mergeGeometries(findGeometries(worldRoot, new LinkedList<Geometry>()), mesh);
        Mesh optiMesh = generator.optimize(mesh);

        navMesh.loadFromMesh(optiMesh);

//        Geometry navBaseGeom = new Geometry("NavBaseMesh");
//        navBaseGeom.setMesh(mesh);
//        Material red = new Material(manager, "Common/MatDefs/Misc/WireColor.j3md");
//        red.setColor("Color", ColorRGBA.Red);
//        navBaseGeom.setMaterial(red);
//        rootNode.attachChild(navBaseGeom);

        Geometry navGeom = new Geometry("NavMesh");
        navGeom.setMesh(optiMesh);
        Material green = new Material(assetManager, "Common/MatDefs/Misc/WireColor.j3md");
        green.setColor("Color", ColorRGBA.Green);
        navGeom.setMaterial(green);

        //XXX: oopsie, are we attaching on another thread here? :D works and is for debug only
        worldRoot.attachChild(navGeom);
    }

    /**
     * attaches the level node to the rootnode
     */
    public void attachLevel() {
        space.addAll(worldRoot);
        rootNode.attachChild(worldRoot);
    }

    private List<Geometry> findGeometries(Node node, List<Geometry> geoms) {
        for (Iterator<Spatial> it = node.getChildren().iterator(); it.hasNext();) {
            Spatial spatial = it.next();
            if (spatial instanceof Geometry) {
                geoms.add((Geometry) spatial);
            } else if (spatial instanceof Node) {
                findGeometries((Node) spatial, geoms);
            }
        }
        return geoms;
    }

    /**
     * get the NavMesh of the currently loaded level
     * @return
     */
    public NavMesh getNavMesh() {
        return navMesh;
    }

    /**
     * get the world root node (not necessarily the application rootNode!)
     * @return
     */
    public Node getWorldRoot() {
        return worldRoot;
    }

    /**
     * adds a player (sends message if server)
     * @param id
     * @param groupId
     * @param name
     * @param aiId
     */
    public void addPlayer(long id, int groupId, String name, int aiId) {
        if (isServer()) {
            try {
                server.broadcast(new ServerAddPlayerMessage(id, name, groupId, aiId));
            } catch (IOException ex) {
                Logger.getLogger(WorldManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        PlayerData player = null;
        player = new PlayerData(id, groupId, name, aiId);
        PlayerData.add(id, player);
    }

    /**
     * removes a player
     * @param id
     */
    public void removePlayer(long id) {
        //TODO: remove other (AI) entities of his group..
        PlayerData.remove(id);
    }

    /**
     * add an entity (vehicle, immobile house etc), always related to a spatial
     * with specific userdata like hp, maxhp etc. (sends message if server)
     * @param id
     * @param modelIdentifier
     * @param location
     * @param rotation
     */
    public void addEntity(long id, String modelIdentifier, Vector3f location, Quaternion rotation) {
        if (isServer()) {
            try {
                server.broadcast(new ServerAddEntityMessage(id, modelIdentifier, location, rotation));
            } catch (IOException ex) {
                Logger.getLogger(WorldManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        Node entityModel = (Node) assetManager.loadModel(modelIdentifier);
        if (entityModel.getControl(RigidBodyControl.class) != null) {
            entityModel.getControl(RigidBodyControl.class).setPhysicsLocation(location);
            entityModel.getControl(RigidBodyControl.class).setPhysicsRotation(rotation.toRotationMatrix());
        } else if (entityModel.getControl(CharacterControl.class) != null) {
            entityModel.getControl(CharacterControl.class).setPhysicsLocation(location);
            entityModel.getControl(CharacterControl.class).setPhysicsRotation(rotation.toRotationMatrix());
            entityModel.addControl(new CharacterAnimControl());
        } else if (entityModel.getControl(VehicleControl.class) != null) {
            entityModel.getControl(VehicleControl.class).setPhysicsLocation(location);
            entityModel.getControl(VehicleControl.class).setPhysicsRotation(rotation.toRotationMatrix());
        } else {
            entityModel.setLocalTranslation(location);
            entityModel.setLocalRotation(rotation);
        }
        entities.put(id, entityModel);
        space.addAll(entityModel);
        worldRoot.attachChild(entityModel);
    }

    /**
     * gets the entity with the specified id
     * @param id
     * @return
     */
    public Spatial getEntity(long id) {
        return entities.get(id);
    }

    /**
     * adds a new entity (only used on server)
     * @param modelIdentifier
     * @param location
     * @param rotation
     * @return
     */
    public long addNewEntity(String modelIdentifier, Vector3f location, Quaternion rotation) {
        long id = 0;
        while (entities.containsKey(id)) {
            id++;
        }
        addEntity(id, modelIdentifier, location, rotation);
        return id;
    }

    /**
     * removes the entity with the specified id (sends message if server)
     * @param id
     */
    public void removeEntity(long id) {
        if (isServer()) {
            try {
                server.broadcast(new ServerRemoveEntityMessage(id));
            } catch (IOException ex) {
                Logger.getLogger(WorldManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        Spatial spat = entities.remove(id);
        spat.removeFromParent();
        if (spat.getControl(PhysicsControl.class) != null) {
            space.remove(spat.getControl(PhysicsControl.class));
        }
    }

    /**
     * finds the entity id of a given spatial if there is one
     * @param entity
     * @return
     */
    public long getEntityId(Spatial entity) {
        for (Iterator<Entry<Long, Spatial>> it = entities.entrySet().iterator(); it.hasNext();) {
            Entry<Long, Spatial> entry = it.next();
            if (entry.getValue() == entity) {
                return entry.getKey();
            }
        }
        return -1;
    }

    /**
     * handle player entering entity (sends message if server)
     * @param playerId
     * @param entityId
     */
    public void enterEntity(long playerId, long entityId) {
        if (isServer()) {
            try {
                server.broadcast(new ServerEnterEntityMessage(playerId, entityId));
            } catch (IOException ex) {
                Logger.getLogger(WorldManager.class.getName()).log(Level.SEVERE, "Cant broadcast enter entity: {0}", ex);
            }
        }
        long curEntity = PlayerData.getLongData(playerId, "entity_id");
        if (curEntity != -1) {
            Spatial curEntitySpat = getEntity(curEntity);
            curEntitySpat.setUserData("player_id", -1);
        }
        Spatial spat = getEntity(entityId);
        spat.setUserData("player_id", playerId);
        PlayerData.setData(playerId, "entity_id", entityId);
        removeMovementControls(curEntity);
        if (PlayerData.isHuman(playerId)) {
            if (playerId == getMyPlayerId()) { //only true on clients
                //TODO: check also for group, not just own entity id
                //to see if we have to add a client to send data
                makeManualControl(entityId, client);
                //move controls for local user to new spatial
                if (curEntity != -1) {
                    removeUserControls(curEntity);
                }
                addUserControls(spat);
            } else {
                makeManualControl(entityId, null);
            }
        } else {
            if (playerId == getMyPlayerId()) { //only true on clients
                makeAutoControl(entityId, client);
            } else {
                makeAutoControl(entityId, null);
            }
        }
    }

    /**
     * makes the specified entity ready to be manually controlled by adding
     * a ManualControl based on the entity type (vehicle etc)
     */
    private void makeManualControl(long entityId, Client client) {
        Spatial spat = getEntity(entityId);
        AutonomousControl autoControl = spat.getControl(AutonomousControl.class);
        if (autoControl != null) {
            spat.removeControl(autoControl);
        }
        ManualControl manualControl = spat.getControl(ManualControl.class);
        if (manualControl == null) {
            if (spat.getControl(CharacterControl.class) != null) {
                if (client != null) {
                    //add net sending for users own manual control
                    spat.addControl(new ManualCharacterControl(client, entityId));
                } else {
                    spat.addControl(new ManualCharacterControl());
                }
            } else if (spat.getControl(VehicleControl.class) != null) {
                if (client != null) {
                    //add net sending for users own manual control
                    spat.addControl(new ManualVehicleControl(client, entityId));
                } else {
                    spat.addControl(new ManualVehicleControl());
                }
            }
        }
    }

    /**
     * makes the specified entity ready to be controlled by an AIControl
     * by adding an AutonomousControl based on entity type.
     */
    private void makeAutoControl(long entityId, Client client) {
        Spatial spat = getEntity(entityId);
        ManualControl manualControl = spat.getControl(ManualControl.class);
        if (manualControl != null) {
            spat.removeControl(manualControl);
        }
        AutonomousControl autoControl = spat.getControl(AutonomousControl.class);
        if (autoControl == null) {
            //TODO: check for group id and add with client/id for networking (like manual)
            if (spat.getControl(CharacterControl.class) != null) {
                if (client != null) {
                    //TODO: clients for auto controls
                    spat.addControl(new AutonomousCharacterControl(client, entityId));
                } else {
                    spat.addControl(new AutonomousCharacterControl());
                }
            } else if (spat.getControl(VehicleControl.class) != null) {
                if (client != null) {
                    spat.addControl(new AutonomousVehicleControl(client, entityId));
                } else {
                    spat.addControl(new AutonomousVehicleControl());
                }
            }
        }
    }

    /**
     * removes all movement controls (ManualControl / AutonomousControl) from
     * entity
     * @param entityID
     */
    private void removeMovementControls(long entityID) {
        Spatial spat = getEntity(entityID);
        if (spat != null) {
            removeMovementControls(spat);
        }
    }

    /**
     * removes all movement controls (ManualControl / AutonomousControl) from
     * spatial
     * @param spat
     */
    private void removeMovementControls(Spatial spat) {
        ManualControl manualControl = spat.getControl(ManualControl.class);
        if (manualControl != null) {
            spat.removeControl(manualControl);
        }
        AutonomousControl autoControl = spat.getControl(AutonomousControl.class);
        if (autoControl != null) {
            spat.removeControl(autoControl);
        }
    }

    /**
     * adds the user controls for human user to the entity
     */
    private void addUserControls(long entityID) {
        Spatial spat = getEntity(entityID);
        addUserControls(spat);
    }

    /**
     * adds the user controls for human user to the entity
     */
    private void removeUserControls(long entityID) {
        Spatial spat = getEntity(entityID);
        if (spat != null) {
            removeUserControls(spat);
        }
    }

    /**
     * adds the user controls for human user to the spatial
     */
    private void removeUserControls(Spatial spat) {
        for (Iterator<Control> it = userControls.iterator(); it.hasNext();) {
            Control control = it.next();
            spat.removeControl(control);
        }
    }

    /**
     * adds the user controls for human user to the spatial
     */
    private void addUserControls(Spatial spat) {
        for (Iterator<Control> it = userControls.iterator(); it.hasNext();) {
            Control control = it.next();
            spat.addControl(control);
        }
    }

    /**
     * set user data of specified entity (sends message if server)
     * @param id
     * @param name
     * @param data
     */
    public void setEntityUserData(long id, String name, Object data) {
        getEntity(id).setUserData(name, data);
    }

    /**
     * play animation on specified entity
     * @param entityId
     * @param animationName
     * @param channel
     */
    public void playEntityAnimation(long entityId, String animationName, int channel) {
    }

    /**
     * applies an auto control message (AI)
     * @param msg
     */
    public void applyAutoControl(AutoControlMessage msg) {
        //TODO: check if call is valid in that the player controls an entity that belongs to him (possible exploit)
        //TODO: check of msg amounts <= 1
        if (isServer()) {
            try {
                //broadcasting movement to others only, client sets it itself (maybe change in future?)
                //client is synced later via global sync
                server.broadcastExcept(msg.getClient(), msg);
            } catch (IOException ex) {
                Logger.getLogger(WorldManager.class.getName()).log(Level.SEVERE, "Cant broadcast control message: {0}", ex);
            }
        }
        AutonomousControl control = getEntity(msg.id).getControl(AutonomousControl.class);
        control.aimAt(msg.aimAt);
        control.moveTo(msg.moveTo);
    }

    /**
     * applies a manual control message (human)
     * @param msg
     */
    public void applyManualControl(ManualControlMessage msg) {
        //TODO: check if call is valid in that the player controls an entity that belongs to him (possible exploit)
        //TODO: check of msg amounts <= 1
        if (isServer()) {
            try {
                //broadcasting movement to others only, client sets it itself (maybe change in future?)
                //client is synced later via global sync
                server.broadcastExcept(msg.getClient(), msg);
            } catch (IOException ex) {
                Logger.getLogger(WorldManager.class.getName()).log(Level.SEVERE, "Cant broadcast control message: {0}", ex);
            }
        }
        ManualControl control = getEntity(msg.id).getControl(ManualControl.class);
        control.steerX(msg.aimX);
        control.steerY(msg.aimY);
        control.moveX(msg.moveX);
        control.moveY(msg.moveY);
        control.moveZ(msg.moveZ);
    }

    /**
     * sends info about all entities
     */
    public void sendSyncData() {
        try {
            for (Iterator<Entry<Long, Spatial>> it = entities.entrySet().iterator(); it.hasNext();) {
                Entry<Long, Spatial> entry = it.next();
                if (entry.getValue().getControl(VehicleControl.class) != null) {
                    VehicleControl control = entry.getValue().getControl(VehicleControl.class);
                    if (control.isActive()) {
                        ServerSyncRigidBodyMessage msg = new ServerSyncRigidBodyMessage(entry.getKey(), control);
                        server.broadcast(msg);
                    }
                } else if (entry.getValue().getControl(RigidBodyControl.class) != null) {
                    RigidBodyControl control = entry.getValue().getControl(RigidBodyControl.class);
                    if (control.isActive()) {
                        ServerSyncRigidBodyMessage msg = new ServerSyncRigidBodyMessage(entry.getKey(), control);
                        server.broadcast(msg);
                    }
                } else if (entry.getValue().getControl(CharacterControl.class) != null) {
                    CharacterControl control = entry.getValue().getControl(CharacterControl.class);
                    ServerSyncCharacterMessage msg = new ServerSyncCharacterMessage(entry.getKey(), control);
                    server.broadcast(msg);
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(WorldManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static class AIControlFactory {

        public static final int DEFAULT_AI = 0;
        public static final int WORKER_AI = 1;
        public static final int FIGHTER_AI = 2;
        public static final int BASIC3_AI = 3;
        public static final int BASIC4_AI = 4;
        public static final int BASIC5_AI = 5;
        public static final int BASIC6_AI = 6;
        public static final int BASIC7_AI = 7;
        public static final int BASIC8_AI = 8;
        public static final int BASIC9_AI = 9;

        public static AIControl createAIControl(int id) {
            switch (id) {
                case 0:
                    break;
                case 1:
                    break;
                case 2:
                    break;
                case 3:
                    break;
                case 4:
                    break;
                case 5:
                    break;
            }
            return null;
        }
    }
}

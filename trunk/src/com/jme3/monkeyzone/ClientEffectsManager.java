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

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioNode;
import com.jme3.audio.AudioRenderer;
import com.jme3.effect.ParticleEmitter;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Plays effects on the client, uses lists to cache effects for fast display,
 * TODO: allow limiting effects count created in lists/world.
 * @author normenhansen
 */
public class ClientEffectsManager extends AbstractAppState {

    private AssetManager assetManager;
    private AudioRenderer audioRenderer;
    private WorldManager worldManager;
    private HashMap<String, LinkedList<Node>> emitters = new HashMap<String, LinkedList<Node>>();
    private HashMap<Long, EmitterData> liveEmitters = new HashMap<Long, EmitterData>();

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.assetManager = app.getAssetManager();
        this.audioRenderer = app.getAudioRenderer();
        this.worldManager = app.getStateManager().getState(WorldManager.class);
    }

    /**
     * plays an effect in the world
     * @param id
     * @param effectName
     * @param location
     * @param endLocation
     * @param rotation
     * @param endRotation
     * @param time
     */
    public void playEffect(long id, String effectName, Vector3f location, Vector3f endLocation, Quaternion rotation, Quaternion endRotation, float time) {
        if (liveEmitters.containsKey(id)) {
            Logger.getLogger(ClientEffectsManager.class.getName()).log(Level.WARNING, "Trying to add effect with existing id");
            return;
        }
        Node effect = getEffect(effectName);
        EmitterData data = new EmitterData(effect, id, effectName, location, endLocation, rotation, endRotation, time);
        effect.setLocalTranslation(location);
        effect.setLocalRotation(rotation);
        data.curTime = 0;
        data.time = time;
        List<Spatial> children = effect.getChildren();
        for (Iterator<Spatial> it = children.iterator(); it.hasNext();) {
            Spatial spat = it.next();
            if (spat instanceof ParticleEmitter) {
                ParticleEmitter emitter = (ParticleEmitter) spat;
                emitter.emitAllParticles();
            } else if (spat instanceof AudioNode) {
                AudioNode audioNode = (AudioNode) spat;
//                audioNode.setStatus(AudioNode.Status.Playing);
                audioRenderer.playSource(audioNode);
            }
        }
        worldManager.getWorldRoot().attachChild(effect);
        if (id == -1) {
            putToNew(data);
        } else {
            data.id = id;
            liveEmitters.put(id, data);
        }
    }

    private long putToNew(EmitterData data) {
        long id = 0;
        while (liveEmitters.containsKey(id)) {
            id++;
        }
        data.id = id;
        liveEmitters.put(id, data);
        return id;
    }

    /**
     * stops an effect
     * @param id
     */
    public void stopEffect(long id) {
        EmitterData data = liveEmitters.get(id);
        data.emit.removeFromParent();
        emitters.get(data.effectName).add(data.emit);
        List<Spatial> children = data.emit.getChildren();
        for (Iterator<Spatial> it = children.iterator(); it.hasNext();) {
            Spatial spatial = it.next();
            if (spatial instanceof AudioNode) {
                audioRenderer.stopSource((AudioNode) spatial);
            } else if (spatial instanceof ParticleEmitter) {
                ((ParticleEmitter) spatial).killAllParticles();
            }
        }
    }

    /**
     * gets an effect from the list (is exists) or loads a new one
     * @param name
     * @return
     */
    private Node getEffect(String name) {
        if (emitters.get(name) == null) {
            emitters.put(name, new LinkedList<Node>());
        }
        Node emit = emitters.get(name).poll();
        if (emit == null) {
            emit = (Node) assetManager.loadModel(name);
        }
        return emit;
    }

    private class EmitterData {

        public Node emit;
        public long id;
        public String effectName;
        public Vector3f location;
        public Vector3f endLocation;
        public Quaternion rotation;
        public Quaternion endRotation;
        public float time;
        public float curTime = 0;

        public EmitterData(Node emit, long id, String effectName, Vector3f location, Vector3f endLocation, Quaternion rotation, Quaternion endRotation, float time) {
            this.emit = emit;
            this.id = id;
            this.effectName = effectName;
            this.location = location;
            this.endLocation = endLocation;
            this.rotation = rotation;
            this.endRotation = endRotation;
            this.time = time;
        }
    }

    @Override
    public void update(float tpf) {
        //TODO: moving effects
        for (Iterator<Entry<Long, EmitterData>> it = liveEmitters.entrySet().iterator(); it.hasNext();) {
            Entry<Long, ClientEffectsManager.EmitterData> entry = it.next();
            EmitterData data = entry.getValue();
            if (data.curTime >= data.time) {
                stopEffect(data.id);
                emitters.get(data.effectName).add(data.emit);
                it.remove();
            }
            data.curTime += tpf;
        }
    }
}

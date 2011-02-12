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
package com.jme3.monkeyzone.ai;

import com.jme3.math.Vector3f;
import com.jme3.monkeyzone.WorldManager;
import com.jme3.monkeyzone.controls.CommandControl;
import com.jme3.scene.Spatial;

/**
 *
 * @author normenhansen
 */
public abstract class AbstractCommand implements Command {

    protected int priority = -1;
    protected long playerId = -1;
    protected long entityId = -1;
    protected Spatial entity = null;
    protected long targetPlayerId = -1;
    protected long targetEntityId = -1;
    protected Spatial targetEntity = null;
    protected Vector3f targetLocation = new Vector3f();
    private boolean running = false;
    protected WorldManager world;

    public abstract boolean doCommand(float tpf);

    public Command initialize(WorldManager world, long playerId, long entityId, Spatial spat) {
        this.world = world;
        this.playerId = playerId;
        this.entityId = entityId;
        this.entity = spat;
        return this;
    }

    public boolean setTargetEntity(Spatial spat) {
        this.targetPlayerId = (Long) spat.getUserData("player_id");
        this.targetEntityId = (Long) spat.getUserData("entity_id");
        this.targetEntity = spat;
        targetLocation.set(spat.getWorldTranslation());
        return true;
    }

    public boolean setTargetLocation(Vector3f location) {
        targetLocation.set(location);
        return true;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void cancel() {
        entity.getControl(CommandControl.class).removeCommand(this);
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }
}

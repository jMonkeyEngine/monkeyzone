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

import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.control.GhostControl;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.monkeyzone.WorldManager;
import com.jme3.monkeyzone.controls.CommandControl;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * Sphere trigger attached to every autonomous entity, contains command that is
 * executed when entity enters and command!=null.
 * @author normenhansen
 */
public class SphereTrigger implements TriggerControl {

    protected Spatial spatial;
    protected boolean enabled = true;
    protected GhostControl ghostControl;
    protected float checkTimer = 0;
    protected float checkTime = 1;
    protected WorldManager world;
    protected CommandControl queueControl;
    protected Command command;

    public SphereTrigger(WorldManager world) {
        this.world = world;
    }

    public SphereTrigger(WorldManager world, Command command) {
        this.world = world;
        this.command = command;
    }

    public void setCommand(Command command) {
        this.command = command;
    }

    public GhostControl getGhost() {
        return ghostControl;
    }

    public void setGhostRadius(float radius) {
        ghostControl.setCollisionShape(new SphereCollisionShape(radius));
    }

    public void setCheckTime(float checkTime) {
        this.checkTime = checkTime;
    }

    public void update(float tpf) {
        if (!enabled) {
            return;
        }
        checkTimer += tpf;
        if (checkTimer >= checkTime) {
            checkTimer = 0;
            if (command != null && ghostControl.getOverlappingCount() > 0) {
                List<PhysicsCollisionObject> objects = ghostControl.getOverlappingObjects();
                for (Iterator<PhysicsCollisionObject> it = objects.iterator(); it.hasNext();) {
                    PhysicsCollisionObject physicsCollisionObject = it.next();
                    Spatial targetEntity = world.getEntity(physicsCollisionObject);
                    if (targetEntity != null && spatial.getUserData("player_id") != targetEntity.getUserData("player_id")) {
                        Command.TargetResult info = command.setTargetEntity(targetEntity);
                        if (info == Command.TargetResult.Accept || info == Command.TargetResult.AcceptEnemy || info == Command.TargetResult.AcceptFriendly) {
                            if (!command.isRunning()) {
                                queueControl.addCommand(command);
                            }
                            return;
                        }
                    }
                }
            }
        }
    }

    public void setSpatial(Spatial spatial) {
        if (spatial == null) {
            if (this.spatial != null) {
                this.spatial.removeControl(ghostControl);
                world.getPhysicsSpace().remove(ghostControl);
            }
            this.spatial = spatial;
            return;
        }
        this.spatial = spatial;
        if (ghostControl == null) {
            ghostControl = new GhostControl(new SphereCollisionShape(10));
        }
        spatial.addControl(ghostControl);
        world.getPhysicsSpace().add(ghostControl);
        queueControl = spatial.getControl(CommandControl.class);
        if (command != null) {
            queueControl.initializeCommand(command);
        }
        if (queueControl == null) {
            throw new IllegalStateException("Cannot add AI control to spatial without CommandQueueControl");
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void render(RenderManager rm, ViewPort vp) {
    }

    public Control cloneForSpatial(Spatial spatial) {
        throw new UnsupportedOperationException("Not supported.");
    }

    public void write(JmeExporter ex) throws IOException {
        throw new UnsupportedOperationException("Not supported.");
    }

    public void read(JmeImporter im) throws IOException {
        throw new UnsupportedOperationException("Not supported.");
    }
}

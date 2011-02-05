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
package com.jme3.monkeyzone.controls;

import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.control.GhostControl;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.monkeyzone.WorldManager;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author normenhansen
 */
public class SimpleAIControl implements AIControl {

    protected Spatial spatial;
    protected boolean enabled = true;
    protected GhostControl ghostControl;
    protected float checkTimer = 0;
    protected float checkTime = 1;
    protected WorldManager world;
    protected AutonomousControl autoControl;

    public SimpleAIControl(WorldManager world) {
        this.world = world;
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
        autoControl = spatial.getControl(AutonomousControl.class);
        if (autoControl == null) {
            throw new IllegalStateException("Cannot add AI control to spatial without AutonomousControl");
        }
        if (ghostControl == null) {
            ghostControl = new GhostControl(new SphereCollisionShape(10));
        }
        spatial.addControl(ghostControl);
        world.getPhysicsSpace().add(ghostControl);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void update(float tpf) {
        checkTimer += tpf;
        if (checkTimer >= checkTime) {
            checkTimer = 0;
//            if (ghostControl.getOverlappingCount() > 0) {
                List<PhysicsCollisionObject> objects = ghostControl.getOverlappingObjects();
                for (Iterator<PhysicsCollisionObject> it = objects.iterator(); it.hasNext();) {
                    PhysicsCollisionObject physicsCollisionObject = it.next();
                    Spatial entity = world.getEntity(physicsCollisionObject);
                    if (entity != null && spatial.getUserData("player_id") != entity.getUserData("player_id")) {
                        if (entity.getUserData("group_id") == spatial.getUserData("group_id")) {
//                            System.out.println("move to " + entity.getWorldTranslation());
                            autoControl.moveTo(entity.getWorldTranslation());
                            return;
                        }
                    }
                }
//            }
        }
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

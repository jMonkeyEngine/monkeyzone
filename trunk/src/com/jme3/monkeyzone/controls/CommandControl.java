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

import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.monkeyzone.WorldManager;
import com.jme3.monkeyzone.ai.Command;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Handles the command queue of an AI entity.
 * @author normenhansen
 */
public class CommandControl implements Control {

    protected Spatial spatial;
    protected boolean enabled = true;
    protected LinkedList<Command> commands = new LinkedList<Command>();
    protected Command defaultCommand;
    protected long playerId;
    protected long entityId;
    protected WorldManager world;

    public CommandControl(WorldManager world, long playerId, long entityId) {
        this.world = world;
        this.playerId = playerId;
        this.entityId = entityId;
    }

    public Command initializeCommand(Command command) {
        return command.initialize(world, playerId, entityId, spatial);
    }

    public void addCommand(Command command) {
        command.setRunning(true);
        for (int i = 0; i < commands.size(); i++) {
            Command command1 = commands.get(i);
            if (command1.getPriority() < command.getPriority()) {
                commands.add(i, command);
                return;
            }
        }
        commands.add(command);
    }

    public void removeCommand(Command command) {
        command.setRunning(false);
        commands.remove(command);
    }

    public void clearCommands() {
        for (Iterator<Command> it = commands.iterator(); it.hasNext();) {
            Command command = it.next();
            command.setRunning(false);
        }
        commands.clear();
    }

    public void update(float tpf) {
        if (!enabled) {
            return;
        }
        for (Iterator<Command> it = commands.iterator(); it.hasNext();) {
            Command command = it.next();
            //do command and remove if returned true, else stop processing
            Command.State commandState = command.doCommand(tpf);
            switch (commandState) {
                case Finished:
                    command.setRunning(false);
                    it.remove();
                    break;
                case Blocking:
                    return;
                case Continuing:
                    break;
            }
        }
    }

    public void setSpatial(Spatial spatial) {
        if (spatial == null) {
            if (this.spatial != null) {
            }
            this.spatial = spatial;
            return;
        }
        this.spatial = spatial;
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

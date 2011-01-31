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
import com.jme3.math.Vector3f;
import com.jme3.monkeyzone.messages.ClientActionMessage;
import com.jme3.network.connection.Client;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract Autonomous Control, handles sending to server when client is set
 * @author normenhansen
 */
public abstract class NetworkedAutonomousControl implements AutonomousControl {

    private Client client;
    private long entity_id;
    protected boolean enabled = true;

    public NetworkedAutonomousControl() {
    }

    public NetworkedAutonomousControl(Client client, long entity_id) {
        this.client = client;
        this.entity_id = entity_id;
    }

    public boolean aimAt(Vector3f direction) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void doAction(int action, boolean activate) {
        if (client != null) {
            try {
                client.send(new ClientActionMessage(entity_id, action, activate));
            } catch (IOException ex) {
                Logger.getLogger(NetworkedAutonomousControl.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public boolean moveTo(Vector3f location) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isMoving() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Vector3f getTargetLocation() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Vector3f getCurrentLocation() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Vector3f getAimDirection() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setSpatial(Spatial spatial) {
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
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

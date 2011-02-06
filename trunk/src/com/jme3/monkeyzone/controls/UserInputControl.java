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
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.monkeyzone.messages.ActionMessage;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import java.io.IOException;

/**
 * When attached to a Spatial, searches for ManualControl and sends user
 * input there, only used on client for current user entity.
 * @author normenhansen
 */
public class UserInputControl implements Control, ActionListener {
    //TODO: add support for joysticks, mouse axis etc. and localization

    private InputManager inputManager;
    private Spatial spatial = null;
    private ManualControl manualControl = null;
    private boolean enabled = true;
    private float aimX = 0;
    private float aimY = 0;
    private float moveX = 0;
    private float moveY = 0;
    private float moveZ = 0;
    private float steerX = 0;
    private Camera cam;
    private Vector3f vectorA = new Vector3f();
    private Vector3f vectorB = new Vector3f();
    private Vector3f elimY = new Vector3f(1, 0, 1);

    public UserInputControl(InputManager inputManager, Camera cam) {
        this.inputManager = inputManager;
        this.cam = cam;
        prepareInputManager();
    }

    private void prepareInputManager() {
        inputManager.addMapping("UserInput_Left_Key", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("UserInput_Right_Key", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("UserInput_Up_Key", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("UserInput_Down_Key", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("UserInput_Left_Arrow_Key", new KeyTrigger(KeyInput.KEY_LEFT));
        inputManager.addMapping("UserInput_Right_Arrow_Key", new KeyTrigger(KeyInput.KEY_RIGHT));
        inputManager.addMapping("UserInput_Space_Key", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("UserInput_Enter_Key", new KeyTrigger(KeyInput.KEY_RETURN));
        inputManager.addMapping("UserInput_Left_Mouse", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(this,
                "UserInput_Left_Key",
                "UserInput_Right_Key",
                "UserInput_Up_Key",
                "UserInput_Down_Key",
                "UserInput_Left_Arrow_Key",
                "UserInput_Right_Arrow_Key",
                "UserInput_Space_Key",
                "UserInput_Enter_Key",
                "UserInput_Left_Mouse");
    }

    public void setSpatial(Spatial spatial) {
        this.spatial = spatial;
        if (spatial == null) {
            manualControl = null;
            return;
        }
        manualControl = spatial.getControl(ManualControl.class);
        if (manualControl == null) {
            throw new IllegalStateException("Cannot add UserInputControl to spatial without ManualControl!");
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void update(float tpf) {
        //TODO: add switch to enable/disable rotate with cam
        vectorA.set(cam.getLeft()).multLocal(elimY).multLocal(-1);
        vectorB.set(manualControl.getAimDirection()).multLocal(elimY);
        float angle = FastMath.HALF_PI - vectorA.angleBetween(vectorB);
        if (angle > 0.1f) {
            manualControl.steerX(1);
        } else if (angle < -0.1f) {
            manualControl.steerX(-1);
        } else {
            manualControl.steerX(0);
        }
    }

    public void render(RenderManager rm, ViewPort vp) {
    }

    public void onAction(String binding, boolean value, float tpf) {
        if (!isEnabled() || manualControl == null) {
            return;
        }
        if (binding.equals("UserInput_Left_Arrow_Key")) {
            if (value) {
                steerX += 1;
                manualControl.steerX(steerX);
            } else {
                steerX -= 1;
                manualControl.steerX(steerX);
            }
        } else if (binding.equals("UserInput_Right_Arrow_Key")) {
            if (value) {
                steerX -= 1;
                manualControl.steerX(steerX);
            } else {
                steerX += 1;
                manualControl.steerX(steerX);
            }
        } else if (binding.equals("UserInput_Left_Key")) {
            if (value) {
                moveX += 1;
                manualControl.moveX(moveX);
            } else {
                moveX -= 1;
                manualControl.moveX(moveX);
            }
        } else if (binding.equals("UserInput_Right_Key")) {
            if (value) {
                moveX -= 1;
                manualControl.moveX(moveX);
            } else {
                moveX += 1;
                manualControl.moveX(moveX);
            }
        } else if (binding.equals("UserInput_Up_Key")) {
            if (value) {
                moveZ += 1;
                manualControl.moveZ(moveZ);
            } else {
                moveZ -= 1;
                manualControl.moveZ(moveZ);
            }
        } else if (binding.equals("UserInput_Down_Key")) {
            if (value) {
                moveZ -= 1;
                manualControl.moveZ(moveZ);
            } else {
                moveZ += 1;
                manualControl.moveZ(moveZ);
            }
        } else if (binding.equals("UserInput_Space_Key")) {
            manualControl.performAction(ActionMessage.JUMP_ACTION, value);
        } else if (binding.equals("UserInput_Enter_Key")) {
            manualControl.performAction(ActionMessage.ENTER_ACTION, value);
        } else if (binding.equals("UserInput_Left_Mouse")) {
            manualControl.performAction(ActionMessage.SHOOT_ACTION, value);
        }
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

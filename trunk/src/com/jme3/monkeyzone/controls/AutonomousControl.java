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

import com.jme3.math.Vector3f;
import com.jme3.scene.control.Control;

/**
 * Basic interface for autonomous controls, these are used by AI to move entities.
 * When a NavigationControl is available on the spatial, it should be used to navigate.
 * @author normenhansen
 */
public interface AutonomousControl extends Control {

    /**
     * aim at location, return false if not possible (max view range, obstacles)
     * @param direction
     * @return
     */
    public boolean aimAt(Vector3f direction);

    /**
     * do action x, same as button press for human player
     * @param action
     */
    public void doAction(int action, boolean activate);

    /**
     * move to location by means of this control, should use NavigationControl
     * if available
     * @param location
     * @return false if already at location, uses radius from NavigationControl if it exists
     */
    public boolean moveTo(Vector3f location);

    /**
     * checks if this entity is moving
     * @return
     */
    public boolean isMoving();

    /**
     * gets the current target location of this entity
     * @return
     */
    public Vector3f getTargetLocation();

    /**
     * gets the current location of this entity
     * @return
     */
    public Vector3f getCurrentLocation();

    /**
     * gets the aim direction of this entity
     * @return
     */
    public Vector3f getAimDirection();

}

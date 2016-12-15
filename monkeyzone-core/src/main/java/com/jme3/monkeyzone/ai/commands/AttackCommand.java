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
package com.jme3.monkeyzone.ai.commands;

import com.jme3.bullet.control.PhysicsControl;
import com.jme3.math.Vector3f;
import com.jme3.monkeyzone.ai.AbstractCommand;
import com.jme3.monkeyzone.ai.SphereTrigger;
import com.jme3.monkeyzone.controls.AutonomousControl;
import com.jme3.monkeyzone.messages.ActionMessage;
import com.jme3.scene.Spatial;

/**
 *
 * @author normenhansen
 */
public class AttackCommand extends AbstractCommand {

    private float timer = 0;
    private float attackTime = .5f;

    @Override
    public TargetResult setTargetLocation(Vector3f location) {
        return TargetResult.Deny;
    }

    @Override
    public TargetResult setTargetEntity(Spatial spatial) {
        int groupId = (Integer) spatial.getUserData("group_id");
        if (groupId != -1 && groupId != entity.getUserData("group_id")) {
            return super.setTargetEntity(spatial);
        }
        return TargetResult.Deny;
    }

    @Override
    public State doCommand(float tpf) {
        timer += tpf;
        if (timer >= attackTime) {
            timer = 0;
            //check if still in range
            if (entity.getControl(SphereTrigger.class).getGhost().getOverlappingObjects().contains(targetEntity.getControl(PhysicsControl.class))) {
                entity.getControl(AutonomousControl.class).moveTo(targetEntity.getWorldTranslation());
                entity.getControl(AutonomousControl.class).performAction(ActionMessage.SHOOT_ACTION, true);
            } else {
                return State.Finished;
            }
        }
        Float targetHP = (Float) targetEntity.getUserData("HitPoints");
        if (targetHP != null && targetHP < 0) {
            return State.Finished;
        }
        return State.Blocking;
    }

    public String getName() {
        return "Attack";
    }
}

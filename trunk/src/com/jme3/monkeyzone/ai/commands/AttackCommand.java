/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
    public boolean setTargetLocation(Vector3f location) {
        return false;
    }

    @Override
    public boolean setTargetEntity(Spatial spatial) {
        if (spatial.getUserData("group_id") != entity.getUserData("group_id")) {
            return super.setTargetEntity(spatial);
        }
        return false;
    }

    @Override
    public boolean doCommand(float tpf) {
        timer += tpf;
        if (timer >= attackTime) {
            timer = 0;
            //check if still in range
            if (entity.getControl(SphereTrigger.class).getGhost().getOverlappingObjects().contains(targetEntity.getControl(PhysicsControl.class))) {
                entity.getControl(AutonomousControl.class).moveTo(targetEntity.getWorldTranslation());
                entity.getControl(AutonomousControl.class).performAction(ActionMessage.SHOOT_ACTION, true);
            } else {
                return true;
            }
        }
        Float targetHP = (Float) targetEntity.getUserData("HitPoints");
        if (targetHP != null && targetHP < 0) {
            return true;
        }
        return false;
    }

    public String getName() {
        return "Attack";
    }
}

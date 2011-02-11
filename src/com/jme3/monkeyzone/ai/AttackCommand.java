/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.monkeyzone.ai;

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
    public boolean setTargetEntity(long playerId, long entityId, Spatial spatial) {
        if (spatial.getUserData("group_id") != entity.getUserData("group_id")) {
            return super.setTargetEntity(playerId, entityId, spatial);
        }
        return false;
    }

    @Override
    public boolean doCommand(float tpf) {
        timer += tpf;
        if (timer >= attackTime) {
            entity.getControl(AutonomousControl.class).moveTo(targetEntity.getWorldTranslation());
            entity.getControl(AutonomousControl.class).performAction(ActionMessage.SHOOT_ACTION, true);
            timer = 0;
        }
        Float targetHP = (Float) targetEntity.getUserData("HitPoints");
        if (targetHP != null && targetHP < 0) {
            return true;
        }
        return false;
    }
}

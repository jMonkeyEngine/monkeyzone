/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.monkeyzone.ai;

import com.jme3.monkeyzone.controls.AutonomousControl;
import com.jme3.scene.Spatial;

/**
 *
 * @author normenhansen
 */
public class FollowCommand extends AbstractCommand {

    float timer = 0;
    float updateTime = 0.25f;

    @Override
    public boolean setTargetEntity(long playerId, long entityId, Spatial spatial) {
        return super.setTargetEntity(playerId, entityId, spatial);
    }

    @Override
    public boolean doCommand(float tpf) {
        timer += tpf;
        if (timer > updateTime) {
            entity.getControl(AutonomousControl.class).moveTo(targetLocation);
            timer = 0;
        }
        return entity.getControl(AutonomousControl.class).isMoving();
    }
}

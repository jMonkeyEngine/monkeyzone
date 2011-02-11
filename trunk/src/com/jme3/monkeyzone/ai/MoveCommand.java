/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.monkeyzone.ai;

import com.jme3.monkeyzone.controls.AutonomousControl;

/**
 *
 * @author normenhansen
 */
public class MoveCommand extends AbstractCommand {

    @Override
    public boolean doCommand(float tpf) {
        entity.getControl(AutonomousControl.class).moveTo(targetLocation);
        if (!entity.getControl(AutonomousControl.class).isMoving()) {
            setRunning(false);
            return true;
        }
        return false;
    }
}

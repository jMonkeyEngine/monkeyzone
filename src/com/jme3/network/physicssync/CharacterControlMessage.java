/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.network.physicssync;

import com.jme3.bullet.control.CharacterControl;
import com.jme3.math.Vector3f;
import com.jme3.network.serializing.Serializable;

/**
 *
 * @author normenhansen
 */
@Serializable()
public class CharacterControlMessage extends PhysicsSyncMessage {

    public Vector3f walkDirection = new Vector3f();
    public Vector3f viewDirection = new Vector3f();

    public CharacterControlMessage(CharacterControl character) {
        this.walkDirection.set(character.getWalkDirection());
        this.viewDirection.set(character.getViewDirection());
    }

    public CharacterControlMessage() {
    }

    public void applyData(Object control){
        ((CharacterControl)control).setWalkDirection(walkDirection);
        ((CharacterControl)control).setViewDirection(viewDirection);
    }
}

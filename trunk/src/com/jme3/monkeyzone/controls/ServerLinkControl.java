/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.monkeyzone.controls;

import com.jme3.network.connection.Server;
import com.jme3.network.physicssync.PhysicsSyncManager;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.control.Control;

/**
 *
 * @author normenhansen
 */
public class ServerLinkControl extends AbstractControl {

    PhysicsSyncManager server;

    public ServerLinkControl(PhysicsSyncManager server) {
        this.server = server;
    }

    public PhysicsSyncManager getSyncManager(){
        return server;
    }

    @Override
    protected void controlUpdate(float tpf) {
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }

    public Control cloneForSpatial(Spatial spatial) {
        throw new UnsupportedOperationException("Not supported.");
    }
}

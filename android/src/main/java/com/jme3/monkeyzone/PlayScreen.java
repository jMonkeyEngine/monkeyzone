package com.jme3.monkeyzone;

import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;

public class PlayScreen {
	
	public static final int BUTTON_COUNT = 4;
	
	private Geometry[] buttonGeometries;
	
	public PlayScreen() {
		buttonGeometries = new Geometry[BUTTON_COUNT];
		Quad buttonQuad = new Quad(1, 1);
		for(int index = 0; index < BUTTON_COUNT; index++) {
			buttonGeometries[index] = new Geometry(String.format("Button%d", index), buttonQuad);
		}
	}
	
	
}

package com.jme3.monkeyzone;
import com.jme3.scene.Spatial;

public interface BuildingPart {
	
	Spatial getSpatial();
	
	boolean isStandAlone();
	
}

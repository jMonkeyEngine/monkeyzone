package com.jme3.monkeyzone;

import com.jme3.app.SimpleApplication;
import com.jme3.input.controls.TouchListener;
import com.jme3.math.Vector2f;
import com.jme3.scene.Node;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.math.Vector3f;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.scene.Geometry;
import com.jme3.material.Material;
import com.jme3.scene.shape.Box;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.CameraNode;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.input.event.TouchEvent;
import com.jme3.math.Quaternion;
import com.jme3.math.FastMath;
import com.jme3.input.controls.TouchTrigger;
import com.jme3.input.TouchInput;
import com.jme3.scene.Spatial;
import com.jme3.util.SkyFactory;
import com.jme3.texture.Texture;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.terrain.heightmap.ImageBasedHeightMap;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.geomipmap.TerrainLodControl;
import com.jme3.terrain.heightmap.HeightMap;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.shader.VarType;
import com.jme3.terrain.Terrain;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import java.nio.FloatBuffer;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import com.jme3.math.Ray;
import com.jme3.collision.CollisionResults;
import com.jme3.collision.CollisionResult;
import com.jme3.bullet.control.GhostControl;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import java.util.List;
import com.jme3.bullet.collision.PhysicsRayTestResult;
import com.jme3.bullet.PhysicsTickListener;
import de.lessvoid.nifty.screen.ScreenController;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.screen.Screen;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import de.lessvoid.nifty.controls.Console;
import java.util.logging.ErrorManager;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;
import de.lessvoid.nifty.tools.Color;
import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import com.jme3.water.SimpleWaterProcessor;
import com.jme3.math.Plane;
import com.jme3.scene.shape.Quad;

import com.jme3.texture.Image.Format;
import com.jme3.light.DirectionalLight;
import com.jme3.asset.AssetManager;
import java.util.concurrent.Callable;

public class MainApplication extends SimpleApplication implements ScreenController, TouchListener, PhysicsCollisionListener {
	public static final Logger LOGGER = Logger.getLogger("MonkeyZone.Logger");
	
	private static final String INPUT_MAPPING_TOUCH = "JMonkeyFPS.Touch";
	private static final int NO_POINTER_ID = -1;
	
	private int widthDp = 470;
	private int heightDp = 320;
	
	private int movePointerId = NO_POINTER_ID;
	private int viewPointerId = NO_POINTER_ID;
	
	private final Vector2f moveOrigin = new Vector2f();
	private final Vector2f viewOrigin = new Vector2f();
	
	private float moveTime;
	private float viewTime;
	
	private BulletAppState bulletState;
	private PhysicsSpace physicsSpace;
	
	private Node player;
	private Node playerCamera;
	private Node playerGun;
	private CharacterControl playerControl;
	private FirstPersonCharacter firstPlayerControl;
	float horizontal, vertical;
	
	private TerrainQuad terrain;
	
	private final Ray targetRay = new Ray();
	private final CollisionResults targetResults = new CollisionResults();
	private final Vector3f groundTarget = new Vector3f();
	private final Vector3f groundNormal = new Vector3f();
	
	private Spatial marker;
	private final Vector3f markerLocation = new Vector3f();
	private GhostControl markerControl;
	private Material markerRed;
	private Material markerGreen;
	
	Nifty nifty;
	private Console console;
	
	boolean canTouch = true;
	boolean canClick = true;
	
	public void buttonClick() {
		LOGGER.log(Level.INFO, "click");
		canTouch = false;
		viewPointerId = NO_POINTER_ID;
	}
	
	public void buttonRelease() {
		LOGGER.log(Level.INFO, "release");
		canTouch = true;
	}
	
	@Override
	public void simpleInitApp() {
		flyCam.setEnabled(false);
		cam.setLocation(Vector3f.ZERO);
		cam.lookAtDirection(Vector3f.UNIT_Z, Vector3f.UNIT_Y);
		
		NiftyJmeDisplay display = new NiftyJmeDisplay(assetManager, inputManager, audioRenderer, guiViewPort);
		nifty = display.getNifty();
		nifty.enableAutoScaling(800, 450);
		nifty.fromXml("Interface/Screens.xml", "screen_game", this);
		guiViewPort.addProcessor(display);
		
		bulletState = new BulletAppState();
		getStateManager().attach(bulletState);
		physicsSpace = bulletState.getPhysicsSpace();
		
		Spatial level = createLevel();
		rootNode.attachChild(level);
		physicsSpace.addAll(level);
		
		player = new Node("Player");
		playerCamera = new CameraNode("Head", getCamera());
		playerCamera.setLocalTranslation(0.0f, 1.0f, 0.0f);
		player.attachChild(playerCamera);
		playerGun = new Node("Gun");
		playerGun.setLocalTranslation(0.0f, 0.0f, getCamera().getFrustumNear());
		playerCamera.attachChild(playerGun);
		playerControl = new CharacterControl(new CapsuleCollisionShape(0.5f, 2.0f), 0.5f);
		player.addControl(playerControl);
		rootNode.attachChild(player);
		physicsSpace.addAll(player);
		
		playerControl.setPhysicsLocation(new Vector3f(0, 300, 0));
		
		Geometry gun = new Geometry("Gun", new Box(0.125f, 0.25f, 0.25f));
		gun.setLocalTranslation(0.0f, -0.5f, 0.75f);
		Material gunMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		gunMaterial.setColor("Color", new ColorRGBA(0.1f, 0.1f, 0.1f, 1.0f));
		gun.setMaterial(gunMaterial);
		playerGun.attachChild(gun);
		
		DirectionalLight sun = new DirectionalLight();
		sun.setDirection(new Vector3f(Vector3f.UNIT_XYZ).negateLocal());
		rootNode.addLight(sun);
		
		firstPlayerControl = new FirstPersonCharacter();
		player.addControl(firstPlayerControl);
		
		getInputManager().addMapping(INPUT_MAPPING_TOUCH, new TouchTrigger(TouchInput.ALL));
		getInputManager().addListener(this, INPUT_MAPPING_TOUCH);
		
		marker = new Geometry("Marker", new Box(2, 1, 2));
		markerControl = new GhostControl(new BoxCollisionShape(new Vector3f(2, 1, 2)));
		marker.addControl(markerControl);
		physicsSpace.add(markerControl);
		markerRed = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		markerRed.setColor("Color", ColorRGBA.Red);
		marker.setMaterial(markerRed);
		rootNode.attachChild(marker);
		
		markerGreen = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		markerGreen.setColor("Color", ColorRGBA.Green);
		physicsSpace.addCollisionListener(this);
	}
	
	@Override
	public void simpleUpdate(float time) {
		targetRay.setOrigin(cam.getLocation());
		targetRay.setDirection(cam.getDirection());

		targetResults.clear();
		terrain.collideWith(targetRay, targetResults);
		if(targetResults.size() > 0) {
			CollisionResult result = targetResults.getClosestCollision();
			float distance = result.getDistance();
			if(distance <= 40) {
				Vector3f hit = result.getContactPoint();
				hit.setX(Math.round(hit.getX() / 4) * 4);
				hit.setZ(Math.round(hit.getZ() / 4) * 4);
				hit.setY(terrain.getHeight(new Vector2f(hit.getX(), hit.getZ())));

				if(!hit.equals(markerLocation)) {
					markerLocation.set(hit);
					marker.setLocalTranslation(markerLocation);
					marker.setMaterial(markerGreen);
				}
			}
		}
	}

	@Override
	public void bind(Nifty nifty, Screen screen) {
	}

	@Override
	public void onStartScreen() {
		// TODO: Implement this method
	}

	@Override
	public void onEndScreen() {
		// TODO: Implement this method
	}
	
	public void placeBuilding() {
		viewPointerId = NO_POINTER_ID;
		enqueue(new Callable<Void>() {
			public Void call() {
				Spatial worldMarker = marker.clone();
				worldMarker.setLocalTransform(marker.getWorldTransform());
				rootNode.attachChild(worldMarker);
				return null;
			}
		});
	}
	
	@Override
	public void collision(PhysicsCollisionEvent event) {
		Spatial nodeA = event.getNodeA();
		Spatial nodeB = event.getNodeB();
		if(nodeA.equals(marker) || nodeB.equals(marker)) {
			marker.setMaterial(markerRed);
		}
	}
	
	private boolean onTouchDown(int pointerId, float x, float y) {
		if(x < getCamera().getWidth() / 2) {
			if(movePointerId == NO_POINTER_ID) {
				movePointerId = pointerId;
				moveOrigin.set(x, y);
				return true;
			}
		} else {
			if(viewPointerId == NO_POINTER_ID) {
				viewPointerId = pointerId;
				viewOrigin.set(x, y);
				return true;
			}
		}
		return false;
	}

	@Override
	public void onTouch(String mapping, TouchEvent event, float time) {
		if(INPUT_MAPPING_TOUCH.equals(mapping)) {
			switch(event.getType()) {
				case DOWN:
					if(onTouchDown(event.getPointerId(), event.getX(), event.getY())) {
						event.setConsumed();
					}
					break;
				case MOVE:
					if(onTouchMove(event.getPointerId(), event.getX(), event.getY(), event.getDeltaX(), event.getDeltaY(), time)) {
						event.setConsumed();
					}
					break;
				case UP:
					if(onTouchUp(event.getPointerId(), event.getX(), event.getY(), time)) {
						event.setConsumed();
					}
					break;
			}
		}
	}
	
	private boolean onTouchMove(int pointerId, float x, float y, float dx, float dy, float time) {
		if(movePointerId == pointerId) {
			
			horizontal = -((x - moveOrigin.x) / (cam.getWidth() / 6));
			if(horizontal > -0.1 && horizontal < 0.1) horizontal = 0;
			//if(horizontal < -1 || horizontal > 1) horizontal = 1;
			
			vertical = (y - moveOrigin.y) / (cam.getWidth() / 6);
			if(vertical > -0.1 && vertical < 0.1) vertical = 0;
			//vertical = Math.max(-1.0f, Math.min(1.0f, vertical));
			firstPlayerControl.setMove(horizontal, vertical);
			return !(horizontal == 0 && vertical == 0);
		}
		if(viewPointerId == pointerId) {
			viewTime = 1;
			if(!canTouch) return false;
			if(dy < -(cam.getHeight() * 0.4)) {
				playerControl.jump();
				Logger.getAnonymousLogger().log(Level.INFO, "jump");
			} else {
				float horizontal = (x - viewOrigin.x) / (cam.getWidth() / 2);
				float vertical = -(y - viewOrigin.y) / cam.getWidth();
				firstPlayerControl.setLook(horizontal, vertical);
			}
			viewOrigin.set(x, y);
			return true;
		}
		return false;
	}
	
	private boolean onTouchUp(int pointerId, float x, float y, float time) {
		if(movePointerId == pointerId) {
			movePointerId = NO_POINTER_ID;
			horizontal = vertical = 0;
			firstPlayerControl.setMove(0, 0);
			return true;
		}
		if(viewPointerId == pointerId) {
			viewTime = 0;
			viewPointerId = NO_POINTER_ID;
			return true;
		}
		return false;
	}
	
	public Spatial createLevel() {
		Node level = new Node("Level");
		
		Texture east = assetManager.loadTexture("Textures/Skybox/East.png");
		Texture west = assetManager.loadTexture("Textures/Skybox/West.png");
		Texture north = assetManager.loadTexture("Textures/Skybox/North.png");
		Texture south = assetManager.loadTexture("Textures/Skybox/South.png");
		Texture top = assetManager.loadTexture("Textures/Skybox/Top.png");
		Texture bottom = assetManager.loadTexture("Textures/Skybox/Bottom.png");
		
		Spatial sky = SkyFactory.createSky(assetManager, west, east, north, south, top, bottom);
		sky.setName("Sky");
		level.attachChild(sky);
		/*
		Texture grass = assetManager.loadTexture("Textures/Terrain/Grass.png");
		grass.setWrap(Texture.WrapMode.Repeat);
		
		Texture rock = assetManager.loadTexture("Textures/Terrain/Rock.png");
		rock.setWrap(Texture.WrapMode.Repeat);
		
		Texture sand = assetManager.loadTexture("Textures/Terrain/Sand.png");
		sand.setWrap(Texture.WrapMode.Repeat);
		*/
		//float[] heightData = TerrainTools.generateNoiseImage(512);
		
		Material material = createTerrainMaterial(assetManager, "Textures/Terrain/AlphaMap.png", "Textures/Terrain/Grass.png", 24, "Textures/Terrain/Sand.png", 32, "Textures/Terrain/Rock.png", 16);
		/*
		Material material = new Material(assetManager, "Shaders/Terrain.j3md");
		Texture alpha = assetManager.loadTexture("Textures/Terrain/AlphaMap.png");
		//new Texture2D(TerrainTools.noiseToAlphaMap(512, heightData));
		//material.setTexture("ColorMap", alpha);
		//material.setFloat("Time", 0);
		
		material.setTexture("SplatMap", alpha);
		material.setTexture("ColorMapR", grass);
		material.setFloat("ScaleR", 24f);
		material.setTexture("ColorMapG", sand);
		material.setFloat("ScaleG", 32f);
		material.setTexture("ColorMapB", rock);
		material.setFloat("ScaleB", 16f);
		
		Image heights = assetManager.loadTexture("Textures/Terrain/HeightMap.png").getImage();
		
		AbstractHeightMap heightmap = new ImageBasedHeightMap(heights);
		heightmap.load();
		//heightmap.smooth(0.6f);
		heightmap.normalizeTerrain(100);
		heightmap.erodeTerrain();
		*/
		HeightMap heightmap = createTerrainHeightMap(assetManager, "Textures/Terrain/HeightMap.png", -50, 50);
		//heightmap.erodeTerrain();
		//fixHeightMap(heightmap, 0);
		terrain = new TerrainQuad("Terrain", 127, 513, heightmap.getHeightMap());
		
		terrain.setMaterial(material);
		//terrain.setLocalTranslation(0, -100, 0);
		terrain.setLocalScale(2f, 1f, 2f);
		level.attachChild(terrain);
		
		terrain.addControl(new TerrainLodControl(terrain, getCamera()));
		terrain.addControl(new RigidBodyControl(0));
		
		return level; 
	}
	
	public static HeightMap createTerrainHeightMap(AssetManager assetManager, String heightMap, float minHeight, float maxHeight) {
		Image heightMapImage = assetManager.loadTexture(heightMap).getImage();
		AbstractHeightMap abstractHeightMap = new ImageBasedHeightMap(heightMapImage);
		abstractHeightMap.load();
		abstractHeightMap.normalizeTerrain(maxHeight - minHeight);
		for(int y = 0; y < abstractHeightMap.getSize(); y++) {
			for(int x = 0; x < abstractHeightMap.getSize(); x++) {
				float height = abstractHeightMap.getTrueHeightAtPoint(x, y);
				abstractHeightMap.setHeightAtPoint(height + minHeight, x, y);
			}
		}
		abstractHeightMap.erodeTerrain();
		return abstractHeightMap;
	}
	
	public static Material createTerrainMaterial(AssetManager assetManager, String alpha, String tex1, float scale1, String tex2, float scale2, String tex3, float scale3) {
		Texture alphaTexture = assetManager.loadTexture(alpha);
		alphaTexture.setMagFilter(Texture.MagFilter.Bilinear);
		alphaTexture.setMinFilter(Texture.MinFilter.Trilinear);
		
		Texture tex1Texture = assetManager.loadTexture(tex1);
		tex1Texture.setMagFilter(Texture.MagFilter.Bilinear);
		tex1Texture.setMinFilter(Texture.MinFilter.Trilinear);
		tex1Texture.setWrap(Texture.WrapMode.Repeat);
		
		Texture tex2Texture = assetManager.loadTexture(tex2);
		tex2Texture.setMagFilter(Texture.MagFilter.Bilinear);
		tex2Texture.setMinFilter(Texture.MinFilter.Trilinear);
		tex2Texture.setWrap(Texture.WrapMode.Repeat);
		
		Texture tex3Texture = assetManager.loadTexture(tex3);
		tex3Texture.setMagFilter(Texture.MagFilter.Bilinear);
		tex3Texture.setMinFilter(Texture.MinFilter.Trilinear);
		tex3Texture.setWrap(Texture.WrapMode.Repeat);
		
		Material material = new Material(assetManager, "Common/MatDefs/Terrain/Terrain.j3md");
		material.setTexture("Alpha", assetManager.loadTexture(alpha));
		material.setTexture("Tex1", tex1Texture);
		material.setFloat("Tex2Scale", scale1);
		material.setTexture("Tex2", tex2Texture);
		material.setFloat("Tex1Scale", scale2);
		material.setTexture("Tex3", tex3Texture);
		material.setFloat("Tex3Scale", scale3);
		return material;
	}
	
}

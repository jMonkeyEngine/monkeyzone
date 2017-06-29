package com.jme3.monkeyzone;

import com.jme3.app.SimpleApplication;
import com.jme3.scene.shape.Sphere;
import com.jme3.scene.Geometry;
import com.jme3.material.Material;
import com.jme3.util.SkyFactory;
import com.jme3.texture.Texture;
import com.jme3.scene.Spatial;
import com.jme3.light.DirectionalLight;
import com.jme3.math.Vector3f;
import com.jme3.math.ColorRGBA;
import com.jme3.post.SceneProcessor;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.texture.FrameBuffer;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Node;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.AnalogListener;
import com.jme3.math.FastMath;
import com.jme3.material.MaterialDef;
import com.jme3.shader.VarType;
import com.jme3.light.PointLight;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture2D;
import com.jme3.texture.Image;
import com.jme3.renderer.Camera;
import com.jme3.texture.TextureArray;
import java.nio.ByteBuffer;
import com.jme3.util.BufferUtils;
import java.util.ArrayList;
import com.jme3.bounding.BoundingBox;
import com.jme3.asset.AssetManager;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.light.Light;
import com.jme3.material.TechniqueDef;
import com.jme3.post.Filter;
import com.jme3.scene.GeometryGroupNode;

public class TestApplication extends SimpleApplication implements AnalogListener {
	
	private Node userNode;
	
	private FrameBuffer normalBuffer;
	private FrameBuffer albedoBuffer;
	
	@Override
	public void simpleInitApp() {
		flyCam.setEnabled(false);
		
		CameraNode cameraNode = new CameraNode("Main Camera", cam);
		cameraNode.setLocalTranslation(0, 5, 15);
		cameraNode.lookAt(new Vector3f(0, 5, 0), Vector3f.UNIT_Y);
		
		userNode = new Node("User Node");
		userNode.attachChild(cameraNode);
		rootNode.attachChild(userNode);
		
		// Create Light
		PointLight lightA = new PointLight();
		lightA.setPosition(new Vector3f(20, 10, 10));
		lightA.setRadius(80);
		rootNode.addLight(lightA);

		PointLight lightB = new PointLight();
		lightB.setPosition(new Vector3f(-20, 10, 10));
		lightB.setRadius(80);
		rootNode.addLight(lightB);
		
		// Create Sky
		Texture east = assetManager.loadTexture("Textures/Skybox/East.png");
		Texture west = assetManager.loadTexture("Textures/Skybox/West.png");
		Texture north = assetManager.loadTexture("Textures/Skybox/North.png");
		Texture south = assetManager.loadTexture("Textures/Skybox/South.png");
		Texture top = assetManager.loadTexture("Textures/Skybox/Top.png");
		Texture bottom = assetManager.loadTexture("Textures/Skybox/Bottom.png");
		
		Spatial sky = SkyFactory.createSky(assetManager, west, east, north, south, top, bottom);
		sky.setName("Sky");
		rootNode.attachChild(sky);
		/*
		Material material = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
		
		material.setBoolean("VertexLighting", false);
		//material.setTexture("DiffuseMap", assetManager.loadTexture("Textures/Grass.Diffuse.png"));
		//material.setTexture("NormalMap", assetManager.loadTexture("Textures/Grass.Normal.png"));
		//material.setTexture("ParallaxMap", assetManager.loadTexture("Textures/Grass.Parallax.png"));
		//material.setTexture("SpecularMap", assetManager.loadTexture("Textures/Grass.Specular.png"));
		material.setColor("Diffuse", ColorRGBA.Red);
		material.setColor("Specular", ColorRGBA.White);
		material.setVector3("FresnelParams", new Vector3f(0.01f, 0.8f, 1.0f));
		material.setFloat("Shininess", 16.0f);
		material.setTextureParam("EnvMap", VarType.TextureCubeMap, ((Geometry)sky).getMaterial().getTextureParam("Texture").getTextureValue());
		//material.setBoolean("HardwareShadows", false);
		//material.setBoolean("BackfaceShadows", true);
		*/
		Spatial ashelin = assetManager.loadModel("Models/AshelinPraxis/Ashelin.obj");
		rootNode.attachChild(ashelin);
		//ashelin.updateGeometricState();
		ashelin.updateModelBound();
		BoundingBox bounds = (BoundingBox) ashelin.getWorldBound();
		float scale = 5f / bounds.getYExtent();
		//ashelin.setMaterial(material);
		ashelin.setLocalScale(scale);
		
		inputManager.addMapping("Test.Positive.AxisX", new MouseAxisTrigger(MouseInput.AXIS_X, false));
		inputManager.addMapping("Test.Negative.AxisX", new MouseAxisTrigger(MouseInput.AXIS_X, true));
		inputManager.addListener(this, "Test.Positive.AxisX", "Test.Negative.AxisX");
		/*
		Texture2D albedoTexture = new Texture2D(bufferCamera.getWidth(), bufferCamera.getHeight(), Image.Format.RGB5A1);
		Texture2D normalTexture = new Texture2D(bufferCamera.getWidth(), bufferCamera.getHeight(), Image.Format.RGB5A1);
		
		Texture2D depthTexture = new Texture2D(bufferCamera.getWidth(), bufferCamera.getHeight(), Image.Format.Depth16);
		Texture2D dataTexture = new Texture2D(bufferCamera.getWidth(), bufferCamera.getHeight(), Image.Format.Depth16);
		
		normalBuffer = new FrameBuffer(bufferCamera.getWidth(), bufferCamera.getHeight(), 0);
		normalBuffer.setColorBuffer(Image.Format.RGB5A1);
		normalBuffer.setDepthBuffer(Image.Format.Depth16);
		normalBuffer.setColorTexture(normalTexture);
		normalBuffer.setDepthTexture(depthTexture);
		
		albedoBuffer = new FrameBuffer(bufferCamera.getWidth(), bufferCamera.getHeight(), 0);
		albedoBuffer.setColorBuffer(Image.Format.RGB5A1);
		albedoBuffer.setDepthBuffer(Image.Format.Depth16);
		albedoBuffer.setColorTexture(albedoTexture);
		albedoBuffer.setDepthTexture(dataTexture);
		*/
	}

	@Override
	public void simpleUpdate(float time) {
		
	}

	@Override
	public void simpleRender(RenderManager renderManager) {
		
	}
	
	
	@Override
	public void onAnalog(String name, float value, float time) {
		switch(name) {
			case "Test.Positive.AxisX":
				userNode.rotate(0, -(value * FastMath.TWO_PI), 0);
				break;
			case "Test.Negative.AxisX":
				userNode.rotate(0, (value * FastMath.TWO_PI), 0);
				break;
		}
	}
	
}

package com.jme3.monkeyzone;
import com.jme3.scene.control.AbstractControl;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.math.Vector3f;
import com.jme3.math.Quaternion;
import com.jme3.scene.CameraNode;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.scene.Spatial;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.scene.Node;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.math.Matrix3f;
import com.jme3.math.Matrix4f;
import com.jme3.bounding.BoundingVolume;
import com.jme3.bounding.BoundingBox;

public class FirstPersonCharacter extends AbstractControl {
	
	public static final float DEFAULT_MAX_HEAD_ANGLE =  55 * FastMath.DEG_TO_RAD;
	public static final float DEFAULT_MIN_HEAD_ANGLE = -70 * FastMath.DEG_TO_RAD;
	
	public static final float DEFAULT_TRAVEL_SPEED = 28 * 0.44704f;
	public static final float DEFAULT_STRAFE_SPEED = 10 * 0.44704f;
	
	private final Quaternion headRotation;
	private final Quaternion bodyRotation;
	
	//private final Vector3f viewDirection;
	//private final Vector3f walkDirection;
	
	private float maxHeadAngle;
	private float minHeadAngle;
	
	private CharacterControl body;
	private CameraNode head;
	
	private float headAngle;
	private float bodyAngle;
	
	private float travelSpeed;
	private float strafeSpeed;
	
	private float travelFactor;
	private float strafeFactor;
	//private float assentFactor;
	
	public FirstPersonCharacter() {
		headRotation = new Quaternion(Quaternion.DIRECTION_Z);
		bodyRotation = new Quaternion(Quaternion.DIRECTION_Z);
		//viewDirection = new Vector3f(Vector3f.UNIT_Z);
		//walkDirection = new Vector3f(Vector3f.ZERO);
		maxHeadAngle = DEFAULT_MAX_HEAD_ANGLE;
		minHeadAngle = DEFAULT_MIN_HEAD_ANGLE;
		travelSpeed = DEFAULT_TRAVEL_SPEED;
		strafeSpeed = DEFAULT_STRAFE_SPEED;
		headAngle = 0.0f;
		bodyAngle = 0.0f;
	}

	@Override
	public void setSpatial(Spatial spatial) {
		super.setSpatial(spatial);
		body = spatial.getControl(CharacterControl.class);
		head = (CameraNode)((Node)spatial).getChild("Head");
	}
	
	@Override
	protected void controlUpdate(float time) {
		bodyRotation.fromAngles(0.0f, bodyAngle, 0.0f);
		
		if(body.onGround() || body.getPhysicsLocation().y < 45) {
			Vector3f walkDirection = body.getWalkDirection();
			walkDirection.setX(strafeFactor * (strafeSpeed * time));
			walkDirection.setZ(travelFactor * (travelSpeed * time));
		
			bodyRotation.multLocal(walkDirection);
			body.setWalkDirection(walkDirection);
		}
		
		Vector3f viewDirection = body.getViewDirection();
		viewDirection.set(Vector3f.UNIT_Z);
		bodyRotation.multLocal(viewDirection);
		body.setViewDirection(viewDirection);
		
		headRotation.fromAngles(headAngle, 0.0f, 0.0f);
		head.setLocalRotation(headRotation);
	}
	
	@Override
	protected void controlRender(RenderManager manager, ViewPort view) {
	}
	
	public void setMove(float strafe, float travel) {
		travelFactor = Math.max(-1.0f, Math.min(1.0f, travel));
		strafeFactor = Math.max(-1.0f, Math.min(1.0f, strafe));
	}
	
	/**
	 * @param body angle to add to body rotation axes-y(pan)
	 * @param head angle to add to head rotation axes-x(tilt)
	 */
	public void setLook(float body, float head) {
		bodyAngle -= (body * FastMath.TWO_PI);
		headAngle = Math.min(maxHeadAngle, Math.max(minHeadAngle, headAngle + (head * FastMath.TWO_PI)));
	}
	
}

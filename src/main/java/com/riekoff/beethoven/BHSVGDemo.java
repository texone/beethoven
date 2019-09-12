package com.riekoff.beethoven;

import java.util.List;

import cc.creativecomputing.app.modules.CCAnimator;
import cc.creativecomputing.core.CCProperty;
import cc.creativecomputing.graphics.CCDrawMode;
import cc.creativecomputing.graphics.CCGraphics;
import cc.creativecomputing.graphics.app.CCGL2Adapter;
import cc.creativecomputing.graphics.app.CCGL2Application;
import cc.creativecomputing.graphics.camera.CCCameraController;
import cc.creativecomputing.io.CCNIOUtil;
import cc.creativecomputing.math.CCVector3;
import cc.creativecomputing.math.spline.CCLinearSpline;
import cc.creativecomputing.model.svg.CCSVGDocument;
import cc.creativecomputing.model.svg.CCSVGIO;

public class BHSVGDemo extends CCGL2Adapter {
	
	
	@CCProperty(name = "camera")
	private CCCameraController _cCameraController;

	public List<CCLinearSpline> contours;
	
	private CCSVGDocument myDocument = CCSVGIO.newSVG(CCNIOUtil.dataPath("Zitat3.svg"));

	@Override
	public void init(CCGraphics g, CCAnimator theAnimator) {
		_cCameraController = new CCCameraController(this, g, 100);
		
		
		contours = myDocument.contours(0.001);
	}

	@Override
	public void update(CCAnimator theAnimator) {
		if(theAnimator.frames() % 30 == 0)CCNIOUtil.saveString(CCNIOUtil.appPath("hearbeat.xml"), "<heartbeat secondsSince1970=\""+System.currentTimeMillis() / 1000 +"\"/>");
	}

	@Override
	public void display(CCGraphics g) {
		_cCameraController.camera().draw(g);
		
		g.clearColor(0);
		g.clear();
		
		g.rect(0,0,100,100);
		
//		for(int i = 0; i < contours.size();i++) {
//			CCLinearSpline  myTextPoints = contours.get(i);
//
//			g.color(1f);
//			g.beginShape(CCDrawMode.POINTS);
//			for(CCVector3 myPoint:myTextPoints) {
//				g.vertex(myPoint);
//			}
//			g.endShape();
//			
//		}
	}

	public static void main(String[] args) {

		BHSVGDemo demo = new BHSVGDemo();

		CCGL2Application myAppManager = new CCGL2Application(demo);
		myAppManager.glcontext().size(1920, 1080);
		myAppManager.animator().framerate = 30;
		myAppManager.animator().animationMode = CCAnimator.CCAnimationMode.FRAMERATE_PRECISE;
		myAppManager.start();
	}
}

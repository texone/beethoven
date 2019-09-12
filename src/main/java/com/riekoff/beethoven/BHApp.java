/*
 * Copyright (c) 2013 christianr.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-3.0.html
 * 
 * Contributors:
 *     christianr - initial API and implementation
 */
package com.riekoff.beethoven;

import java.util.ArrayList;
import java.util.List;

import cc.creativecomputing.app.modules.CCAnimator;
import cc.creativecomputing.controlui.CCControlApp;
import cc.creativecomputing.controlui.timeline.controller.CCTransportController;
import cc.creativecomputing.core.CCProperty;
import cc.creativecomputing.core.logging.CCLog;
import cc.creativecomputing.graphics.CCGraphics;
import cc.creativecomputing.graphics.CCGraphics.CCBlendMode;
import cc.creativecomputing.graphics.app.CCGL2Adapter;
import cc.creativecomputing.graphics.app.CCGL2Application;
import cc.creativecomputing.graphics.camera.CCCameraController;
import cc.creativecomputing.graphics.export.CCScreenCaptureController;
import cc.creativecomputing.graphics.font.CCFontIO;
import cc.creativecomputing.graphics.shader.CCShaderBufferDebugger;
import cc.creativecomputing.io.CCNIOUtil;
import cc.creativecomputing.math.CCVector2;
import cc.creativecomputing.math.CCVector3;
import cc.creativecomputing.realsense.CCRealSenseTextures;
import cc.creativecomputing.simulation.particles.CCParticles;
import cc.creativecomputing.simulation.particles.blends.CCBlend;
import cc.creativecomputing.simulation.particles.blends.CCConstantBlend;
import cc.creativecomputing.simulation.particles.emit.CCParticlesIndexParticleEmitter;
import cc.creativecomputing.simulation.particles.forces.CCAttractor;
import cc.creativecomputing.simulation.particles.forces.CCForce;
import cc.creativecomputing.simulation.particles.forces.CCForceField;
import cc.creativecomputing.simulation.particles.forces.CCGravity;
import cc.creativecomputing.simulation.particles.forces.CCPathTargetForce;
import cc.creativecomputing.simulation.particles.forces.CCTextureForceField2D;
import cc.creativecomputing.simulation.particles.forces.CCViscousDrag;
import cc.creativecomputing.simulation.particles.forces.springs.CCSpringForce;
import cc.creativecomputing.simulation.particles.render.CCParticleRenderer;
import cc.creativecomputing.simulation.particles.render.CCQuadRenderer;
import cc.creativecomputing.simulation.particles.render.CCSpringLineRenderer;
import cc.creativecomputing.simulation.particles.render.CCSpringVolumentricLineRenderer;

public class BHApp extends CCGL2Adapter {

	

	@CCProperty(name = "particles")
	private CCParticles _myParticles;


	@CCProperty(name = "camera")
	private CCCameraController _cCameraController;

	private int _myXres = 700;
	private int _myYres = 700;

	@CCProperty(name = "alpha", min = 0, max = 1)
	private double _cAlpha = 0;

	@CCProperty(name = "debugger")
	private CCShaderBufferDebugger _myDebugger;
	@CCProperty(name = "particle debugger")
	private CCShaderBufferDebugger _myParticleDebugger;
	@CCProperty(name = "screencapture")
	private CCScreenCaptureController _cScreenCaptureController;

	private CCSpringForce _mySprings;
	private CCPathTargetForce _myNoteSheetTargetForce;
	private CCPathTargetForce _myZitatTargetForce;
	private CCTextureForceField2D _myForceField;
	
	@CCProperty(name = "real sense")
	private CCRealSenseTextures _myRealSenseTextures;
	
	@CCProperty(name = "textpath speed", min = -10, max = 10)
	private double _cTextPathSpeed = 0;
	@CCProperty(name = "noise notes speed", min = -10, max = 10)
	private double _cNoiseSpeed = 0;
	@CCProperty(name = "noise notes Amount", min = 0, max = 1)
	private double _cNoiseAmount = 0;
	@CCProperty(name = "document path offset", min = -1, max = 1)
	private double _cDocumentPathOffset = 0;
	@CCProperty(name = "document path scale")
	private double _cDocumentPathScale = 500;
	@CCProperty(name = "min wait Time", min = 0, max = 60)
	private double _cMinWaitTime = 10;
	@CCProperty(name = "speed", min = 1, max = 10)
	private double _cSpeed = 4;

	List<List<CCVector3>> myDebugDocumentSplines = new ArrayList<>();
	
	private BHVectorManager _myManager;
	
	private double _myLoopTime = 0;
	private double _myWaitTime = 1000;
	private boolean _myPlay = false;
	
	@Override
	public void init(CCGraphics g, CCAnimator theAnimator) {
		g.noDebug();
		
		_cScreenCaptureController = new CCScreenCaptureController(this);
		_myRealSenseTextures = new CCRealSenseTextures();
		
		_myManager = new BHVectorManager();
		
		List<CCForce> myForces = new ArrayList<>();
		myForces.add(new CCForceField());
		myForces.add(new CCViscousDrag());
		myForces.add(new CCAttractor());
		myForces.add(_mySprings = _myManager.springs);
		myForces.add(_myNoteSheetTargetForce = _myManager.noteSheetTargetForce);
		myForces.add(_myZitatTargetForce = _myManager.zitatTargetForce);
		myForces.add(_myForceField = new CCTextureForceField2D(_myRealSenseTextures.forceField(), new CCVector2(1920d, -1080d), new CCVector2(0.5, 0.5)));
		myForces.add(new CCGravity());
		
		List<CCBlend> myBlends = new ArrayList<>();
		myBlends.add(new CCConstantBlend());
		myBlends.add(_myManager.textureBlend);

		List<CCParticleRenderer> myRenderer = new ArrayList<>();
		myRenderer.add(new CCSpringVolumentricLineRenderer(_mySprings, false));
		myRenderer.add(new CCSpringLineRenderer(_mySprings));
		myRenderer.add(_myManager.triangleRenderer);
		myRenderer.add(new CCQuadRenderer());

		_myParticles = new CCParticles(g, myRenderer, myForces, myBlends, new ArrayList<>(), new ArrayList<>(), _myXres, _myYres);
		CCParticlesIndexParticleEmitter emitter = new CCParticlesIndexParticleEmitter(_myParticles);
		_myParticles.addEmitter(emitter);

		_cCameraController = new CCCameraController(this, g, 100);

		g.textFont(CCFontIO.createTextureMapFont("arial", 12));
		
		_myManager.setup(g, emitter);
		keyReleased().add(e ->{
			switch(e.keyCode()) {
			case VK_P:
				playLoop();
				break;
			}
		});
	}
	
	public void playLoop() {
		if(_myPlay)return;
		if(_myWaitTime < _cMinWaitTime)return;
		_myPlay = true;
		_myLoopTime = 0;
		CCLog.info("yo");
	}
	
	private void updateLoop(final CCAnimator theAnimator) {
		_myWaitTime += theAnimator.deltaTime() / _myUpdateCycles;;
		if(!_myPlay) return;

		CCTransportController myTransport = timeline().activeTimeline().transportController();
		
		_myLoopTime += theAnimator.deltaTime() / _myUpdateCycles * _cSpeed;
		if(_myLoopTime >= myTransport.loopEnd()) {
			_myPlay = false;
			myTransport.time(0);
			_myWaitTime = 0;
			return;
		}
		
		myTransport.time(_myLoopTime);
	}
	
	@Override
	public void setupControls(CCControlApp theControlApp) {
		timeline().loadProject(CCNIOUtil.dataPath("ablauf.json"));
	}

	private double _myTextOffset = 0;
	private double _myNoiseOffset = 0;
	
	private int _myUpdateCycles = 4;
	
	@CCProperty(name = "write hearbeat")
	private boolean _cWriteHeartBeat = false;

	@Override
	public void update(final CCAnimator theAnimator) {
		_myTextOffset += theAnimator.deltaTime() * _cTextPathSpeed;
		_myNoiseOffset += theAnimator.deltaTime() * _cNoiseSpeed;

		_myRealSenseTextures.update(theAnimator);
		
		if(_myRealSenseTextures.amountInBounds > _cThreshold) {
			playLoop();
		}
		
		for(int i = 0; i < _myUpdateCycles;i++) {
			updateLoop(theAnimator);
			
			_myNoteSheetTargetForce.pathAdd(_cDocumentPathOffset * _cDocumentPathScale);
			_myNoteSheetTargetForce.noiseAdd(_myNoiseOffset);
			_myNoteSheetTargetForce.noiseAmount(_cNoiseAmount);
			_myZitatTargetForce.pathAdd(_myTextOffset);
			_myParticles.update(theAnimator);
		}
		
		if(_cWriteHeartBeat)CCNIOUtil.saveString(CCNIOUtil.appPath("hearbeat.xml"), "<heartbeat secondsSince1970=\""+System.currentTimeMillis() / 1000 +"\"/>");
	}

	@CCProperty(name = "debug force field")
	private boolean _cDebugForceField = true;
	
	@CCProperty(name = "draw particles")
	private boolean _cDrawParticles = true;

	@CCProperty(name = "debug depth")
	private boolean _cDebugDepth = true;
	
	@CCProperty(name = "line alpha", min = 0, max = 1)
	private double _cLineAlpha = 1;

	@CCProperty(name = "threshold", min = 0, max = 1)
	private double _cThreshold = 0.5;

	@Override
	public void display(CCGraphics g) {
		_myRealSenseTextures.preDisplay(g);
		for(int i = 0; i < _myUpdateCycles;i++) {
			_myParticles.preDisplay(g);
		}
		g.clearColor(0,0,0);
		g.clear();
		g.pushMatrix();
		_cCameraController.camera().draw(g);
		
		g.blend();
		if(_cDebugForceField) {
			g.pushMatrix();
			g.color(1d);
			_myForceField.display(g);
			g.popMatrix();
		}

		g.blend(CCBlendMode.ADD);
		
		if(_cDrawParticles) {
			g.color(1f, _cLineAlpha);
			g.image(_myManager.linesTextures, 0,0);
			g.noDepthTest();
			g.color(1f);
			_myParticles.display(g);
		}

		//_myManager.display(g);
		g.popMatrix();
		
		if(_cDebugDepth) {
			g.blend();
			_myRealSenseTextures.drawPointCloud(g);
			g.pushMatrix();
			g.translate(-g.width()/2 + 100, -g.height()/2 + 100);
			g.color(255);

			g.color(255, 50);
			g.rect(0, 0, 1000, 20);
			g.color(255);
			g.rect(0, 0, _myRealSenseTextures.amountInBounds *1000, 20);
			
			g.color(255,0,0);
			g.line(_cThreshold * 1000, 0,0, _cThreshold * 1000, 20, 0);
			g.popMatrix();
		}
		
	}

	public static void main(String[] args) {
		CCGL2Application myAppManager = new CCGL2Application(new BHApp());
		myAppManager.glcontext().size(3840, 2160);
		myAppManager.glcontext().undecorated = true;
		myAppManager.glcontext().windowX = 0;
		myAppManager.glcontext().windowY = 0;
		myAppManager.animator().framerate = 30;
		myAppManager.animator().animationMode = CCAnimator.CCAnimationMode.FRAMERATE_PRECISE;
		myAppManager.start();
	}
}

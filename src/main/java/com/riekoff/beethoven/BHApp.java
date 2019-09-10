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
import cc.creativecomputing.core.CCProperty;
import cc.creativecomputing.graphics.CCGraphics;
import cc.creativecomputing.graphics.CCGraphics.CCBlendMode;
import cc.creativecomputing.graphics.app.CCGL2Adapter;
import cc.creativecomputing.graphics.app.CCGL2Application;
import cc.creativecomputing.graphics.camera.CCCameraController;
import cc.creativecomputing.graphics.export.CCScreenCaptureController;
import cc.creativecomputing.graphics.font.CCFontIO;
import cc.creativecomputing.graphics.shader.CCShaderBufferDebugger;
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

	List<List<CCVector3>> myDebugDocumentSplines = new ArrayList<>();
	
	private BHVectorManager _myManager;
	
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

		g.strokeWeight(0.5f);

//		_myDebugger = new CCShaderBufferDebugger(_mySprings.idBuffer());
//		_myParticleDebugger = new CCShaderBufferDebugger(_myParticles.infoData());

		g.textFont(CCFontIO.createTextureMapFont("arial", 12));
		
		_myManager.setup(g, emitter);
	}

	int myIndex = 0;
	double _myTextOffset = 0;
	
	double _myNoiseOffset = 0;
	
	private int _myUpdateCycles = 4;

	@Override
	public void update(final CCAnimator theAnimator) {
		_myTextOffset += theAnimator.deltaTime() * _cTextPathSpeed;
		_myNoiseOffset += theAnimator.deltaTime() * _cNoiseSpeed;

		_myRealSenseTextures.update(theAnimator);
		
		for(int i = 0; i < _myUpdateCycles;i++) {
			_myNoteSheetTargetForce.pathAdd(_cDocumentPathOffset * _cDocumentPathScale);
			_myNoteSheetTargetForce.noiseAdd(_myNoiseOffset);
			_myNoteSheetTargetForce.noiseAmount(_cNoiseAmount);
			_myZitatTargetForce.pathAdd(_myTextOffset);
			_myParticles.update(theAnimator);
		}
	}

	@CCProperty(name = "debug force field")
	private boolean _cDebugForceField = true;
	
	@CCProperty(name = "draw particles")
	private boolean _cDrawParticles = true;

	@CCProperty(name = "debug depth")
	private boolean _cDebugDepth = true;
	
	@CCProperty(name = "line alpha", min = 0, max = 1)
	private double _cLineAlpha = 1;

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
		g.popMatrix();
		
		if(_cDebugDepth) {
			g.pushMatrix();
			g.color(255);
			_myRealSenseTextures.drawPointCloud(g);

			g.color(255, 50);
			g.rect(-g.width()/2 + 100, -g.height()/2 + 100, 1000, 20);
			g.color(255);
			g.rect(-g.width()/2 + 100, -g.height()/2 + 100,_myRealSenseTextures.amountInBounds *1000, 20);
			g.popMatrix();
		}
		
	}

	public static void main(String[] args) {
		CCGL2Application myAppManager = new CCGL2Application(new BHApp());
		myAppManager.glcontext().size(1800, 1368);
		myAppManager.animator().framerate = 30;
		myAppManager.animator().animationMode = CCAnimator.CCAnimationMode.FRAMERATE_PRECISE;
		myAppManager.start();
	}
}

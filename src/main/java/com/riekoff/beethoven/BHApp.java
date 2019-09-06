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
import cc.creativecomputing.core.logging.CCLog;
import cc.creativecomputing.demo.simulation.particles.realsense.CCRealSenseForceField;
import cc.creativecomputing.graphics.CCDrawMode;
import cc.creativecomputing.graphics.CCGraphics;
import cc.creativecomputing.graphics.CCGraphics.CCBlendMode;
import cc.creativecomputing.graphics.app.CCGL2Adapter;
import cc.creativecomputing.graphics.app.CCGL2Application;
import cc.creativecomputing.graphics.camera.CCCameraController;
import cc.creativecomputing.graphics.export.CCScreenCaptureController;
import cc.creativecomputing.graphics.font.CCFontIO;
import cc.creativecomputing.graphics.shader.CCShaderBufferDebugger;
import cc.creativecomputing.graphics.texture.CCTexture2D;
import cc.creativecomputing.image.CCImageIO;
import cc.creativecomputing.io.CCNIOUtil;
import cc.creativecomputing.math.CCMath;
import cc.creativecomputing.math.CCVector2;
import cc.creativecomputing.math.CCVector3;
import cc.creativecomputing.math.spline.CCLinearSpline;
import cc.creativecomputing.simulation.particles.CCParticle;
import cc.creativecomputing.simulation.particles.CCParticles;
import cc.creativecomputing.simulation.particles.blends.CCBlend;
import cc.creativecomputing.simulation.particles.blends.CCConstantBlend;
import cc.creativecomputing.simulation.particles.blends.CCLifeTimeBlend;
import cc.creativecomputing.simulation.particles.blends.CCTextureBlend;
import cc.creativecomputing.simulation.particles.constraints.CCConstraint;
import cc.creativecomputing.simulation.particles.constraints.CCPositionConstraint;
import cc.creativecomputing.simulation.particles.emit.CCParticlesIndexParticleEmitter;
import cc.creativecomputing.simulation.particles.forces.CCAttractor;
import cc.creativecomputing.simulation.particles.forces.CCForce;
import cc.creativecomputing.simulation.particles.forces.CCForceField;
import cc.creativecomputing.simulation.particles.forces.CCGravity;
import cc.creativecomputing.simulation.particles.forces.CCPathTargetForce;
import cc.creativecomputing.simulation.particles.forces.CCTargetForce;
import cc.creativecomputing.simulation.particles.forces.CCTextureForceField2D;
import cc.creativecomputing.simulation.particles.forces.CCViscousDrag;
import cc.creativecomputing.simulation.particles.forces.springs.CCSpringForce;
import cc.creativecomputing.simulation.particles.render.CCParticleRenderer;
import cc.creativecomputing.simulation.particles.render.CCParticleTriangleRenderer;
import cc.creativecomputing.simulation.particles.render.CCQuadRenderer;
import cc.creativecomputing.simulation.particles.render.CCSpringLineRenderer;
import cc.creativecomputing.simulation.particles.render.CCSpringVolumentricLineRenderer;

public class BHApp extends CCGL2Adapter {

	

	@CCProperty(name = "particles")
	private CCParticles _myParticles;

	private CCParticlesIndexParticleEmitter _myEmitter;

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
	private CCRealSenseForceField _myRealSenseForceField;
	
	
	
	
	private CCTextureBlend _myTextureBlend;
	
	private CCTexture2D _myTexture;
	private CCTexture2D _myLines;
	int myPathResolution = 200;
	
	@CCProperty(name = "textpath speed", min = -10, max = 10)
	private double _cTextPathSpeed = 0;
	@CCProperty(name = "document path offset", min = -1, max = 1)
	private double _cDocumentPathOffset = 0;
	@CCProperty(name = "document path scale")
	private double _cDocumentPathScale = 500;

	List<CCVector3>[] _myNoteSheetPaths;
	List<CCVector3>[] _myZitatPaths;

	List<List<CCVector3>> myDebugDocumentSplines = new ArrayList<>();
	
	
	
	private void setPathPoints(CCLinearSpline theSpline, double theD0, double theD1, List<CCVector3> thePathPoints) {
		CCVector3 p0 = theSpline.interpolate(theD0);
		CCVector3 p1 = theSpline.interpolate(theD1);
		
		CCVector3 dir = p1.subtract(p0).normalizeLocal();
		
		thePathPoints.add(p0.add( dir.y * 10, -dir.x * 10, 0));
		thePathPoints.add(p0.add(-dir.y * 10,  dir.x * 10, 0));
	}
	
	private void setPathInfoData(List<CCVector3> thePathInfos, double thePath, double thePointIndex) {
		CCVector3 pd0 = new CCVector3(thePath, thePointIndex,  10);
		CCVector3 pd1 = new CCVector3(thePath, thePointIndex, -10);
		
		thePathInfos.add(pd0);
		thePathInfos.add(pd1);
	}
	
	private void calculatePathTargetData(BHVectors theNoteSheetVectors, BHVectors theZitatVectors, List<CCVector3>[] theNoteSheetPathData, List<CCVector3>[] theZitatPathData) {
		List<Integer> myIndices = new ArrayList<>();
		for(int i = 0; i < theNoteSheetVectors.size();i++) {
			myIndices.add(i);
		}
		
		while(myIndices.size() > 0) {
			//get the letter with the worst coverage
			double myMinCoverage = 100000000;
			int myZitatIndex = 0;
			for(int i = 0; i < theZitatVectors.size();i++) {
				double myCoverage = theZitatVectors.coverage(i);
				if(myCoverage < myMinCoverage) {
					myMinCoverage = myCoverage;
					myZitatIndex = i;
				}
			}
			
			CCVector3 myZitatCenter = theZitatVectors.centers.get(myZitatIndex);
			double myMinDist = 10000000;
			
			int myLookUpIndex = 0;
			int myNoteSheetIndex = 0;
			for(int i = 0; i < myIndices.size(); i++) {
				int myIndex = myIndices.get(i);
				
				CCVector3 myNoteSheetCenterCenter = theNoteSheetVectors.centers.get(myIndex);
				double dist = myNoteSheetCenterCenter.distance(myZitatCenter);
				if( dist < myMinDist) {
					myLookUpIndex = i;
					myMinDist = dist;
					myNoteSheetIndex = myIndex;
				}
			}
			myIndices.remove(myLookUpIndex);
			
			CCLinearSpline myNoteSheetSpline = theNoteSheetVectors.contours.get(myNoteSheetIndex);
			CCVector3 myNoteSheetCenter = theNoteSheetVectors.centers.get(myNoteSheetIndex);
			double myNoteSheetSplineLength = myNoteSheetSpline.totalLength();
			
			if(myMinDist < 3000) {
				theZitatVectors.coverages[myZitatIndex]+=myNoteSheetSplineLength;
			}else {
				myMinDist = 10000000;
				for(int i = 0; i < theZitatVectors.size();i++) {
					myZitatCenter = theZitatVectors.centers.get(i);
					double dist = myNoteSheetCenter.distance(myZitatCenter);
					if( dist < myMinDist) {
						myMinDist = dist;
						myZitatIndex = i;
					}
				}
			}
			
			CCLinearSpline myZitatSpline = theZitatVectors.contours.get(myZitatIndex);

			List<CCVector3> myNoteSheetPathPoints = new ArrayList<>();
			List<CCVector3> myNoteSheetPathInfos = new ArrayList<>();
			
			List<CCVector3> myZitatPathPoints = new ArrayList<>();
			List<CCVector3> myZitatPathInfos = new ArrayList<>();
			
			double myZitatSplineLength = myZitatSpline.totalLength();
			double myRatio = myNoteSheetSplineLength / myZitatSplineLength;
			
			double textOffset = CCMath.random();
			int myNumberOfPoints = CCMath.ceil(myNoteSheetSplineLength);
			
			for(int i = 0; i < myNumberOfPoints - 1;i++) {
				double d0 = CCMath.norm(i, 0, myNumberOfPoints);
				double d1 = CCMath.norm(i + 1, 0, myNumberOfPoints);
				double myNoteSheetPointIndex = CCMath.map(i, 0, myNumberOfPoints - 1, 0,myPathResolution);
				
				setPathPoints(myNoteSheetSpline, d0, d1, myNoteSheetPathPoints);
				setPathInfoData(myNoteSheetPathInfos, myNoteSheetIndex, myNoteSheetPointIndex);
				
				double td0= (d0 * myRatio + textOffset) % 1;
				double td1= (d1 * myRatio + textOffset) % 1;
				
				double myZitatPointIndex = CCMath.blend(0d, myPathResolution, td0);

				setPathPoints(myZitatSpline, td0, td1, myZitatPathPoints);
				setPathInfoData(myZitatPathInfos, myZitatIndex, myZitatPointIndex);
			}

			theNoteSheetPathData[myNoteSheetIndex] = myNoteSheetPathInfos;
			theZitatPathData[myNoteSheetIndex] = myZitatPathInfos;
			
			_myNoteSheetPaths[myNoteSheetIndex] = myNoteSheetPathPoints;
			_myZitatPaths[myNoteSheetIndex] = myZitatPathPoints;
		}
		
		for(int i = 0; i < theZitatVectors.size();i++) {
			CCLog.info(i + " " + theZitatVectors.coverage(i));
		}
	}
	
	private static class CCParticleVector{
		public final CCParticle particle;
		public final CCVector3 vector;
		
		public CCParticleVector(CCParticle theParticle, CCVector3 theVector) {
			particle = theParticle;
			vector = theVector;
		}
	}
	
	private void setupParticles(CCGraphics g, CCParticleTriangleRenderer theTriangleRenderer, List<CCVector3>[] theNoteSheetPathData, List<CCVector3>[] theZitatPathData) {
	
		List<CCParticleVector> myNewNoteSheetTargets = new ArrayList<>();
		List<CCParticleVector> myNewZitatTargets = new ArrayList<>();
		List<CCParticleVector> myBlendValues = new ArrayList<>();
		
		for(int j = 0; j < theNoteSheetPathData.length;j++) {
			List<CCVector3> myNoteSheetPath = _myNoteSheetPaths[j];
			List<CCVector3> myNoteSheetPathData = theNoteSheetPathData[j];
			List<CCVector3> myZitatPathData = theZitatPathData[j];
			CCParticle myLast0 = null;
			CCParticle myLast1 = null;
			
			for(int i = 0; i < myNoteSheetPath.size();i++) {
				CCVector3 myPoint = myNoteSheetPath.get(i);
				CCVector3 myPosition = new CCVector3(myPoint.x,  _myTexture.height() - myPoint.y);

				CCParticle myParticle = _myEmitter.emit(myPosition, new CCVector3());
				myParticle.texCoords().set(myPosition.x / _myTexture.width(), myPosition.y / _myTexture.height(), 0, 0);
				myParticle.target().set(myPosition.x, myPosition.y, myPosition.z, 1);
				
				if (myLast0 != null) {
					_mySprings.addSpring(myParticle, myLast0);
				}
				if (myLast1 != null) {
					_mySprings.addSpring(myParticle, myLast1);
				}
				theTriangleRenderer.addTriangle(myParticle, myLast0, myLast1);
			
				myLast1 = myLast0;
				myLast0 = myParticle;
				
				CCVector3 myBlendValue = new CCVector3(
					CCMath.norm(j, 0, theNoteSheetPathData.length - 1),
					CCMath.norm(i, 0, myNoteSheetPath.size() - 1),
					CCMath.random()
				);
				
				myNewNoteSheetTargets.add(new CCParticleVector(myParticle, myNoteSheetPathData.get(i)));
				myNewZitatTargets.add(new CCParticleVector(myParticle, myZitatPathData.get(i)));
				myBlendValues.add(new CCParticleVector(myParticle, myBlendValue));
			}
		}
		
		_myNoteSheetTargetForce.beginSetTargets(g);
		myNewNoteSheetTargets.forEach( pv -> _myNoteSheetTargetForce.addTarget(pv.particle, pv.vector));
		_myNoteSheetTargetForce.endSetTargets(g);
		
		_myZitatTargetForce.beginSetTargets(g);
		myNewZitatTargets.forEach( pv -> _myZitatTargetForce.addTarget(pv.particle, pv.vector));
		_myZitatTargetForce.endSetTargets(g);
		
		_myTextureBlend.beginSetBlends(g);
		myBlendValues.forEach( pv -> _myTextureBlend.addBlend(pv.particle, pv.vector.x, pv.vector.y, pv.vector.z));
		_myTextureBlend.endSetBlends(g);
		
	}
	
	@Override
	public void init(CCGraphics g, CCAnimator theAnimator) {
		_cScreenCaptureController = new CCScreenCaptureController(this);
		_myRealSenseForceField = new CCRealSenseForceField(CCNIOUtil.dataPath("realsense02.byt"),1280,720);
		_myTexture = new CCTexture2D(CCImageIO.newImage(CCNIOUtil.dataPath("Wittgenstein.png")));
		_myLines = new CCTexture2D(CCImageIO.newImage(CCNIOUtil.dataPath("lines.jpg")));
		
		BHVectors myZitatVectors = new BHVectors(CCNIOUtil.dataPath("Zitat2.svg"));
		BHVectors myNoteSheetVectors = new BHVectors(CCNIOUtil.dataPath("Wittgenstein.svg"));
		
		List<CCForce> myForces = new ArrayList<>();
		myForces.add(new CCForceField());
		myForces.add(new CCViscousDrag());
		myForces.add(new CCAttractor());
		myForces.add(_mySprings = new CCSpringForce(4, 4f));
		myForces.add(_myNoteSheetTargetForce = new CCPathTargetForce(myNoteSheetVectors.size(), myPathResolution));
		myForces.add(_myZitatTargetForce = new CCPathTargetForce(myZitatVectors.size(), myPathResolution));
		myForces.add(_myForceField = new CCTextureForceField2D(_myRealSenseForceField.forceField(), new CCVector2(1920d, -1080d), new CCVector2(0.5, 0.5)));
		myForces.add(new CCGravity());
		
		List<CCBlend> myBlends = new ArrayList<>();
		myBlends.add(new CCConstantBlend());
		myBlends.add(_myTextureBlend = new CCTextureBlend());
		
		List<CCConstraint> myConstraints = new ArrayList<>();
//		myConstraints.add(_myPositionConstraint = new CCPositionConstraint(4));

		CCParticleTriangleRenderer _myTriangleRenderer = new CCParticleTriangleRenderer(3);
		_myTriangleRenderer.texture(_myTexture);
		List<CCParticleRenderer> myRenderer = new ArrayList<>();

		myRenderer.add(new CCSpringVolumentricLineRenderer(_mySprings, false));
		myRenderer.add(new CCSpringLineRenderer(_mySprings));
		myRenderer.add(_myTriangleRenderer);
		myRenderer.add(new CCQuadRenderer());

		_myParticles = new CCParticles(g, myRenderer, myForces, myBlends, myConstraints, new ArrayList<>(), _myXres, _myYres);
		_myParticles.addEmitter(_myEmitter = new CCParticlesIndexParticleEmitter(_myParticles));

		_cCameraController = new CCCameraController(this, g, 100);

		g.strokeWeight(0.5f);

		_myDebugger = new CCShaderBufferDebugger(_mySprings.idBuffer());
		_myParticleDebugger = new CCShaderBufferDebugger(_myParticles.infoData());

		g.textFont(CCFontIO.createTextureMapFont("arial", 12));
		
		myNoteSheetVectors.targetForceSetup(g,_myNoteSheetTargetForce, _myTexture.height());
		myZitatVectors.targetForceSetup(g,_myZitatTargetForce, _myTexture.height());

		List<CCVector3>[] myNoteSheetPathData = new List[myNoteSheetVectors.size()];
		List<CCVector3>[] myZitatPathData = new List[myNoteSheetVectors.size()];
		
		_myNoteSheetPaths = new List[myNoteSheetVectors.size()];
		_myZitatPaths = new List[myNoteSheetVectors.size()];
		
		calculatePathTargetData(myNoteSheetVectors, myZitatVectors, myNoteSheetPathData, myZitatPathData);
		
		setupParticles(g, _myTriangleRenderer, myNoteSheetPathData, myZitatPathData);
	}

	int myIndex = 0;
	double _myTextOffset = 0;

	@Override
	public void update(final CCAnimator theAnimator) {
		_myTextOffset += theAnimator.deltaTime() * _cTextPathSpeed;
		
		_myNoteSheetTargetForce.pathAdd(_cDocumentPathOffset * _cDocumentPathScale);
		_myZitatTargetForce.pathAdd(_myTextOffset);
		_myRealSenseForceField.update(theAnimator);
		_myParticles.update(theAnimator);
	}

	@CCProperty(name = "debug")
	private boolean _cDebug = true;

	@Override
	public void display(CCGraphics g) {
		_myRealSenseForceField.preDisplay(g);
		
		_myParticles.preDisplay(g);
		g.clearColor(0,0,0);
		g.clear();
		g.pushMatrix();
		_cCameraController.camera().draw(g);

		g.blend();
		g.pushMatrix();
		g.color(1d);
		_myForceField.display(g);
		g.popMatrix();

		g.blend(CCBlendMode.ADD);
		g.color(255);
		g.image(_myLines, 0,0);
		g.noDepthTest();
		_myParticles.display(g);
		
		
//		g.color(1f,0.01);
//		for(int i = 0; i < _myZitatPaths.length;i++) {
//			List<CCVector3> myTextPoints = _myZitatPaths[i];
//			List<CCVector3> myPoints = _myNoteSheetPaths[i];
//			
//			g.beginShape(CCDrawMode.LINES);
//			for(int j = 0; j < myTextPoints.size();j++) {
//				CCVector3 myTextPoint = myTextPoints.get(j);
//				CCVector3 myPoint = myPoints.get(j);
//
//				double dist = myPoint.distance(myTextPoint);
////				if(dist > 3000) {
//				g.vertex(myPoint.x, _myTexture.height() - myPoint.y);
//				g.vertex(myTextPoint.x, _myTexture.height() - myTextPoint.y);
////				}
//			}
//			g.endShape();
//		}
		
//
//		g.blend();
		g.popMatrix();
		
		
		
//		g.color(1d);
////		CCLog.info(g.vendor());
//		_myDebugger.display(g);
//		_myParticleDebugger.display(g);
	}

	public static void main(String[] args) {
		CCGL2Application myAppManager = new CCGL2Application(new BHApp());
		myAppManager.glcontext().size(1800, 1368);
		myAppManager.animator().framerate = 30;
		myAppManager.animator().animationMode = CCAnimator.CCAnimationMode.FRAMERATE_PRECISE;
		myAppManager.start();
	}
}

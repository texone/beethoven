package com.riekoff.beethoven;

import java.util.ArrayList;
import java.util.List;

import cc.creativecomputing.core.logging.CCLog;
import cc.creativecomputing.graphics.CCDrawMode;
import cc.creativecomputing.graphics.CCGraphics;
import cc.creativecomputing.graphics.texture.CCTexture2D;
import cc.creativecomputing.image.CCImageIO;
import cc.creativecomputing.io.CCNIOUtil;
import cc.creativecomputing.math.CCMath;
import cc.creativecomputing.math.CCVector3;
import cc.creativecomputing.math.spline.CCLinearSpline;
import cc.creativecomputing.simulation.particles.CCParticle;
import cc.creativecomputing.simulation.particles.blends.CCTextureBlend;
import cc.creativecomputing.simulation.particles.emit.CCParticlesIndexParticleEmitter;
import cc.creativecomputing.simulation.particles.forces.CCPathTargetForce;
import cc.creativecomputing.simulation.particles.forces.springs.CCSpringForce;
import cc.creativecomputing.simulation.particles.render.CCParticleTriangleRenderer;

public class BHVectorManager {
	
	private final List<CCVector3>[] _myNoteSheetPaths;
	private final List<CCVector3>[] _myZitatPaths;
	
	private final List<CCVector3>[] _myNoteSheetPathDatas;
	private final List<CCVector3>[] _myZitatPathDatas;

	private final int _myPathResolution = 200;
	
	private final BHVectors _myZitatVectors;
	private final BHVectors _myNoteSheetVectors;
	
	public final CCPathTargetForce noteSheetTargetForce;
	public final CCPathTargetForce zitatTargetForce;
	
	public final CCSpringForce springs;

	public final CCTextureBlend textureBlend;
	
	public final CCTexture2D notesTexture;
	public final CCTexture2D linesTextures;
	
	public final CCParticleTriangleRenderer triangleRenderer;
	
	public BHVectorManager() {
		notesTexture = new CCTexture2D(CCImageIO.newImage(CCNIOUtil.dataPath("Wittgenstein.png")));
		linesTextures = new CCTexture2D(CCImageIO.newImage(CCNIOUtil.dataPath("lines.jpg")));
		
		_myZitatVectors = new BHVectors(CCNIOUtil.dataPath("Zitat3.svg"));
		_myNoteSheetVectors = new BHVectors(CCNIOUtil.dataPath("Wittgenstein.svg"));
		
		noteSheetTargetForce = new CCPathTargetForce(_myNoteSheetVectors.size(), _myPathResolution);
		zitatTargetForce = new CCPathTargetForce(_myZitatVectors.size(), _myPathResolution);
		textureBlend = new CCTextureBlend();
		springs = new CCSpringForce(4, 4f);
		
		triangleRenderer = new CCParticleTriangleRenderer(3);
		triangleRenderer.texture(notesTexture);
				
		_myNoteSheetPathDatas = new List[_myNoteSheetVectors.size()];
		_myZitatPathDatas = new List[_myNoteSheetVectors.size()];
		
		_myNoteSheetPaths = new List[_myNoteSheetVectors.size()];
		_myZitatPaths = new List[_myNoteSheetVectors.size()];
	}
	
	public int zitatVectorsSize() {
		return _myZitatVectors.size();
	}
	public int noteSheetVectorsSize() {
		return _myNoteSheetVectors.size();
	}
	
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
	
	public void calculatePathTargetData(List<CCVector3>[] theNoteSheetPathData, List<CCVector3>[] theZitatPathData) {
		List<Integer> myIndices = new ArrayList<>();
		for(int i = 0; i < _myNoteSheetVectors.size();i++) {
			myIndices.add(i);
		}
		
		while(myIndices.size() > 0) {
			//get the letter with the worst coverage
			double myMinCoverage = 100000000;
			int myZitatIndex = 0;
			for(int i = 0; i < _myZitatVectors.size();i++) {
				double myCoverage = _myZitatVectors.coverage(i);
				if(myCoverage < myMinCoverage) {
					myMinCoverage = myCoverage;
					myZitatIndex = i;
				}
			}
			
			CCVector3 myZitatCenter = _myZitatVectors.centers.get(myZitatIndex);
			double myMinDist = 10000000;
			
			int myLookUpIndex = 0;
			int myNoteSheetIndex = 0;
			for(int i = 0; i < myIndices.size(); i++) {
				int myIndex = myIndices.get(i);
				
				CCVector3 myNoteSheetCenterCenter = _myNoteSheetVectors.centers.get(myIndex);
				double dist = myNoteSheetCenterCenter.distance(myZitatCenter);
				if( dist < myMinDist) {
					myLookUpIndex = i;
					myMinDist = dist;
					myNoteSheetIndex = myIndex;
				}
			}
			myIndices.remove(myLookUpIndex);
			
			CCLinearSpline myNoteSheetSpline = _myNoteSheetVectors.contours.get(myNoteSheetIndex);
			CCVector3 myNoteSheetCenter = _myNoteSheetVectors.centers.get(myNoteSheetIndex);
			double myNoteSheetSplineLength = myNoteSheetSpline.totalLength();
			
			if(myMinDist < 3000) {
				_myZitatVectors.coverages[myZitatIndex]+=myNoteSheetSplineLength;
			}else {
				myMinDist = 10000000;
				for(int i = 0; i < _myZitatVectors.size();i++) {
					myZitatCenter = _myZitatVectors.centers.get(i);
					double dist = myNoteSheetCenter.distance(myZitatCenter);
					if( dist < myMinDist) {
						myMinDist = dist;
						myZitatIndex = i;
					}
				}
			}
			
			CCLinearSpline myZitatSpline = _myZitatVectors.contours.get(myZitatIndex);

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
				double myNoteSheetPointIndex = CCMath.map(i, 0, myNumberOfPoints - 1, 0, _myPathResolution);
				
				setPathPoints(myNoteSheetSpline, d0, d1, myNoteSheetPathPoints);
				setPathInfoData(myNoteSheetPathInfos, myNoteSheetIndex, myNoteSheetPointIndex);
				
				double td0= (d0 * myRatio + textOffset) % 1;
				double td1= (d1 * myRatio + textOffset) % 1;
				
				double myZitatPointIndex = CCMath.blend(0d, _myPathResolution, td0);

				setPathPoints(myZitatSpline, td0, td1, myZitatPathPoints);
				setPathInfoData(myZitatPathInfos, myZitatIndex, myZitatPointIndex);
			}

			theNoteSheetPathData[myNoteSheetIndex] = myNoteSheetPathInfos;
			theZitatPathData[myNoteSheetIndex] = myZitatPathInfos;
			
			_myNoteSheetPaths[myNoteSheetIndex] = myNoteSheetPathPoints;
			_myZitatPaths[myNoteSheetIndex] = myZitatPathPoints;
		}
		
		for(int i = 0; i < _myZitatVectors.size();i++) {
			CCLog.info(i + " " + _myZitatVectors.coverage(i));
		}
	}
	
	public void setupParticles(CCGraphics g, CCParticlesIndexParticleEmitter theEmitter) {
		_myNoteSheetVectors.targetForceSetup(g,noteSheetTargetForce, linesTextures.height());
		_myZitatVectors.targetForceSetup(g,zitatTargetForce, linesTextures.height());
		
		List<CCParticleVector> myNewNoteSheetTargets = new ArrayList<>();
		List<CCParticleVector> myNewZitatTargets = new ArrayList<>();
		List<CCParticleVector> myBlendValues = new ArrayList<>();
		
		for(int j = 0; j < _myNoteSheetPathDatas.length;j++) {
			List<CCVector3> myNoteSheetPath = _myNoteSheetPaths[j];
			List<CCVector3> myNoteSheetPathData = _myNoteSheetPathDatas[j];
			List<CCVector3> myZitatPathData = _myZitatPathDatas[j];
			CCParticle myLast0 = null;
			CCParticle myLast1 = null;
			
			double myNoteSheetRandom = CCMath.random();
			double myZitatRandom = CCMath.random();
			double myBlendRandom = CCMath.random();
			
			for(int i = 0; i < myNoteSheetPath.size();i++) {
				CCVector3 myPoint = myNoteSheetPath.get(i);
				CCVector3 myPosition = new CCVector3(myPoint.x,  linesTextures.height() - myPoint.y);

				CCParticle myParticle = theEmitter.emit(myPosition, new CCVector3());
				myParticle.texCoords().set(myPosition.x / linesTextures.width(), myPosition.y / linesTextures.height(), 0, 0);
				myParticle.target().set(myPosition.x, myPosition.y, myPosition.z, 1);
				
				if (myLast0 != null) {
					springs.addSpring(myParticle, myLast0);
				}
				if (myLast1 != null) {
					springs.addSpring(myParticle, myLast1);
				}
				triangleRenderer.addTriangle(myParticle, myLast0, myLast1);
			
				myLast1 = myLast0;
				myLast0 = myParticle;
				
				CCVector3 myBlendValue = new CCVector3(
					CCMath.norm(j, 0, _myNoteSheetPathDatas.length - 1),
					CCMath.norm(i, 0, myNoteSheetPath.size() - 1),
					myBlendRandom
				);
				
				myNewNoteSheetTargets.add(new CCParticleVector(myParticle, myNoteSheetPathData.get(i), myNoteSheetRandom));
				myNewZitatTargets.add(new CCParticleVector(myParticle, myZitatPathData.get(i), myZitatRandom));
				myBlendValues.add(new CCParticleVector(myParticle, myBlendValue, 0));
			}
		}
		
		noteSheetTargetForce.beginSetTargets(g);
		myNewNoteSheetTargets.forEach( pv -> noteSheetTargetForce.addTarget(pv.particle, pv.vector, pv.random));
		noteSheetTargetForce.endSetTargets(g);
		
		zitatTargetForce.beginSetTargets(g);
		myNewZitatTargets.forEach( pv -> zitatTargetForce.addTarget(pv.particle, pv.vector, pv.random));
		zitatTargetForce.endSetTargets(g);
		
		textureBlend.beginSetBlends(g);
		myBlendValues.forEach( pv -> textureBlend.addBlend(pv.particle, pv.vector.x, pv.vector.y, pv.vector.z));
		textureBlend.endSetBlends(g);
		
	}

	public void setup(CCGraphics g,  CCParticlesIndexParticleEmitter theEmitter) {
		calculatePathTargetData(_myNoteSheetPathDatas, _myZitatPathDatas);
		setupParticles(g,  theEmitter);
	}
	
	public void display(CCGraphics g) {
		
//		for(int i = 0; i < _myZitatPaths.length;i++) {
//			List<CCVector3> myTextPoints = _myZitatPaths[i];
//			List<CCVector3> myPoints = _myNoteSheetPaths[i];
//			
//			g.color(1f,0.01);
//			g.beginShape(CCDrawMode.LINES);
//			for(int j = 0; j < myTextPoints.size();j++) {
//				CCVector3 myTextPoint = myTextPoints.get(j);
//				CCVector3 myPoint = myPoints.get(j);
//
//				double dist = myPoint.distance(myTextPoint);
////				if(dist > 3000) {
//				g.vertex(myPoint.x, linesTextures.height() - myPoint.y);
//				g.vertex(myTextPoint.x, linesTextures.height() - myTextPoint.y);
////				}
//			}
//			g.endShape();
//			
//			g.color(1f,0f,0f);
//			g.beginShape(CCDrawMode.LINE_STRIP);
//			for(int j = 0; j < myTextPoints.size();j++) {
//				CCVector3 myTextPoint = myTextPoints.get(j);
//				g.vertex(myTextPoint.x, linesTextures.height() - myTextPoint.y);
//			}
//			g.endShape();
//		}
		
		for(int i = 0; i < _myZitatVectors.size();i++) {
			CCLinearSpline  myTextPoints = _myZitatVectors.contours.get(i);

			g.color(1f,0f,0f);
			g.beginShape(CCDrawMode.LINE_STRIP);
			for(CCVector3 myPoint:myTextPoints) {
				g.vertex(myPoint);
			}
			g.endShape();
			
		}
	}
}

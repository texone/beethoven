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
import cc.creativecomputing.simulation.particles.CCParticles;
import cc.creativecomputing.simulation.particles.blends.CCTextureBlend;
import cc.creativecomputing.simulation.particles.emit.CCParticlesIndexParticleEmitter;
import cc.creativecomputing.simulation.particles.emit.gpu.CCEmitter;
import cc.creativecomputing.simulation.particles.forces.CCPathTargetForce;
import cc.creativecomputing.simulation.particles.forces.springs.CCSpringForce;
import cc.creativecomputing.simulation.particles.render.CCIndexedParticleRenderer;
import cc.creativecomputing.simulation.particles.render.CCParticleTriangleRenderer;

public class BHVectorManager {
	
	List<CCVector3>[] _myNoteSheetPaths;
	List<CCVector3>[] _myZitatPaths;

	public int myPathResolution = 200;
	
	public BHVectors myZitatVectors;
	public BHVectors myNoteSheetVectors;
	
	public CCPathTargetForce noteSheetTargetForce;
	public CCPathTargetForce zitatTargetForce;
	
	public CCSpringForce springs;

	public CCTextureBlend textureBlend;
	
	List<CCVector3>[] myNoteSheetPathDatas;
	List<CCVector3>[] myZitatPathDatas;
	
	public CCTexture2D notesTexture;
	public CCTexture2D linesTextures;
	
	public CCParticleTriangleRenderer triangleRenderer;
	
	public BHVectorManager() {
		notesTexture = new CCTexture2D(CCImageIO.newImage(CCNIOUtil.dataPath("Wittgenstein.png")));
		linesTextures = new CCTexture2D(CCImageIO.newImage(CCNIOUtil.dataPath("lines.jpg")));
		
		myZitatVectors = new BHVectors(CCNIOUtil.dataPath("Zitat2.svg"));
		myNoteSheetVectors = new BHVectors(CCNIOUtil.dataPath("Wittgenstein.svg"));
		
		noteSheetTargetForce = new CCPathTargetForce(myNoteSheetVectors.size(), myPathResolution);
		zitatTargetForce = new CCPathTargetForce(myZitatVectors.size(), myPathResolution);
		textureBlend = new CCTextureBlend();
		springs = new CCSpringForce(4, 4f);
		
		triangleRenderer = new CCParticleTriangleRenderer(3);
		triangleRenderer.texture(notesTexture);
				
		myNoteSheetPathDatas = new List[myNoteSheetVectors.size()];
		myZitatPathDatas = new List[myNoteSheetVectors.size()];
		
		_myNoteSheetPaths = new List[myNoteSheetVectors.size()];
		_myZitatPaths = new List[myNoteSheetVectors.size()];
	}
	
	public int zitatVectorsSize() {
		return myZitatVectors.size();
	}
	public int noteSheetVectorsSize() {
		return myNoteSheetVectors.size();
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
		for(int i = 0; i < myNoteSheetVectors.size();i++) {
			myIndices.add(i);
		}
		
		while(myIndices.size() > 0) {
			//get the letter with the worst coverage
			double myMinCoverage = 100000000;
			int myZitatIndex = 0;
			for(int i = 0; i < myZitatVectors.size();i++) {
				double myCoverage = myZitatVectors.coverage(i);
				if(myCoverage < myMinCoverage) {
					myMinCoverage = myCoverage;
					myZitatIndex = i;
				}
			}
			
			CCVector3 myZitatCenter = myZitatVectors.centers.get(myZitatIndex);
			double myMinDist = 10000000;
			
			int myLookUpIndex = 0;
			int myNoteSheetIndex = 0;
			for(int i = 0; i < myIndices.size(); i++) {
				int myIndex = myIndices.get(i);
				
				CCVector3 myNoteSheetCenterCenter = myNoteSheetVectors.centers.get(myIndex);
				double dist = myNoteSheetCenterCenter.distance(myZitatCenter);
				if( dist < myMinDist) {
					myLookUpIndex = i;
					myMinDist = dist;
					myNoteSheetIndex = myIndex;
				}
			}
			myIndices.remove(myLookUpIndex);
			
			CCLinearSpline myNoteSheetSpline = myNoteSheetVectors.contours.get(myNoteSheetIndex);
			CCVector3 myNoteSheetCenter = myNoteSheetVectors.centers.get(myNoteSheetIndex);
			double myNoteSheetSplineLength = myNoteSheetSpline.totalLength();
			
			if(myMinDist < 3000) {
				myZitatVectors.coverages[myZitatIndex]+=myNoteSheetSplineLength;
			}else {
				myMinDist = 10000000;
				for(int i = 0; i < myZitatVectors.size();i++) {
					myZitatCenter = myZitatVectors.centers.get(i);
					double dist = myNoteSheetCenter.distance(myZitatCenter);
					if( dist < myMinDist) {
						myMinDist = dist;
						myZitatIndex = i;
					}
				}
			}
			
			CCLinearSpline myZitatSpline = myZitatVectors.contours.get(myZitatIndex);

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
				double myNoteSheetPointIndex = CCMath.map(i, 0, myNumberOfPoints - 1, 0, myPathResolution);
				
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
		
		for(int i = 0; i < myZitatVectors.size();i++) {
			CCLog.info(i + " " + myZitatVectors.coverage(i));
		}
	}
	
	public void setupParticles(CCGraphics g, CCParticlesIndexParticleEmitter theEmitter) {
		myNoteSheetVectors.targetForceSetup(g,noteSheetTargetForce, linesTextures.height());
		myZitatVectors.targetForceSetup(g,zitatTargetForce, linesTextures.height());
		
		List<CCParticleVector> myNewNoteSheetTargets = new ArrayList<>();
		List<CCParticleVector> myNewZitatTargets = new ArrayList<>();
		List<CCParticleVector> myBlendValues = new ArrayList<>();
		
		for(int j = 0; j < myNoteSheetPathDatas.length;j++) {
			List<CCVector3> myNoteSheetPath = _myNoteSheetPaths[j];
			List<CCVector3> myNoteSheetPathData = myNoteSheetPathDatas[j];
			List<CCVector3> myZitatPathData = myZitatPathDatas[j];
			CCParticle myLast0 = null;
			CCParticle myLast1 = null;
			
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
					CCMath.norm(j, 0, myNoteSheetPathDatas.length - 1),
					CCMath.norm(i, 0, myNoteSheetPath.size() - 1),
					CCMath.random()
				);
				
				myNewNoteSheetTargets.add(new CCParticleVector(myParticle, myNoteSheetPathData.get(i)));
				myNewZitatTargets.add(new CCParticleVector(myParticle, myZitatPathData.get(i)));
				myBlendValues.add(new CCParticleVector(myParticle, myBlendValue));
			}
		}
		
		noteSheetTargetForce.beginSetTargets(g);
		myNewNoteSheetTargets.forEach( pv -> noteSheetTargetForce.addTarget(pv.particle, pv.vector));
		noteSheetTargetForce.endSetTargets(g);
		
		zitatTargetForce.beginSetTargets(g);
		myNewZitatTargets.forEach( pv -> zitatTargetForce.addTarget(pv.particle, pv.vector));
		zitatTargetForce.endSetTargets(g);
		
		textureBlend.beginSetBlends(g);
		myBlendValues.forEach( pv -> textureBlend.addBlend(pv.particle, pv.vector.x, pv.vector.y, pv.vector.z));
		textureBlend.endSetBlends(g);
		
	}

	public void setup(CCGraphics g,  CCParticlesIndexParticleEmitter theEmitter) {
		calculatePathTargetData(myNoteSheetPathDatas, myZitatPathDatas);
		setupParticles(g,  theEmitter);
	}
	
	public void display(CCGraphics g) {
		g.color(1f,0.01);
		for(int i = 0; i < _myZitatPaths.length;i++) {
			List<CCVector3> myTextPoints = _myZitatPaths[i];
			List<CCVector3> myPoints = _myNoteSheetPaths[i];
			
			g.beginShape(CCDrawMode.LINES);
			for(int j = 0; j < myTextPoints.size();j++) {
				CCVector3 myTextPoint = myTextPoints.get(j);
				CCVector3 myPoint = myPoints.get(j);

				double dist = myPoint.distance(myTextPoint);
//				if(dist > 3000) {
				g.vertex(myPoint.x, linesTextures.height() - myPoint.y);
				g.vertex(myTextPoint.x, linesTextures.height() - myTextPoint.y);
//				}
			}
			g.endShape();
		}
	}
}

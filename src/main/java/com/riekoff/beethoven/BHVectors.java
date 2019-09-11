package com.riekoff.beethoven;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import cc.creativecomputing.graphics.CCGraphics;
import cc.creativecomputing.math.CCMath;
import cc.creativecomputing.math.CCVector3;
import cc.creativecomputing.math.spline.CCLinearSpline;
import cc.creativecomputing.model.svg.CCSVGDocument;
import cc.creativecomputing.model.svg.CCSVGIO;
import cc.creativecomputing.simulation.particles.forces.CCPathTargetForce;

public class BHVectors{
	public final List<CCLinearSpline> contours;
	public final List<CCVector3> centers;
	public final List<Double> length;
	public final double[] coverages;
	
	public BHVectors(Path theSVG) {
		CCSVGDocument myDocument = CCSVGIO.newSVG(theSVG);
		contours = myDocument.contours(0.001);
		centers = new ArrayList<>();
		length = new ArrayList<>();
		coverages = new double[contours.size()];
		int i = 0;
		for(CCLinearSpline mySpline:contours) {
			centers.add(center(mySpline));
			length.add(mySpline.totalLength());
			coverages[i++] = 0;
		}
	}
	
	public double coverage(int theIndex) {
		return coverages[theIndex] / length.get(theIndex);
	}
	
	public int size() {
		return contours.size();
	}
	
	private CCVector3 center(CCLinearSpline theSpline) {
		CCVector3 myCenter = new CCVector3();
		for(CCVector3 myPoint:theSpline) {
			myCenter.addLocal(myPoint);
		}
		myCenter.multiplyLocal(1d / theSpline.points().size());
		return myCenter;
	}
	
	public void targetForceSetup(CCGraphics g, CCPathTargetForce theForce, double theHeight) {
		theForce.beginSetPaths(g);
		int p = 0;
		for(CCLinearSpline myDocumentSpline:contours) {
			CCVector3 myFirst = myDocumentSpline.first().clone();
			myFirst.y = theHeight - myFirst.y;
			
			CCVector3 myLast = myDocumentSpline.last().clone();
			myLast.y = theHeight - myLast.y;
			
			theForce.setJump(p, myLast.subtract(myFirst));
			for(int i = 0; i < theForce.pathResolution();i++) {
				double d = CCMath.norm(i, 0, theForce.pathResolution() - 1);
				CCVector3 myPoint = myDocumentSpline.interpolate(d);
				myPoint.y = theHeight - myPoint.y;
				theForce.addPath(p, i, myPoint);
			}
			p++;
		}
		theForce.endSetPaths(g);
	}
}
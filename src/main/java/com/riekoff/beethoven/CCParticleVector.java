package com.riekoff.beethoven;

import cc.creativecomputing.math.CCVector3;
import cc.creativecomputing.simulation.particles.CCParticle;

public class CCParticleVector{
	public final CCParticle particle;
	public final CCVector3 vector;
	
	public CCParticleVector(CCParticle theParticle, CCVector3 theVector) {
		particle = theParticle;
		vector = theVector;
	}
}
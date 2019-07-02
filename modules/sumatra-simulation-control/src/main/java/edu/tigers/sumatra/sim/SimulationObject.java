/*
 * Copyright (c) 2009 - 2018, DHBW Mannheim - TIGERs Mannheim
 */

package edu.tigers.sumatra.sim;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import edu.tigers.sumatra.math.vector.IVector3;
import edu.tigers.sumatra.math.vector.Vector3;


/**
 * Container for a simulation object (robot, ball)
 */
public class SimulationObject
{
	/** [mm,mm,mm] */
	private IVector3 pos = Vector3.zero();
	/** [m/s,m/s,rad/s] */
	private IVector3 vel = Vector3.zero();


	/**
	 * Default
	 */
	public SimulationObject()
	{
		// empty
	}


	/**
	 * @return the pos [mm,mm,mm|rad]
	 */
	public final IVector3 getPos()
	{
		return pos;
	}


	/**
	 * @param pos the pos to set [mm,mm,mm|rad]
	 */
	public final void setPos(final IVector3 pos)
	{
		this.pos = pos;
	}


	/**
	 * @return the vel
	 */
	public final IVector3 getVel()
	{
		return vel;
	}


	/**
	 * @param vel the vel to set
	 */
	public final void setVel(final IVector3 vel)
	{
		this.vel = vel;
	}


	@Override
	public String toString()
	{
		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
				.append("pos", pos)
				.append("vel", vel)
				.toString();
	}
}

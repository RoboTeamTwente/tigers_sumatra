/*
 * Copyright (c) 2009 - 2020, DHBW Mannheim - TIGERs Mannheim
 */

package edu.tigers.sumatra.sim.collision.ball;

import edu.tigers.sumatra.ids.BotID;
import edu.tigers.sumatra.math.vector.IVector2;
import edu.tigers.sumatra.math.vector.IVector3;
import edu.tigers.sumatra.math.vector.Vector2f;
import edu.tigers.sumatra.math.vector.Vector3f;

import java.util.Optional;


/**
 * An object that can collide with the ball
 */
public interface ICollisionObject
{
	default IVector3 getVel()
	{
		return Vector3f.ZERO_VECTOR;
	}

	default IVector2 getSurfaceVel(IVector2 contactPos)
	{
		return Vector2f.ZERO_VECTOR;
	}


	default IVector3 getAcc()
	{
		return Vector3f.ZERO_VECTOR;
	}


	/**
	 * @return if the ball should stick on the obstacle
	 */
	default boolean isSticky()
	{
		return false;
	}


	default double getDampFactor()
	{
		return 0.5;
	}

	default double getDampFactorOrthogonal()
	{
		return 0;
	}

	default IVector2 stick(IVector2 pos)
	{
		return pos;
	}


	/**
	 * Get the collision information, if a collision is present.<br>
	 * A collision must be between pre and post pos. If both points are inside the obstacle, there is no collision.
	 *
	 * @param prePos  old pos
	 * @param postPos new pos
	 * @return collision information, if present
	 */
	Optional<ICollision> getCollision(IVector3 prePos, IVector3 postPos);


	/**
	 * Get the collision information, if pos is inside obstacle
	 *
	 * @param pos the current pos
	 * @return collision information, if present
	 */
	default Optional<ICollision> getInsideCollision(IVector3 pos)
	{
		return Optional.empty();
	}


	/**
	 * @return the impulse to add to the ball on a collision
	 */
	default IVector3 getImpulse()
	{
		return Vector3f.ZERO_VECTOR;
	}


	/**
	 * @return the bot id of the colliding robot or no_bot
	 */
	default BotID getBotID()
	{
		return BotID.noBot();
	}

	/**
	 * @return if the collision object is a field boundary and therefore can't be chipped over
	 */
	default boolean isFieldBoundary()
	{
		return false;
	}
}

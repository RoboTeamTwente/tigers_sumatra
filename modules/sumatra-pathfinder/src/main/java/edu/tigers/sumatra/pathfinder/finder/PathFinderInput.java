/*
 * Copyright (c) 2009 - 2022, DHBW Mannheim - TIGERs Mannheim
 */

package edu.tigers.sumatra.pathfinder.finder;

import edu.tigers.sumatra.bot.IMoveConstraints;
import edu.tigers.sumatra.bot.State;
import edu.tigers.sumatra.math.vector.IVector2;
import edu.tigers.sumatra.math.vector.IVector3;
import edu.tigers.sumatra.pathfinder.obstacles.IObstacle;
import edu.tigers.sumatra.trajectory.ITrajectory;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.List;


/**
 * Data holder for path planning information
 */
@Builder(toBuilder = true)
@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class PathFinderInput
{
	long timestamp;
	IMoveConstraints moveConstraints;
	IVector2 pos;
	IVector2 vel;
	IVector2 dest;
	List<IObstacle> obstacles;


	/**
	 * Create the input from a tracked bot
	 *
	 * @param state
	 * @return
	 */
	public static PathFinderInput.PathFinderInputBuilder fromBot(State state)
	{
		return PathFinderInput.builder()
				.pos(state.getPos())
				.vel(state.getVel2());
	}


	/**
	 * Create the input from the current trajectory
	 *
	 * @param trajectory
	 * @return
	 */
	public static PathFinderInput.PathFinderInputBuilder fromTrajectory(ITrajectory<IVector3> trajectory)
	{
		return PathFinderInput.builder()
				.pos(trajectory.getPositionMM(0).getXYVector())
				.vel(trajectory.getVelocity(0).getXYVector());
	}
}

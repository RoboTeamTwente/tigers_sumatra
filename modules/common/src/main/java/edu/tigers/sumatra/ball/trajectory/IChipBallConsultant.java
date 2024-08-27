/*
 * Copyright (c) 2009 - 2021, DHBW Mannheim - TIGERs Mannheim
 */

package edu.tigers.sumatra.ball.trajectory;

import edu.tigers.sumatra.ball.trajectory.chipped.ChipBallTrajectory;
import edu.tigers.sumatra.math.vector.IVector3;


/**
 * Consultant for chipped balls.
 */
public interface IChipBallConsultant extends IBallConsultant
{
	/**
	 * Get initial velocity such that the ball hits ground for the n-th time after given distance.<br>
	 * n == 0 => first touchdown location<br>
	 * n == 1 => second touchdown location...
	 *
	 * @param distance     [mm]
	 * @param numTouchdown
	 * @return initial velocity [m/s]
	 */
	double getInitVelForDistAtTouchdown(final double distance, final int numTouchdown);


	/**
	 * Get initial velocity such that the peak height of the first parabola/hop is equal to the given height.
	 *
	 * @param height Desired parabola peak height [mm]
	 * @return initial velocity [m/s]
	 */
	double getInitVelForPeakHeight(final double height);


	/**
	 * Get minimum distance from where an obstacle (e.g. a bot) can safely be over-chipped when kicking with a specific
	 * velocity.<br>
	 * That is where the ball's height gets greater than the specified height.<br>
	 * This method only uses only the first hop of the ball trajectory.
	 *
	 * @param initVel Absolute kick velocity, can be calculated by {@link #getInitVelForDistAtTouchdown(double, int)} for
	 *                a specific distance [m/s]
	 * @param height  Height that must be passed [mm]
	 * @return the minimum distance or <b>infinity</b> if the specified height is never reached [mm]
	 */
	double getMinimumDistanceToOverChip(final double initVel, final double height);


	/**
	 * Get maximum distance to where an obstacle (e.g. a bot) can safely be over-chipped when kicking with a specific
	 * velocity.<br>
	 * That is where the ball's height gets lower than the specified height.<br>
	 * This method only uses the first hop of the ball trajectory.<br>
	 *
	 * @param initVel Absolute kick velocity, can be calculated by {@link #getInitVelForDistAtTouchdown(double, int)} for
	 *                a specific distance [m/s]
	 * @param height  Height that must be passed [mm]
	 * @return the maximum distance or zero if the specified height is never reached [mm]
	 */
	double getMaximumDistanceToOverChip(final double initVel, final double height);


	/**
	 * Get a new chip consultant of the same type with a specific chipAngle (default is 45°).
	 *
	 * @param chipAngle chip angle in [deg]
	 * @return this
	 */
	IChipBallConsultant withChipAngle(double chipAngle);


	/**
	 * Convert an absolute kick velocity to an x (flat) and y (up) velocity.
	 *
	 * @param direction the direction in xy [rad]
	 * @param speed     the absolute speed (length of the resulting vector) [any unit]
	 * @return
	 */
	IVector3 speedToVel(double direction, double speed);


	/**
	 * Calculates the bot velocity to chip farther than the maximum distance
	 *
	 * @param distance      [mm]
	 * @param maxKickVel    [m/s]
	 * @param numTouchdowns
	 * @return bot vel [m/s]
	 */
	double botVelocityToChipFartherThanMaximumDistance(double distance, int numTouchdowns, double maxKickVel);


	/**
	 * Calculate the absolute velocity of the ball after given distance.
	 *
	 * @param distance
	 * @param kickSpeed
	 * @return
	 */
	double getVelForKick(double distance, double kickSpeed);

	/**
	 * Get the distance at which the target ball speed is reached by a chip kick
	 *
	 * @param kickSpeed       in [m/s]
	 * @param targetBallSpeed in [m/s]
	 * @return distance in [mm]
	 */
	double getDistanceByTargetVel(double kickSpeed, double targetBallSpeed);

	/**
	 * Get a chip trajectory from a kickspeed
	 * @param kickSpeed in [m/s]
	 * @return
	 */
	ChipBallTrajectory getChipTrajectory(double kickSpeed);
}

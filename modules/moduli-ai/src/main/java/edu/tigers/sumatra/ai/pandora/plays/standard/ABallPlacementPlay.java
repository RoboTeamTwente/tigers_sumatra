/*
 * Copyright (c) 2009 - 2023, DHBW Mannheim - TIGERs Mannheim
 */

package edu.tigers.sumatra.ai.pandora.plays.standard;

import edu.tigers.sumatra.ai.pandora.plays.APlay;
import edu.tigers.sumatra.ai.pandora.plays.EPlay;
import edu.tigers.sumatra.ai.pandora.roles.ARole;
import edu.tigers.sumatra.ai.pandora.roles.move.MoveRole;
import edu.tigers.sumatra.ai.pandora.roles.offense.KeepDistToBallRole;
import edu.tigers.sumatra.ai.pandora.roles.placement.BallPlacementRole;
import edu.tigers.sumatra.geometry.Geometry;
import edu.tigers.sumatra.ids.BotID;
import edu.tigers.sumatra.math.line.LineMath;
import edu.tigers.sumatra.math.vector.IVector2;

import java.util.Comparator;
import java.util.Objects;


/**
 * Base play for placing the ball.
 */
public abstract class ABallPlacementPlay extends APlay
{
	protected ABallPlacementPlay(final EPlay type)
	{
		super(type);
	}


	protected final void assignBallPlacementRoles()
	{
		IVector2 placementPos = getBallTargetPos();

		if (getRoles().isEmpty())
		{
			return;
		}

		var currentBallPlacementBot = findRoles(BallPlacementRole.class).stream()
				.map(BallPlacementRole::getBotID)
				.findFirst().orElse(BotID.noBot());
		if (getRoles().size() == 1)
		{
			var placementRole = reassignRole(getRoles().getFirst(), BallPlacementRole.class, BallPlacementRole::new);
			placementRole.setPassMode(BallPlacementRole.EPassMode.NONE);
		} else if (useAssistant())
		{
			ARole ballPlacementRole = getRoles()
					.stream()
					.min(Comparator.comparing(r -> getBall().getTrajectory().distanceTo(r.getPos())
							- ((Objects.equals(r.getBotID(), currentBallPlacementBot)) ? 500 : 0)))
					.map(r -> reassignRole(r, BallPlacementRole.class, BallPlacementRole::new))
					.orElseThrow();
			MoveRole receivingRole = allRolesExcept(ballPlacementRole)
					.stream()
					.min(Comparator.comparing(r -> r.getPos().distanceToSqr(getBallTargetPos())))
					.map(r -> reassignRole(r, MoveRole.class, MoveRole::new))
					.orElseThrow();


			double dist2Ball = receivingRole.getBot().getCenter2DribblerDist() + Geometry.getBallRadius();
			receivingRole.updateDestination(LineMath.stepAlongLine(placementPos, getBall().getPos(), -dist2Ball));
			receivingRole.updateLookAtTarget(getBall());
			boolean isReadyForPass = receivingRole.isDestinationReached();

			receivingRole.getMoveCon().physicalObstaclesOnly();
			receivingRole.getMoveCon().setBallObstacle(!isReadyForPass);

			findRoles(BallPlacementRole.class).forEach(r -> r.setPassMode(
					isReadyForPass
							? BallPlacementRole.EPassMode.READY
							: BallPlacementRole.EPassMode.WAIT));

			allRolesExcept(receivingRole, ballPlacementRole)
					.forEach(this::handleNonPlacingRole);
		} else
		{
			BallPlacementRole ballPlacementRole = getRoles()
					.stream()
					.min(Comparator.comparing(r -> getBall().getTrajectory().distanceTo(r.getPos())
							- ((Objects.equals(r.getBotID(), currentBallPlacementBot)) ? 500 : 0)))
					.map(r -> reassignRole(r, BallPlacementRole.class, BallPlacementRole::new))
					.orElseThrow();

			ballPlacementRole.setPassMode(BallPlacementRole.EPassMode.NONE);

			allRolesExcept(ballPlacementRole)
					.forEach(this::handleNonPlacingRole);
		}

		if (placementPos != null)
		{
			findRoles(BallPlacementRole.class).forEach(r -> r.setBallTargetPos(placementPos));
		}
	}


	protected final boolean ballPlacementDone()
	{
		return !getRoles().isEmpty() && findRoles(BallPlacementRole.class).stream()
				.allMatch(BallPlacementRole::isBallPlacedAndCleared);
	}


	protected boolean useAssistant()
	{
		return getRoles().size() > 1;
	}


	protected void handleNonPlacingRole(ARole role)
	{
		reassignRole(role, KeepDistToBallRole.class, KeepDistToBallRole::new);
	}


	protected abstract IVector2 getBallTargetPos();
}

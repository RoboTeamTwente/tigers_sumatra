/*
 * Copyright (c) 2009 - 2023, DHBW Mannheim - TIGERs Mannheim
 */

package edu.tigers.sumatra.skillsystem.skills;

import com.github.g3force.configurable.ConfigRegistration;
import com.github.g3force.configurable.Configurable;
import edu.tigers.sumatra.drawable.DrawableCircle;
import edu.tigers.sumatra.drawable.DrawableTriangle;
import edu.tigers.sumatra.geometry.Geometry;
import edu.tigers.sumatra.ids.BotID;
import edu.tigers.sumatra.math.intersections.IIntersections;
import edu.tigers.sumatra.math.line.ILineSegment;
import edu.tigers.sumatra.math.line.Lines;
import edu.tigers.sumatra.math.triangle.Triangle;
import edu.tigers.sumatra.math.vector.IVector2;
import edu.tigers.sumatra.math.vector.Vector2;
import edu.tigers.sumatra.math.vector.VectorMath;
import edu.tigers.sumatra.skillsystem.ESkillShapesLayer;
import edu.tigers.sumatra.statemachine.IEvent;
import edu.tigers.sumatra.wp.data.ITrackedBall;
import edu.tigers.sumatra.wp.data.ITrackedBot;
import edu.tigers.sumatra.wp.data.WorldFrame;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;


/**
 * Class for calculating the next keeper state.
 */
public class DefendingKeeper
{
	@Configurable(comment = "Ball Speed wher Keeper react on his direction", defValue = "0.1")
	private static double blockDecisionVelocity = 0.1;

	@Configurable(comment = "offset to the Sides of the goalposts (BalVelIsDirToGoal State)", defValue = "180.0")
	private static double goalAreaOffset = 180.0;

	@Configurable(comment = "Ball declared as shot after kick event", defValue = "0.5")
	private static double maxKickTime = 0.5;

	@Configurable(comment = "Distance from Opponent to Ball where Keeper beliefs Opponent could be in possession of the ball", defValue = "250.0")
	private static double opponentBotBallPossessionDistance = 250.0;

	@Configurable(comment = "Speed limit of ball of ChipKickState", defValue = "0.8")
	private static double chipKickDecisionVelocity = 0.8;

	@Configurable(comment = "Additional area around PE where Opponent with Ball are very dangerous (GoOutState is triggered)", defValue = "300.0")
	private static double ballDangerZone = 300.0;

	@Configurable(comment = "Additional distance to allow until ball reach goal line", defValue = "1000.0")
	private static double distToIntersectionTolerance = 1000;

	static
	{
		ConfigRegistration.registerClass("skills", DefendingKeeper.class);
	}

	private final ASkill drawingSkill;
	private double lastTimeKicked = 0;
	private WorldFrame worldFrame;
	private BotID keeperID;


	protected DefendingKeeper(final ASkill drawingSkill)
	{
		this.drawingSkill = drawingSkill;
	}


	protected void update(WorldFrame worldFrame, BotID keeperID)
	{
		this.worldFrame = worldFrame;
		this.keeperID = keeperID;
	}


	private ITrackedBall getBall()
	{
		return worldFrame.getBall();
	}


	private ITrackedBot getBot()
	{
		return worldFrame.getBot(keeperID);
	}


	protected ECriticalKeeperStates calcNextKeeperState()
	{
		// The order in if else represents the priority of the states
		ECriticalKeeperStates nextState;
		if (hasBallContact())
		{
			nextState = ECriticalKeeperStates.HAS_CONTACT;
		} else if (isSomeoneShootingAtOurGoal())
		{
			nextState = ECriticalKeeperStates.INTERCEPT_BALL;
		} else if (isOpponentRedirecting())
		{
			nextState = ECriticalKeeperStates.DEFEND_REDIRECT;
		} else if (isGoOutFeasible())
		{
			nextState = ECriticalKeeperStates.GO_OUT;
		} else
		{
			nextState = ECriticalKeeperStates.NORMAL;
		}
		return nextState;
	}


	private boolean hasBallContact()
	{
		return getBot().getBallContact().getContactDuration() > 0.1;
	}

	private boolean isSomeoneShootingAtOurGoal()
	{
		ILineSegment goalLine = Geometry.getGoalOur()
				.withMargin(0, Geometry.getGoalOur().getWidth() / 2.0).getLineSegment();
		Optional<IVector2> intersect = getBall().getTrajectory().getTravelLineSegments().stream()
				.map(line -> line.intersect(goalLine))
				.flatMap(IIntersections::stream)
				.findAny();

		if (intersect.isPresent() && (getBall().getVel().x() < 0))
		{
			double distToIntersection = intersect.get().distanceTo(getBall().getPos());
			double distWithTolerance = Math.max(0, distToIntersection - distToIntersectionTolerance);
			boolean isBallFastEnough =
					getBall().getTrajectory().getAbsVelByDist(distWithTolerance) > blockDecisionVelocity;
			boolean isBallOnOurSite = getBall().getPos().x() < 0;
			boolean isBallVelocityIntersectingTheGoalLine = Math
					.abs(intersect.get().y()) < ((Geometry.getGoalOur().getWidth() / 2) + goalAreaOffset);

			worldFrame.getKickedBall().ifPresent(iKickEvent -> lastTimeKicked = iKickEvent.getTimestamp());
			boolean isBallKicked = ((lastTimeKicked - worldFrame.getTimestamp()) / 1e9) < maxKickTime;

			boolean isBallLeavingOpponent = !isOpponentNearBall() || isBallKicked;

			return isBallOnOurSite
					&& isBallVelocityIntersectingTheGoalLine
					&& isBallFastEnough
					&& isBallLeavingOpponent;
		}
		return false;
	}


	private boolean isOpponentNearBall()
	{
		return worldFrame.getOpponentBots().values().stream()
				.anyMatch(b -> b.getPos().distanceTo(getBall().getPos()) < opponentBotBallPossessionDistance);
	}


	private boolean isOpponentRedirecting()
	{
		BotID redirectOpponentBotId = getBestRedirector(worldFrame.getOpponentBots());
		IVector2 redirectOpponentBot = null;
		if (redirectOpponentBotId.isBot())
		{
			redirectOpponentBot = worldFrame.getOpponentBot(redirectOpponentBotId).getPos();
		}

		// Alternative to p2pVisibility in AiMath
		boolean p2pVisibilityRedirectOpponentGoal;
		if (redirectOpponentBot != null)
		{
			var lineGoalOpponentBot = Lines.segmentFromPoints(redirectOpponentBot, Geometry.getGoalOur().getCenter());
			p2pVisibilityRedirectOpponentGoal = worldFrame.getBots().values().stream()
					.filter(b -> !b.getBotId().equals(drawingSkill.getBotId()))
					.anyMatch(b -> lineGoalOpponentBot.distanceTo(b.getPos())
							< Geometry.getBotRadius() + Geometry.getBallRadius());
		} else
		{
			p2pVisibilityRedirectOpponentGoal = false;
		}

		return (redirectOpponentBot != null)
				&& (p2pVisibilityRedirectOpponentGoal)
				&& (getBall().getVel().getLength() > chipKickDecisionVelocity);
	}


	protected BotID getBestRedirector(final Map<BotID, ITrackedBot> bots)
	{
		IVector2 ballPos = getBall().getPos();
		IVector2 endPos = getBall().getTrajectory().getPosByVel(0.0).getXYVector();

		BotID minID = null;
		double minDist = Double.MAX_VALUE;

		List<BotID> filteredBots = getPotentialRedirectors(bots, endPos);
		for (BotID key : filteredBots)
		{
			IVector2 pos = bots.get(key).getPos();
			if (VectorMath.distancePP(pos, ballPos) < minDist)
			{
				minDist = VectorMath.distancePP(pos, ballPos);
				minID = key;
			}
		}
		if (minID != null)
		{
			return minID;
		}
		return BotID.noBot();
	}


	private List<BotID> getPotentialRedirectors(final Map<BotID, ITrackedBot> bots, final IVector2 endPos)
	{
		List<BotID> filteredBots = new ArrayList<>();
		final double redirectTol = 350.;
		IVector2 ballPos = getBall().getPos();

		// input: endpoint, ballVel.vel = endpoint - curPos.getAngle().
		IVector2 ballVel = endPos.subtractNew(ballPos);

		if (ballVel.getLength() < 0.4)
		{
			return Collections.emptyList();
		}

		IVector2 left = Vector2.fromAngle(ballVel.getAngle() - 0.2).normalizeNew();
		IVector2 right = Vector2.fromAngle(ballVel.getAngle() + 0.2).normalizeNew();

		double dist = Math.max(VectorMath.distancePP(ballPos, endPos) - redirectTol, 10);

		IVector2 lp = ballPos.addNew(left.multiplyNew(dist));
		IVector2 rp = ballPos.addNew(right.multiplyNew(dist));

		IVector2 normal = ballVel.getNormalVector().normalizeNew();
		IVector2 tleft = ballPos.addNew(normal.scaleToNew(160));
		IVector2 tright = ballPos.addNew(normal.scaleToNew(-160));
		IVector2 uleft = tleft.addNew(left.scaleToNew(dist)).addNew(normal.scaleToNew(100));
		IVector2 uright = tright.addNew(right.scaleToNew(dist)).addNew(normal.scaleToNew(-100));


		Triangle tri1 = Triangle.fromCorners(ballPos, lp, rp);
		Triangle tri2 = Triangle.fromCorners(tleft, uleft, uright);
		Triangle tri3 = Triangle.fromCorners(tleft, tright, uright);

		for (Map.Entry<BotID, ITrackedBot> entry : bots.entrySet())
		{
			BotID botID = entry.getKey();
			ITrackedBot tBot = entry.getValue();
			if (tBot == null)
			{
				continue;
			}
			IVector2 pos = tBot.getPos();
			if (tri2.isPointInShape(pos) || tri3.isPointInShape(pos))
			{
				filteredBots.add(botID);
			}
			if ((drawingSkill != null) && filteredBots.contains(botID))
			{
				drawRedirectorPos(pos);
			}
		}

		if (drawingSkill != null)
		{
			drawRedirectorTriangles(tri1, tri2, tri2);
		}
		return filteredBots;
	}


	private void drawRedirectorTriangles(final Triangle tri1, final Triangle tri2, final Triangle tri3)
	{

		DrawableTriangle dtri1 = new DrawableTriangle(tri1, new Color(255, 0, 0, 20));
		dtri1.setFill(true);
		DrawableTriangle dtri2 = new DrawableTriangle(tri2, new Color(255, 0, 0, 20));
		dtri2.setFill(true);
		DrawableTriangle dtri3 = new DrawableTriangle(tri3, new Color(255, 0, 0, 20));
		dtri3.setFill(true);

		drawingSkill.getShapes().get(ESkillShapesLayer.KEEPER).add(dtri1);
		drawingSkill.getShapes().get(ESkillShapesLayer.KEEPER).add(dtri2);
		drawingSkill.getShapes().get(ESkillShapesLayer.KEEPER).add(dtri3);
	}


	private void drawRedirectorPos(final IVector2 pos)
	{
		drawingSkill.getShapes().get(ESkillShapesLayer.KEEPER).add(new DrawableCircle(pos, 150, Color.CYAN));
	}


	public boolean isGoOutFeasible()
	{
		return (isPositionCloseToPenaltyArea(getBall().getPos()) && isOpponentNearBall());
	}


	public boolean isPositionCloseToPenaltyArea(IVector2 position)
	{
		return Geometry.getPenaltyAreaOur().withMargin(ballDangerZone).isPointInShape(position);
	}


	protected enum ECriticalKeeperStates implements IEvent
	{
		NORMAL,
		INTERCEPT_BALL,
		DEFEND_REDIRECT,
		GO_OUT,
		HAS_CONTACT,
	}
}

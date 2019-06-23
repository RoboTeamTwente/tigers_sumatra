/*
 * Copyright (c) 2009 - 2017, DHBW Mannheim - TIGERs Mannheim
 */

package edu.tigers.sumatra.ai.metis.keeper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.g3force.configurable.Configurable;

import edu.tigers.sumatra.ai.data.BotDistance;
import edu.tigers.sumatra.ai.data.TacticalField;
import edu.tigers.sumatra.ai.data.frames.BaseAiFrame;
import edu.tigers.sumatra.ai.math.AiMath;
import edu.tigers.sumatra.ai.math.OffensiveMath;
import edu.tigers.sumatra.ai.metis.ACalculator;
import edu.tigers.sumatra.geometry.Geometry;
import edu.tigers.sumatra.geometry.Goal;
import edu.tigers.sumatra.ids.BotID;
import edu.tigers.sumatra.math.line.ILine;
import edu.tigers.sumatra.math.line.Line;
import edu.tigers.sumatra.math.line.v2.ILineSegment;
import edu.tigers.sumatra.math.line.v2.Lines;
import edu.tigers.sumatra.math.rectangle.IRectangle;
import edu.tigers.sumatra.math.rectangle.Rectangle;
import edu.tigers.sumatra.math.vector.AVector2;
import edu.tigers.sumatra.math.vector.IVector2;
import edu.tigers.sumatra.math.vector.Vector2;
import edu.tigers.sumatra.math.vector.VectorMath;
import edu.tigers.sumatra.statemachine.IEvent;
import edu.tigers.sumatra.wp.data.ITrackedBot;
import edu.tigers.sumatra.wp.data.ITrackedObject;


/**
 * @author ChrisC
 */
public class KeeperStateCalc extends ACalculator
{
	@Configurable(comment = "Distance from Foe to Ball where Keeper beliefs Foe could be in possession of the ball", defValue = "450")
	private static double foeBotBallPossessionDistance = 450;
	
	@Configurable(comment = "Additional area around PE where FOE with Ball are very dangerous (GoOutState is triggered)", defValue = "360")
	private static double ballDangerZone = 360;
	
	@Configurable(comment = "Ball Speed where Keeper react on his direction", defValue = "0.1")
	private static double blockDecisionVelocity = 0.1;
	
	@Configurable(comment = "offset to the Sides of the goalposts (BalVelIsDirToGoal State)", defValue = "180.0")
	private static double goalAreaOffset = 180;
	
	@Configurable(comment = "Additional Penalty Area margin where Keeper goes into ChipKickState", defValue = "100")
	private static double chipKickDecisionDistance = 100;
	
	@Configurable(comment = "Speed limit of ball of ChipKickState", defValue = "0.8")
	private static double chipKickDecisionVelocity = 0.8;
	
	@Configurable(comment = "Speed limit of ball of PullBackState", defValue = "0.1")
	private static double pullBackDecisionVelocity = 0.1;
	
	@Configurable(comment = "Ball declared as shooted after kick event", defValue = "500")
	private static long maxKickTime = 500;
	
	@Configurable(comment = "Additional margin to PE where single Attacker is in", defValue = "2500.0")
	private static double singleAttackerPenaltyAreaMargin = 2500;
	
	@Configurable(comment = "Check to use pullback when ball is lying close to goal posts")
	private static boolean usePullWhenBallAtGoalPost = true;
	
	private TacticalField newTacticalField;
	private BaseAiFrame baseAiFrame;
	private boolean isFOENearBall;
	private long lastTimeKicked = 0;
	
	
	public static double getPullBackDecisionVelocity()
	{
		return pullBackDecisionVelocity;
	}
	
	
	public static void setPullBackDecisionVelocity(final double pullBackDecisionVelocity)
	{
		KeeperStateCalc.pullBackDecisionVelocity = pullBackDecisionVelocity;
	}
	
	
	@Override
	public void doCalc(final TacticalField newTacticalField, final BaseAiFrame baseAiFrame)
	{
		updateGlobalFields(newTacticalField, baseAiFrame);
		if (isKeeperSet())
		{
			setNextKeeperState();
		}
	}
	
	
	private void updateGlobalFields(final TacticalField newTacticalField, final BaseAiFrame baseAiFrame)
	{
		this.newTacticalField = newTacticalField;
		this.baseAiFrame = baseAiFrame;
		isFOENearBall = isFOENearBall();
	}
	
	
	private boolean isKeeperSet()
	{
		return (baseAiFrame.getKeeperId() != null)
				&& baseAiFrame.getWorldFrame().getBots().containsKey(baseAiFrame.getKeeperId());
	}
	
	
	private void setNextKeeperState()
	{
		// The order in if else represents the priority of the states
		EKeeperState nextState;
		newTacticalField.setBotInterferingKeeperChip(isBotInterferingChip());
		if (newTacticalField.getGameState().isBallPlacement())
		{
			nextState = EKeeperState.BALL_PLACEMENT;
		} else if (newTacticalField.getGameState().isStoppedGame())
		{
			nextState = EKeeperState.STOPPED;
		} else if (isSomeoneShootingAtOurGoal())
		{
			nextState = EKeeperState.INTERCEPT_BALL;
		} else if (isFOERedirecting())
		{
			nextState = EKeeperState.DEFEND_REDIRECT;
		} else if (isChipFastFeasible())
		{
			nextState = EKeeperState.CHIP_FAST;
		} else if (isPullBackUseful() || !isPullBackFinished())
		{
			nextState = EKeeperState.PULL_BACK;
		} else if (isGoOutFeasible())
		{
			nextState = EKeeperState.GO_OUT;
		} else if (isKeeperOutSideOfPenaltyArea())
		{
			nextState = EKeeperState.MOVE_TO_PENALTY_AREA;
		} else
		{
			nextState = EKeeperState.NORMAL;
		}
		newTacticalField.setKeeperState(nextState);
	}
	
	
	private boolean isPullBackFinished()
	{
		double minDistance = 2 * Geometry.getBotRadius();
		double actualDist = getBall().getPos().distanceTo(getWFrame().getTiger(getAiFrame().getKeeperId()).getPos());
		return !baseAiFrame.getPrevFrame().getTacticalField().getKeeperState().equals(EKeeperState.PULL_BACK)
				|| actualDist > minDistance;
	}
	
	
	private boolean isGoOutFeasible()
	{
		return (isBallCloseToPenaltyArea() && isFOENearBall) || isSingleAttacker();
	}
	
	
	private boolean isChipFastFeasible()
	{
		return isBallSaveInPenaltyArea() && !newTacticalField.isBotInterferingKeeperChip()
				&& !isBotBetweenBallAndKeeper() && isPullBackFinished();
	}
	
	
	private boolean isBotBetweenBallAndKeeper()
	{
		ILineSegment keeperBallLine = Lines.segmentFromPoints(getWFrame().getTiger(getAiFrame().getKeeperId()).getPos(),
				getBall().getPos());
		return getWFrame().getBots().values().stream()
				.filter(bot -> !bot.getBotId().equals(getAiFrame().getKeeperId()))
				.map(ITrackedObject::getPos)
				.anyMatch(pos -> keeperBallLine.closestPointOnLine(pos).distanceTo(pos) < Geometry.getBotRadius());
	}
	
	
	private boolean isSingleAttacker()
	{
		List<ITrackedBot> nearFoes = getWFrame().getFoeBots().values().stream()
				.filter(bot -> Geometry.getPenaltyAreaOur().isPointInShape(bot.getPos(), singleAttackerPenaltyAreaMargin))
				.collect(Collectors.toList());
		boolean isSingleAttacker = nearFoes.size() < 2;
		boolean hasFOEBall = false;
		if (!nearFoes.isEmpty())
		{
			hasFOEBall = nearFoes.stream().anyMatch(bot -> bot.getPos().distanceTo(getBall().getPos()) < 1000);
		}
		
		return isSingleAttacker && hasFOEBall;
	}
	
	
	private boolean isPullBackUseful()
	{
		boolean ballNotMoving = isBallInsidePenaltyAreaNotMoving() && newTacticalField.isBotInterferingKeeperChip();
		boolean isBallSave = isBotInterferingChip() && !isFOENearBall
				&& Geometry.getPenaltyAreaOur().isPointInShape(getBall().getPos(), ballDangerZone);
		boolean isBallLyingCloseToGoalPosts = false;
		if (usePullWhenBallAtGoalPost)
		{
			isBallLyingCloseToGoalPosts = isBallInsidePenaltyAreaNotMoving()
					&& isBallCloseToGoalPosts(4 * Geometry.getBotRadius());
		}
		return (ballNotMoving || isBallSave || isBallLyingCloseToGoalPosts) && !isBotBetweenBallAndKeeper();
	}
	
	
	private boolean isFOENearBall()
	{
		BotDistance foeBot = newTacticalField.getEnemyClosestToBall();
		return (foeBot != BotDistance.NULL_BOT_DISTANCE)
				&& (VectorMath.distancePP(foeBot.getBot().getPos(), getBall().getPos()) < foeBotBallPossessionDistance);
	}
	
	
	private boolean isKeeperOutSideOfPenaltyArea()
	{
		IVector2 keeperPos = baseAiFrame.getWorldFrame().getBot(baseAiFrame.getKeeperId()).getPos();
		return !Geometry.getPenaltyAreaOur().isPointInShape(keeperPos);
	}
	
	
	private boolean isBallCloseToPenaltyArea()
	{
		return Geometry.getPenaltyAreaOur().isPointInShape(getBall().getPos(),
				ballDangerZone);
	}
	
	
	private boolean isBallInsidePenaltyAreaNotMoving()
	{
		
		return Geometry.getPenaltyAreaOur().withMargin(chipKickDecisionDistance).isPointInShape(getBall().getPos())
				&& (getBall().getVel().getLength() < pullBackDecisionVelocity);
	}
	
	
	private boolean isBallSaveInPenaltyArea()
	{
		boolean isBallFarFromGoalPost = true;
		if (usePullWhenBallAtGoalPost)
		{
			isBallFarFromGoalPost = !isBallCloseToGoalPosts(3 * Geometry.getBotRadius());
		}
		return Geometry.getPenaltyAreaOur().isPointInShape(getBall().getPos(), chipKickDecisionDistance)
				&& (getBall().getVel().getLength() < chipKickDecisionVelocity)
				&& (isBallFarFromGoalPost || isKeeperBehindBall());
	}
	
	
	private boolean isBallCloseToGoalPosts(double distance)
	{
		return Geometry.getGoalOur().getRightPost().distanceTo(getBall().getPos()) < distance
				|| Geometry.getGoalOur().getLeftPost().distanceTo(getBall().getPos()) < distance;
	}
	
	
	private boolean isKeeperBehindBall()
	{
		Goal goal = Geometry.getGoalOur();
		IVector2 ballPos = getBall().getPos();
		ITrackedBot keeper = getWFrame().getBot(getAiFrame().getKeeperId());
		IRectangle zoneBehindBall = Rectangle.aroundLine(ballPos.subtractNew(Vector2.fromX(Geometry.getBotRadius())),
				ballPos.addNew(Vector2.fromX(-(Geometry.getFieldLength() / 2
						+ ballPos.x() + goal.getDepth() - Geometry.getBotRadius()))),
				Geometry.getBotRadius());
		
		boolean bothBetweenGoalPosts = Math.abs(getBall().getPos().y()) < goal.getWidth() / 2
				&& Math.abs(keeper.getPos().y()) < goal.getWidth() / 2;
		boolean bothBesideRightPost = ballPos.y() < goal.getRightPost().y()
				&& keeper.getPos().y() < goal.getRightPost().y();
		boolean bothBesideLeftPost = ballPos.y() > goal.getLeftPost().y() && keeper.getPos().y() > goal.getLeftPost().y();
		
		return zoneBehindBall.isPointInShape(keeper.getPos())
				&& (bothBesideRightPost || bothBesideLeftPost || bothBetweenGoalPosts);
	}
	
	
	private boolean isBotInterferingChip()
	{
		double minDistance = getBall().getChipConsultant()
				.getMinimumDistanceToOverChip(getBall().getChipConsultant().getInitVelForDistAtTouchdown(1000, 0), 160);
		List<BotDistance> tigersBotDistance = newTacticalField.getTigersToBallDist().stream()
				.filter(botDistance -> !botDistance.getBot().getBotId().equals(getAiFrame().getKeeperId()))
				.filter(botDistance -> botDistance.getDist() - Geometry.getBotRadius() < minDistance)
				.collect(Collectors.toList());
		List<BotDistance> foeBotDistance = newTacticalField.getEnemiesToBallDist().stream()
				.filter(botDistance -> (botDistance.getDist() - Geometry.getBotRadius()) < minDistance)
				.collect(Collectors.toList());
		ILine keeperBallLine = Line.fromPoints(getWFrame().getBot(getAiFrame().getKeeperId()).getPos(),
				getWFrame().getBall().getPos());
		boolean isBotInterferingChip = tigersBotDistance.stream().anyMatch(
				botDistance -> keeperBallLine.distanceTo(botDistance.getBot().getPos()) < Geometry.getBotRadius());
		isBotInterferingChip |= foeBotDistance.stream().anyMatch(
				botDistance -> keeperBallLine.distanceTo(botDistance.getBot().getPos()) < Geometry.getBotRadius());
		return isBotInterferingChip;
	}
	
	
	private boolean isFOERedirecting()
	{
		BotID redirectFOEBotId = OffensiveMath.getBestRedirector(baseAiFrame.getWorldFrame(),
				baseAiFrame.getWorldFrame().getFoeBots());
		IVector2 redirectFOEBot = null;
		if (redirectFOEBotId.isBot())
		{
			redirectFOEBot = baseAiFrame.getWorldFrame().getFoeBot(redirectFOEBotId).getPos();
		}
		
		return (redirectFOEBot != null)
				&& (AiMath.p2pVisibility(baseAiFrame.getWorldFrame().getBots().values(), redirectFOEBot,
						Geometry.getGoalOur().getCenter(), baseAiFrame.getKeeperId()))
				&& (getBall().getVel().getLength() > chipKickDecisionVelocity);
	}
	
	
	private boolean isSomeoneShootingAtOurGoal()
	{
		Optional<IVector2> intersect = getBall().getTrajectory().getTravelLine()
				.intersectionWith(Line.fromDirection(Geometry.getGoalOur().getCenter(), AVector2.Y_AXIS));
		
		if (intersect.isPresent())
		{
			boolean isBallFastEnough = getBall().getTrajectory()
					.getAbsVelByDist(intersect.get().distanceTo(getBall().getPos())) > blockDecisionVelocity;
			boolean isBallOnOurSite = getBall().getVel().x() < 0;
			boolean isBallVelocityIntersectingTheGoalLine = Math.abs(intersect.get().y()) < Math
					.abs(Geometry.getGoalOur().getLeftPost().y() + goalAreaOffset);
			
			if (getWFrame().getKickEvent().isPresent())
			{
				lastTimeKicked = getWFrame().getKickEvent().get().getTimestamp();
			}
			boolean isBallKicked = (lastTimeKicked - getWFrame().getTimestamp()) < maxKickTime;
			boolean isBallLeavingFOE = !isFOENearBall || isBallKicked;
			boolean wasKeeperInPullBackState = getAiFrame().getPrevFrame().getTacticalField()
					.getKeeperState() == EKeeperState.PULL_BACK;
			boolean isBallDangerous = isBallOnOurSite
					&& isBallVelocityIntersectingTheGoalLine
					&& isBallFastEnough
					&& isBallLeavingFOE;
			return isBallDangerous && !wasKeeperInPullBackState;
		}
		return false;
	}
	
	
	/**
	 * This enum represents all possible states of the Keeper
	 */
	public enum EKeeperState implements IEvent
	{
		/** */
		NORMAL,
		/** */
		INTERCEPT_BALL,
		/** */
		DEFEND_REDIRECT,
		/** */
		MOVE_TO_PENALTY_AREA,
		/** */
		CHIP_FAST,
		/** */
		BALL_PLACEMENT,
		/** */
		STOPPED,
		/** */
		GO_OUT,
		/** */
		PULL_BACK
	}
	
}
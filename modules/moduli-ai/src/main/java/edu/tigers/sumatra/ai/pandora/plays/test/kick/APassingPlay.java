/*
 * Copyright (c) 2009 - 2023, DHBW Mannheim - TIGERs Mannheim
 */
package edu.tigers.sumatra.ai.pandora.plays.test.kick;

import com.github.g3force.configurable.Configurable;
import edu.tigers.sumatra.ai.metis.EAiShapesLayer;
import edu.tigers.sumatra.ai.metis.kicking.EBallReceiveMode;
import edu.tigers.sumatra.ai.metis.kicking.Kick;
import edu.tigers.sumatra.ai.metis.kicking.KickFactory;
import edu.tigers.sumatra.ai.metis.kicking.Pass;
import edu.tigers.sumatra.ai.metis.kicking.PassFactory;
import edu.tigers.sumatra.ai.metis.offense.action.OffensiveAction;
import edu.tigers.sumatra.ai.pandora.plays.EPlay;
import edu.tigers.sumatra.ai.pandora.roles.ARole;
import edu.tigers.sumatra.ai.pandora.roles.move.MoveRole;
import edu.tigers.sumatra.ai.pandora.roles.offense.PassReceiverRole;
import edu.tigers.sumatra.ai.pandora.roles.offense.attacker.AttackerRole;
import edu.tigers.sumatra.ai.pandora.roles.placement.BallPlacementRole;
import edu.tigers.sumatra.botmanager.botskills.data.EKickerDevice;
import edu.tigers.sumatra.drawable.DrawableCircle;
import edu.tigers.sumatra.geometry.Geometry;
import edu.tigers.sumatra.ids.BotID;
import edu.tigers.sumatra.math.circle.Circle;
import edu.tigers.sumatra.math.line.LineMath;
import edu.tigers.sumatra.math.vector.IVector2;
import edu.tigers.sumatra.statemachine.AState;
import edu.tigers.sumatra.wp.data.KickedBall;
import lombok.Setter;
import org.apache.commons.lang.Validate;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * Test redirecting with multiple bots
 */
public abstract class APassingPlay extends ABallPreparationPlay
{
	@Configurable(defValue = "1.57", comment = "Max allowed redirect angle (rad)")
	private static double maxAllowedRedirectAngle = 1.57;

	private final PassFactory passFactory = new PassFactory();
	private final KickFactory kickFactory = new KickFactory();

	@Setter
	private double maxDistBall2PassStart = 500;
	@Setter
	private double minPassDuration;
	@Setter
	private EKickerDevice kickerDevice = EKickerDevice.STRAIGHT;

	protected List<IVector2> origins = Collections.emptyList();
	private Map<BotID, IVector2> botIdToOriginMap = Collections.emptyMap();
	protected Map<IVector2, BotID> originToBotIdMap = Collections.emptyMap();


	protected APassingPlay(EPlay playType)
	{
		super(playType);

		setExecutionState(new ExecutionState());
	}


	protected abstract List<IVector2> getOrigins();


	protected abstract IVector2 getReceiverTarget(IVector2 origin);


	protected abstract double getMaxReceivingBallSpeed(IVector2 origin);


	protected IVector2 getReceiverCatchPoint(IVector2 origin)
	{
		var kickAge = getWorldFrame().getKickedBall()
				.map(KickedBall::getKickTimestamp)
				.map(ts -> (getWorldFrame().getTimestamp() - ts) / 1e9)
				.orElse(Double.POSITIVE_INFINITY);
		if (kickAge > 0.2)
		{
			return getBall().getTrajectory().closestPointTo(origin);
		}
		return origin;
	}


	protected BotID getReceiverBot(IVector2 origin)
	{
		var target = getReceiverTarget(origin);
		return originToBotIdMap.get(target);
	}


	protected abstract EReceiveMode getReceiveMode(IVector2 origin);


	@SuppressWarnings("squid:S1172") // unused parameter is used in overloaded methods
	protected boolean doGoalKick(IVector2 origin)
	{
		return false;
	}


	protected void updateDuringExecution()
	{
	}


	@Override
	protected boolean ready()
	{
		return getRoles().size() > 1;
	}


	@Override
	protected void doUpdateBeforeRoles()
	{
		passFactory.update(getWorldFrame());
		kickFactory.update(getWorldFrame());

		origins = getOrigins();
		botIdToOriginMap = mapDestinationsToRoles(origins);
		originToBotIdMap = botIdToOriginMap.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
		drawReceivingPositions();
		updateBallTargetPos();

		super.doUpdateBeforeRoles();

		if (origins.size() != getRoles().size())
		{
			return;
		}

		// set waiting positions
		findRoles(MoveRole.class).forEach(this::updateWaitingDestination);

		findRoles(AttackerRole.class).forEach(this::updateAttacker);

		// set default outgoing kicks
		findRoles(PassReceiverRole.class).forEach(this::updatePassReceiver);

		var allReceivingPosReachedIn = findRoles(PassReceiverRole.class).stream()
				.mapToDouble(PassReceiverRole::getReceivingPositionReachedIn)
				.max()
				.orElse(0.0);
		findRoles(AttackerRole.class).forEach(r -> r.setWaitForKick(allReceivingPosReachedIn > 0.2));
	}


	private void updatePassReceiver(PassReceiverRole passReceiverRole)
	{
		passReceiverRole.setPhysicalObstaclesOnly(true);

		var origin = botIdToOriginMap.get(passReceiverRole.getBotID());
		var target = getReceiverTarget(origin);
		var receiver = getReceiverBot(origin);
		var receiveMode = getReceiveMode(origin);

		if (receiveMode == EReceiveMode.RECEIVE)
		{
			passReceiverRole.setOutgoingKick(null);
		} else
		{
			// Set the outgoing kick for the pass receiver
			// This pass will not be really be performed, it is just for the pass receiver to look to the right direction.
			var pass = passFactory.straight(
					passReceiverRole.getIncomingPass().getKick().getTarget(),
					target,
					passReceiverRole.getBotID(),
					receiver,
					minPassDuration,
					0.0,
					EBallReceiveMode.DONT_CARE
			);
			passReceiverRole.setOutgoingKick(pass.getKick());
		}
	}


	private void updateAttacker(AttackerRole attackerRole)
	{
		attackerRole.setPhysicalObstaclesOnly(true);

		var origin = botIdToOriginMap.get(attackerRole.getBotID());
		var receiveMode = getReceiveMode(origin);
		if (receiveMode == EReceiveMode.REDIRECT && doGoalKick(origin))
		{
			attackerRole.setAction(OffensiveAction.buildKick(createGoalKick(origin)));

			// set default incoming passes
			findRoles(PassReceiverRole.class)
					.forEach(r -> r.setIncomingPass(null));
		} else
		{
			var pass = createPass(attackerRole, origin);
			var source = pass.getKick().getSource();
			var target = pass.getKick().getTarget();
			var redirectAngle = getBall().getPos().subtractNew(source).angleToAbs(
					target.subtractNew(source)
			).orElse(0.0);

			if ((receiveMode == EReceiveMode.REDIRECT && redirectAngle < maxAllowedRedirectAngle)
					|| getBall().getVel().getLength2() < 0.5)
			{
				attackerRole.setAction(OffensiveAction.buildPass(pass));
			} else
			{
				attackerRole.setAction(OffensiveAction.buildReceive(pass.getKick().getSource()));
			}

			// set default incoming passes
			findRoles(PassReceiverRole.class)
					.forEach(r -> r.setIncomingPass(pass));
		}
	}


	private Pass createPass(AttackerRole attackerRole, IVector2 origin)
	{
		passFactory.setMaxReceivingBallSpeed(getMaxReceivingBallSpeed(origin));
		var source = getReceiverCatchPoint(origin);
		var target = getReceiverTarget(origin);
		var receiver = getReceiverBot(origin);
		if (kickerDevice == EKickerDevice.STRAIGHT)
		{
			return passFactory.straight(
					source,
					target,
					attackerRole.getBotID(),
					receiver,
					minPassDuration,
					0.0,
					EBallReceiveMode.DONT_CARE
			);
		}
		return passFactory.chip(
				source,
				target,
				attackerRole.getBotID(),
				receiver,
				minPassDuration,
				0.0,
				EBallReceiveMode.DONT_CARE
		);
	}


	private Kick createGoalKick(IVector2 origin)
	{
		var source = getReceiverCatchPoint(origin);
		var target = getReceiverTarget(origin);
		return kickFactory.goalKick(source, target);
	}


	private Map<BotID, IVector2> mapDestinationsToRoles(final List<IVector2> destinations)
	{
		Map<BotID, IVector2> mappedDestinations = new HashMap<>();
		List<ARole> remainingRoles = new ArrayList<>(getRoles());

		for (var dest : destinations)
		{
			var bestRole = remainingRoles.stream()
					.min(Comparator.comparing(role -> role.getPos().distanceTo(dest)))
					.map(ARole::getBotID);
			bestRole.ifPresent(r -> mappedDestinations.put(r, dest));
			bestRole.ifPresent(id -> remainingRoles.removeIf(role -> Objects.equals(role.getBotID(), id)));
		}
		return mappedDestinations;
	}


	private void drawReceivingPositions()
	{
		var shapes = getAiFrame().getShapeMap().get(EAiShapesLayer.TEST_PASSING);
		origins.stream()
				.map(p -> new DrawableCircle(Circle.createCircle(p, Geometry.getBallRadius() + 20), Color.green))
				.forEach(shapes::add);
	}


	private void updateWaitingDestination(MoveRole moveRole)
	{
		var origin = botIdToOriginMap.get(moveRole.getBotID());
		var target = getBall().getPos();
		var dist = Geometry.getBallRadius() + moveRole.getBot().getCenter2DribblerDist();
		var dest = LineMath.stepAlongLine(origin, target, -dist);
		moveRole.updateDestination(dest);
		moveRole.updateLookAtTarget(target);
		moveRole.getMoveCon().physicalObstaclesOnly();
	}


	private void updateBallTargetPos()
	{
		findRoles(BallPlacementRole.class).stream().findAny()
				.map(ARole.class::cast)
				.or(() -> Optional.ofNullable(getBestRoleForBall()))
				.map(ARole::getBotID)
				.map(this::getBallPlacementPos)
				.ifPresent(this::setBallTargetPos);
	}


	private IVector2 getBallPlacementPos(BotID botID)
	{
		return botIdToOriginMap.get(botID);
	}


	private ARole getBestRoleForBall()
	{
		if (getBall().getVel().getLength2() < 0.5)
		{
			return getClosestToBallTrajectory();
		}
		return getRoles().stream()
				.filter(role -> getBall().getTrajectory().getTravelLine().isPointInFront(role.getPos()))
				.min(Comparator.comparing(role -> getBall().getTrajectory().distanceTo(role.getBot().getBotKickerPos())))
				.orElseGet(this::getClosestToBallTrajectory);
	}


	private ARole getClosestToBallTrajectory()
	{
		return getRoles().stream()
				.min(Comparator.comparing(role -> getBall().getTrajectory().distanceTo(role.getBot().getBotKickerPos())))
				.orElse(null);
	}


	private class ExecutionState extends AState
	{
		@Override
		public void doUpdate()
		{
			if (!Geometry.getField().isPointInShape(getBall().getPos())
					|| (getBall().getVel().getLength2() < 0.5
					&& getBallTargetPos().distanceTo(getBall().getPos()) > maxDistBall2PassStart))
			{
				stopExecution();
				return;
			}

			var nearest2BallRole = reassignRole(getBestRoleForBall(), AttackerRole.class, AttackerRole::new);
			var origin = botIdToOriginMap.get(nearest2BallRole.getBotID());
			var receiver = getReceiverBot(origin);
			Validate.isTrue(!nearest2BallRole.getBotID().equals(receiver));
			Objects.requireNonNull(receiver);
			var passReceiverRole = getRoles().stream()
					.filter(r -> Objects.equals(r.getBotID(), receiver))
					.findFirst()
					.map(r -> reassignRole(r, PassReceiverRole.class, PassReceiverRole::new))
					.orElseThrow();
			allRolesExcept(nearest2BallRole, passReceiverRole)
					.forEach(r -> reassignRole(r, MoveRole.class, MoveRole::new));

			updateDuringExecution();
		}
	}


	public enum EReceiveMode
	{
		REDIRECT,
		RECEIVE,
	}
}

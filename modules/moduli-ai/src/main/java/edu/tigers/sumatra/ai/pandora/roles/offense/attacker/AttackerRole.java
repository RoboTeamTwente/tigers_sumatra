/*
 * Copyright (c) 2009 - 2023, DHBW Mannheim - TIGERs Mannheim
 */

package edu.tigers.sumatra.ai.pandora.roles.offense.attacker;

import com.github.g3force.configurable.Configurable;
import edu.tigers.sumatra.ai.metis.EAiShapesLayer;
import edu.tigers.sumatra.ai.metis.kicking.Kick;
import edu.tigers.sumatra.ai.metis.kicking.Pass;
import edu.tigers.sumatra.ai.metis.offense.action.EOffensiveActionType;
import edu.tigers.sumatra.ai.metis.offense.action.OffensiveAction;
import edu.tigers.sumatra.ai.metis.offense.dribble.EDribblingCondition;
import edu.tigers.sumatra.ai.metis.offense.situation.rating.EOffensiveExecutionStatus;
import edu.tigers.sumatra.ai.pandora.roles.ARole;
import edu.tigers.sumatra.ai.pandora.roles.ERole;
import edu.tigers.sumatra.ai.pandora.roles.offense.attacker.states.ApproachAndStopBallState;
import edu.tigers.sumatra.ai.pandora.roles.offense.attacker.states.ApproachBallLineState;
import edu.tigers.sumatra.ai.pandora.roles.offense.attacker.states.ChopTrickState;
import edu.tigers.sumatra.ai.pandora.roles.offense.attacker.states.DribbleState;
import edu.tigers.sumatra.ai.pandora.roles.offense.attacker.states.DribblingKickState;
import edu.tigers.sumatra.ai.pandora.roles.offense.attacker.states.EAttackerState;
import edu.tigers.sumatra.ai.pandora.roles.offense.attacker.states.FreeKickState;
import edu.tigers.sumatra.ai.pandora.roles.offense.attacker.states.KickState;
import edu.tigers.sumatra.ai.pandora.roles.offense.attacker.states.ProtectState;
import edu.tigers.sumatra.ai.pandora.roles.offense.attacker.states.ReceiveState;
import edu.tigers.sumatra.ai.pandora.roles.offense.attacker.states.RedirectState;
import edu.tigers.sumatra.drawable.DrawableAnnotation;
import edu.tigers.sumatra.geometry.Geometry;
import edu.tigers.sumatra.math.vector.Vector2;
import edu.tigers.sumatra.skillsystem.skills.ESkillState;
import edu.tigers.sumatra.statemachine.IState;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.awt.Color;


/**
 * Kick the still or moving ball.
 */
@Log4j2
public class AttackerRole extends ARole
{
	@Configurable(defValue = "2000.0")
	private static double switchToKickDist = 2000;

	@Configurable(defValue = "0.85")
	private static double maxBallVel = 0.85;

	@Getter
	@Setter
	private OffensiveAction action;
	@Setter
	@Getter
	private boolean useSingleTouch;
	@Setter
	@Getter
	private boolean waitForKick;
	@Setter
	@Getter
	private boolean physicalObstaclesOnly;

	private IState previousState;

	ChopTrickState chopTrickState;


	public AttackerRole()
	{
		super(ERole.ATTACKER);

		var protectState = new ProtectState(this);
		var approachAndStopBallState = new ApproachAndStopBallState(this);
		var approachBallLineState = new ApproachBallLineState(this);
		var kickState = new KickState(this);
		var freeKickState = new FreeKickState(this);
		var receiveState = new ReceiveState(this);
		var redirectState = new RedirectState(this);
		var dribblingKickState = new DribblingKickState(this);
		var dribbleState = new DribbleState(this);
		chopTrickState = new ChopTrickState(this);
		setInitialState(protectState);

		// switch from protect
		protectState.addTransition(this::ballMoves, approachBallLineState);
		protectState.addTransition(this::switchToKick, kickState);
		protectState.addTransition(ESkillState.SUCCESS, this::switchToDribble, dribbleState);

		// switch from dribble
		dribbleState.addTransition(this::switchToDribbleKick, dribblingKickState);
		dribbleState.addTransition(this::switchToKick, kickState);
		dribbleState.addTransition(this::dribbleFailed, protectState);
		dribbleState.addTransition(ESkillState.FAILURE, protectState);

		// switch from approach ball line
		approachBallLineState.addTransition(ESkillState.SUCCESS, receiveState);
		approachBallLineState.addTransition(ESkillState.FAILURE, approachAndStopBallState);
		approachBallLineState.addTransition(this::closeToBall, approachAndStopBallState);

		// switch from approach and stop
		approachAndStopBallState.addTransition(ESkillState.SUCCESS, protectState);
		approachAndStopBallState.addTransition(ESkillState.FAILURE, protectState);

		// switch from kick
		kickState.addTransition(ESkillState.SUCCESS, approachBallLineState);
		kickState.addTransition(ESkillState.FAILURE, protectState);
		kickState.addTransition(this::kickStateIsInvalid, protectState);
		kickState.addTransition(() -> waitForKick || useSingleTouch, freeKickState);

		// switch from free kick
		freeKickState.addTransition(ESkillState.SUCCESS, approachBallLineState);
		freeKickState.addTransition(ESkillState.FAILURE, protectState);

		// switch from receive
		receiveState.addTransition(ESkillState.SUCCESS, protectState);
		receiveState.addTransition(ESkillState.FAILURE, protectState);
		receiveState.addTransition(this::switchToRedirect, redirectState);

		// switch from redirect
		redirectState.addTransition(ESkillState.SUCCESS, approachBallLineState);
		redirectState.addTransition(ESkillState.FAILURE, protectState);
		redirectState.addTransition(this::switchToReceive, receiveState);

		// switch from dribbling kick
		dribblingKickState.addTransition(this::dribblingKickIsBlocked, protectState);
		dribblingKickState.addTransition(ESkillState.FAILURE, protectState);

		// switch to and from chop trick are handled by global transitions
	}


	private boolean switchToDribble()
	{
		return action.getDribbleToPos() != null
				&& action.getDribbleToPos().getProtectFromPos() != null;
	}


	private boolean dribbleFailed()
	{
		return action.getType() != EOffensiveActionType.PROTECT
				&& action.getType() != EOffensiveActionType.DRIBBLE_KICK;
	}


	private boolean kickStateIsInvalid()
	{
		return action.getType() != EOffensiveActionType.KICK && action.getType() != EOffensiveActionType.PASS;
	}


	private boolean dribblingKickIsBlocked()
	{
		return action.getDribbleToPos() == null
				|| action.getDribbleToPos().getDribblingCondition() != EDribblingCondition.DRIBBLING_KICK;
	}


	private boolean switchToDribbleKick()
	{
		return action.getDribbleToPos() != null && (action.getDribbleToPos().getDribblingCondition()
				== EDribblingCondition.DRIBBLING_KICK);
	}


	public EOffensiveExecutionStatus getExecutionStatus()
	{
		boolean switchedState = previousState != getCurrentState();
		previousState = getCurrentState();
		if (getPos().distanceTo(getBall().getPos()) > 200 || switchedState)
		{
			return EOffensiveExecutionStatus.GETTING_READY;
		}
		return EOffensiveExecutionStatus.IMMINENT;
	}


	private boolean switchToRedirect()
	{
		return ballMoves() && (action.getType() == EOffensiveActionType.REDIRECT_KICK
				|| action.getType() == EOffensiveActionType.PASS);
	}


	private boolean switchToReceive()
	{
		return action.getType() == EOffensiveActionType.RECEIVE
				|| (action.getType() != EOffensiveActionType.REDIRECT_KICK
				&& action.getType() != EOffensiveActionType.PASS);
	}


	private boolean switchToKick()
	{
		boolean ballContactOrNoOpponentNearBall =
				getBot().getBallContact().hasContactFromVisionOrBarrier()
						|| getBot().getBallContact().hadContact(0.2)
						|| getAiFrame().getTacticalField().getOpponentClosestToBall().getDist()
						> Geometry.getBotRadius() * 1.5;
		boolean validKickAndIsClose =
				action.getKick() != null && getBall().getPos().distanceTo(getPos()) < switchToKickDist
						&& action.getDribbleToPos() == null;
		boolean kickOrPass =
				action.getType() == EOffensiveActionType.KICK || action.getType() == EOffensiveActionType.PASS;
		return validKickAndIsClose && ballContactOrNoOpponentNearBall && kickOrPass;
	}


	private boolean ballMoves()
	{
		return getBall().getVel().getLength2() > maxBallVel;
	}


	private boolean closeToBall()
	{
		return getBall().getPos().distanceTo(getPos()) < 200;
	}


	@Override
	protected void beforeUpdate()
	{
		super.beforeUpdate();
		if (action.getType() == EOffensiveActionType.CHOP_TRICK)
		{
			triggerEvent(EAttackerState.CHOP_TRICK);
		}
	}


	@Override
	protected void afterUpdate()
	{
		Pass pass = action.getPass();
		Kick kick = action.getKick();
		if (pass != null)
		{
			var color = getAiFrame().getTeamColor().getColor();
			var drawables = pass.createDrawables();
			drawables.forEach(d -> d.setColor(color));
			getShapes(EAiShapesLayer.OFFENSIVE_ATTACKER).addAll(drawables);
		} else if (kick != null)
		{
			var drawables = kick.createDrawables();
			drawables.forEach(d -> d.setColor(Color.red));
			getShapes(EAiShapesLayer.OFFENSIVE_ATTACKER).addAll(drawables);
		}

		if (isWaitForKick())
		{
			getShapes(EAiShapesLayer.OFFENSIVE_ATTACKER)
					.add(new DrawableAnnotation(getPos(), "Waiting")
							.withCenterHorizontally(true)
							.withOffset(Vector2.fromY(100))
							.setColor(Color.red));
		}
	}
}

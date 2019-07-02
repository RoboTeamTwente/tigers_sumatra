/*
 * Copyright (c) 2009 - 2018, DHBW Mannheim - TIGERs Mannheim
 */

package edu.tigers.sumatra.ai.pandora.roles.offense.attacker;

import java.util.Objects;

import com.github.g3force.configurable.ConfigRegistration;
import com.github.g3force.configurable.Configurable;

import edu.tigers.sumatra.ai.metis.offense.action.EOffensiveAction;
import edu.tigers.sumatra.ai.pandora.roles.ARole;
import edu.tigers.sumatra.skillsystem.skills.ProtectBallSkill;
import edu.tigers.sumatra.wp.data.DynamicPosition;
import edu.tigers.sumatra.wp.data.ITrackedBot;


public class ProtectBallState extends AOffensiveState
{
	@Configurable(defValue = "2.0")
	private static double protectionDistanceGain = 2.0;

	@Configurable(defValue = "0.5")
	private static double maxBallVel = 0.5;

	private DynamicPosition protectionTarget;
	private ProtectBallSkill skill;

	static
	{
		ConfigRegistration.registerClass("roles", ProtectBallState.class);
	}


	public ProtectBallState(final ARole role)
	{
		super(role);
	}


	@Override
	public void doEntryActions()
	{
		ITrackedBot opponentBot = getAiFrame().getTacticalField().getEnemyClosestToBall().getBot();
		Objects.requireNonNull(opponentBot, "Protecting ball without opponents seems useless");
		protectionTarget = new DynamicPosition(opponentBot);
		skill = new ProtectBallSkill(protectionTarget);
		setNewSkill(skill);
	}


	@Override
	public void doUpdate()
	{
		protectionTarget.update(getAiFrame().getTacticalField().getEnemyClosestToBall().getBot().getBotId());
		skill.setMarginToTheirPenArea(getMarginToTheirPenArea());
		boolean velTooBig = getBall().getVel().getLength2() > maxBallVel;
		boolean foundSuitableStrategy = getAiFrame().getTacticalField().getOffensiveActions().get(getBotID())
				.getAction() != EOffensiveAction.PROTECT;
		boolean ballSaved = !ballPossessionIsThreatened(protectionDistanceGain);

		if (velTooBig)
		{
			triggerEvent(EBallHandlingEvent.BALL_MOVES);
		} else if (foundSuitableStrategy && ballSaved)
		{
			triggerEvent(EBallHandlingEvent.FOUND_SUITABLE_STRATEGY);
		}
	}
}

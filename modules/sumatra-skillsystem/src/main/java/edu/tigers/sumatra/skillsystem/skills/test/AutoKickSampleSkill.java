/*
 * Copyright (c) 2009 - 2018, DHBW Mannheim - TIGERs Mannheim
 */

package edu.tigers.sumatra.skillsystem.skills.test;

import edu.tigers.sumatra.botmanager.botskills.BotSkillLocalVelocity;
import edu.tigers.sumatra.botmanager.botskills.BotSkillMotorsOff;
import edu.tigers.sumatra.botmanager.botskills.data.EKickerDevice;
import edu.tigers.sumatra.botmanager.botskills.data.EKickerMode;
import edu.tigers.sumatra.geometry.Geometry;
import edu.tigers.sumatra.math.line.LineMath;
import edu.tigers.sumatra.math.vector.IVector2;
import edu.tigers.sumatra.math.vector.Vector2;
import edu.tigers.sumatra.skillsystem.ESkill;
import edu.tigers.sumatra.skillsystem.skills.AMoveToSkill;
import edu.tigers.sumatra.skillsystem.skills.MoveToState;
import edu.tigers.sumatra.skillsystem.skills.util.AroundBallCalc;
import edu.tigers.sumatra.skillsystem.skills.util.BallStabilizer;
import edu.tigers.sumatra.statemachine.AState;
import edu.tigers.sumatra.statemachine.IEvent;
import edu.tigers.sumatra.time.TimestampTimer;
import edu.tigers.sumatra.wp.data.DynamicPosition;


/**
 * Kick with a single touch (taking care to not double touching the ball)
 */
public class AutoKickSampleSkill extends AMoveToSkill
{
	private final BallStabilizer ballStabilizer = new BallStabilizer();
	
	private DynamicPosition target;
	private double kickDuration;
	private EKickerDevice device;
	
	
	/**
	 * UI constructor
	 *
	 * @param target
	 * @param device
	 * @param kickDuration
	 */
	@SuppressWarnings("unused") // used by UI
	public AutoKickSampleSkill(
			final DynamicPosition target,
			final EKickerDevice device,
			final double kickDuration)
	{
		super(ESkill.AUTO_KICK_SAMPLE);
		this.target = target;
		this.device = device;
		this.kickDuration = kickDuration;
		final PrepareState prepareState = new PrepareState();
		final KickState kickState = new KickState();
		final CalmDownState calmDownState = new CalmDownState();
		setInitialState(prepareState);
		addTransition(EEvent.KICK_DONE, prepareState);
		addTransition(EEvent.PREPARED, calmDownState);
		addTransition(EEvent.CALMED_DOWN, kickState);
	}
	
	
	@Override
	protected void beforeStateUpdate()
	{
		super.beforeStateUpdate();
		ballStabilizer.update(getBall(), getTBot());
	}
	
	
	private enum EEvent implements IEvent
	{
		PREPARED,
		KICK_DONE,
		CALMED_DOWN,
	}
	
	private class PrepareState extends MoveToState
	{
		
		
		private PrepareState()
		{
			super(AutoKickSampleSkill.this);
		}
		
		
		@Override
		public void doEntryActions()
		{
			super.doEntryActions();
			getMoveCon().setBallObstacle(false);
		}
		
		
		@Override
		public void doUpdate()
		{
			IVector2 dest = AroundBallCalc
					.aroundBall()
					.withBallPos(getBallPos())
					.withTBot(getTBot())
					.withDestination(getDestination(20))
					.withMaxMargin(120)
					.withMinMargin(20)
					.build()
					.getAroundBallDest();
			
			getMoveCon().updateDestination(dest);
			double targetOrientation = target.getPos().subtractNew(getBallPos()).getAngle(0);
			getMoveCon().updateTargetAngle(targetOrientation);
			super.doUpdate();
			
			if (getPos().distanceTo(dest) < 10)
			{
				triggerEvent(EEvent.PREPARED);
			}
		}
		
		
		private IVector2 getDestination(double margin)
		{
			return LineMath.stepAlongLine(getBallPos(), target.getPos(), -getDistance(margin));
		}
		
		
		private double getDistance(double margin)
		{
			return getTBot().getCenter2DribblerDist() + Geometry.getBallRadius() + margin;
		}
		
		
		private IVector2 getBallPos()
		{
			return ballStabilizer.getBallPos();
		}
	}
	
	private class CalmDownState extends AState
	{
		TimestampTimer timer = new TimestampTimer(1.0);
		
		
		@Override
		public void doEntryActions()
		{
			getMatchCtrl().setSkill(new BotSkillMotorsOff());
			getBot().setCurrentTrajectory(null);
			timer.reset();
		}
		
		
		@Override
		public void doUpdate()
		{
			timer.update(getWorldFrame().getTimestamp());
			if (timer.isTimeUp(getWorldFrame().getTimestamp()))
			{
				triggerEvent(EEvent.CALMED_DOWN);
			}
		}
	}
	
	private class KickState extends AState
	{
		private long sampleTimeMs = 2000; // [ms]
		private long startTime;
		
		
		@Override
		public void doEntryActions()
		{
			startTime = getWorldFrame().getTimestamp();
			BotSkillLocalVelocity loc = new BotSkillLocalVelocity(Vector2.fromXY(0, 0.1), 0,
					getMoveCon().getMoveConstraints());
			loc.getKickerDribbler().setKick(kickDuration, device, EKickerMode.ARM_TIME);
			getMatchCtrl().setSkill(loc);
			getBot().setCurrentTrajectory(null);
		}
		
		
		@Override
		public void doUpdate()
		{
			if (((getWorldFrame().getTimestamp() - startTime) * 1e-6) > 1000.0)
			{
				getMatchCtrl().setSkill(new BotSkillMotorsOff());
			}
			
			if (((getWorldFrame().getTimestamp() - startTime) * 1e-6) > sampleTimeMs)
			{
				triggerEvent(EEvent.KICK_DONE);
			}
		}
	}
}

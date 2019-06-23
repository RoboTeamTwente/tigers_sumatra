/*
 * Copyright (c) 2009 - 2017, DHBW Mannheim - TIGERs Mannheim
 */

package edu.tigers.sumatra.skillsystem.skills;

import java.awt.Color;

import org.apache.log4j.Logger;

import com.github.g3force.configurable.Configurable;

import edu.tigers.sumatra.ai.data.BotAiInformation;
import edu.tigers.sumatra.bot.MoveConstraints;
import edu.tigers.sumatra.botmanager.bots.ABot;
import edu.tigers.sumatra.botmanager.commands.botskills.AMoveBotSkill;
import edu.tigers.sumatra.botmanager.commands.botskills.BotSkillFastGlobalPosition;
import edu.tigers.sumatra.botmanager.commands.botskills.BotSkillGlobalPosition;
import edu.tigers.sumatra.botmanager.commands.botskills.BotSkillGlobalVelocity;
import edu.tigers.sumatra.botmanager.commands.botskills.BotSkillLocalVelocity;
import edu.tigers.sumatra.botmanager.commands.botskills.BotSkillMotorsOff;
import edu.tigers.sumatra.botmanager.commands.botskills.data.KickerDribblerCommands;
import edu.tigers.sumatra.drawable.DrawableAnnotation;
import edu.tigers.sumatra.drawable.DrawableCircle;
import edu.tigers.sumatra.geometry.Geometry;
import edu.tigers.sumatra.ids.EAiTeam;
import edu.tigers.sumatra.math.AngleMath;
import edu.tigers.sumatra.math.BotMath;
import edu.tigers.sumatra.math.vector.IVector2;
import edu.tigers.sumatra.math.vector.IVector3;
import edu.tigers.sumatra.math.vector.Vector2;
import edu.tigers.sumatra.pathfinder.TrajectoryGenerator;
import edu.tigers.sumatra.referee.data.GameState;
import edu.tigers.sumatra.skillsystem.ESkill;
import edu.tigers.sumatra.skillsystem.ESkillShapesLayer;
import edu.tigers.sumatra.trajectory.ITrajectory;
import edu.tigers.sumatra.trajectory.TrajectoryWithTime;
import edu.tigers.sumatra.trajectory.TrajectoryXyw;
import edu.tigers.sumatra.wp.data.ITrackedBall;
import edu.tigers.sumatra.wp.data.ITrackedBot;
import edu.tigers.sumatra.wp.data.TrackedBot;
import edu.tigers.sumatra.wp.data.WorldFrame;
import edu.tigers.sumatra.wp.data.WorldFrameWrapper;


/**
 * The base class for all move-skills.
 *
 * @author NicolaiO
 */
public abstract class AMoveSkill extends ASkill
{
	@SuppressWarnings("unused")
	private static final Logger	log						= Logger.getLogger(AMoveSkill.class.getName());
	
	@Configurable(comment = "This tolerance is subtracted from the default bot speed that is required on STOP")
	private static double			stopSpeedTolerance	= 0.2;
	
	private WorldFrame				worldFrame				= null;
	private GameState					gameState				= GameState.HALT;
	private ITrackedBot				tBot						= null;
	
	
	protected AMoveSkill(final ESkill skillName)
	{
		super(skillName);
	}
	
	
	protected final AMoveBotSkill setTargetPose(final IVector2 destination, final double targetAngle)
	{
		return setTargetPose(destination, targetAngle, getMoveCon().getMoveConstraints());
	}
	
	
	protected final AMoveBotSkill setTargetPose(final IVector2 destination, final double targetAngle,
			final MoveConstraints moveConstraints)
	{
		ITrajectory<IVector2> traj2d = TrajectoryGenerator.generatePositionTrajectory(getTBot(), destination,
				moveConstraints);
		ITrajectory<IVector3> trajectory = new TrajectoryXyw(traj2d,
				TrajectoryGenerator.generateRotationTrajectoryStub(getMoveCon().getTargetAngle()));
		return setTargetPose(destination, targetAngle, getMoveCon().getMoveConstraints(), trajectory);
	}
	
	
	protected final AMoveBotSkill setTargetPose(final IVector2 destination, final double targetAngle,
			final MoveConstraints moveConstraints,
			final ITrajectory<IVector3> trajectory)
	{
		IVector2 dest = destination;
		double orient = targetAngle;
		if (getWorldFrame().isInverted())
		{
			dest = dest.multiplyNew(-1);
			orient = AngleMath.normalizeAngle(orient + AngleMath.PI);
		}
		
		AMoveBotSkill skill;
		if (getMoveCon().isFastPosMode())
		{
			skill = new BotSkillFastGlobalPosition(dest, orient, moveConstraints);
		} else
		{
			skill = new BotSkillGlobalPosition(dest, orient, moveConstraints);
		}
		getMatchCtrl().setSkill(skill);
		updateKickerDribbler(skill.getKickerDribbler());
		
		TrajectoryWithTime<IVector3> twt = new TrajectoryWithTime<>(trajectory, getWorldFrame().getTimestamp());
		getBot().setCurrentTrajectory(twt);
		
		return skill;
	}
	
	
	protected final BotSkillLocalVelocity setLocalVelocity(final IVector2 vel, final double rot,
			final MoveConstraints moveConstraints)
	{
		BotSkillLocalVelocity skill = new BotSkillLocalVelocity(vel, rot, moveConstraints);
		getMatchCtrl().setSkill(skill);
		updateKickerDribbler(skill.getKickerDribbler());
		return skill;
	}
	
	
	protected final BotSkillLocalVelocity setLocalVelFromGlobalVel(final IVector2 vel, final double rot,
			final MoveConstraints moveConstraints)
	{
		IVector2 locVel = BotMath.convertGlobalBotVector2Local(vel, getAngle());
		return setLocalVelocity(locVel, rot, moveConstraints);
	}
	
	
	protected final BotSkillGlobalVelocity setGlobalVelocity(final IVector2 vel, final double rot,
			final MoveConstraints moveConstraints)
	{
		BotSkillGlobalVelocity skill;
		if (getWorldFrame().isInverted())
		{
			skill = new BotSkillGlobalVelocity(vel.multiplyNew(-1), rot, moveConstraints);
		} else
		{
			skill = new BotSkillGlobalVelocity(vel, rot, moveConstraints);
		}
		getMatchCtrl().setSkill(skill);
		updateKickerDribbler(skill.getKickerDribbler());
		return skill;
	}
	
	
	protected final BotSkillMotorsOff setMotorsOff()
	{
		BotSkillMotorsOff skill = new BotSkillMotorsOff();
		getMatchCtrl().setSkill(skill);
		updateKickerDribbler(skill.getKickerDribbler());
		return skill;
	}
	
	
	@Override
	protected final void doCalcActionsBeforeStateUpdate()
	{
		getMoveCon().update(getWorldFrame(), getTBot());
		
		beforeStateUpdate();
	}
	
	
	@Override
	protected final void doCalcActionsAfterStateUpdate()
	{
		afterStateUpdate();
		
		if (getGameState().isVelocityLimited())
		{
			double limitedVel = Geometry.getStopSpeed() - stopSpeedTolerance;
			
			switch (getMatchCtrl().getSkill().getType())
			{
				case GLOBAL_POSITION:
					BotSkillGlobalPosition botSkillGlobalPosition = (BotSkillGlobalPosition) getMatchCtrl().getSkill();
					botSkillGlobalPosition.setVelMax(Math.min(limitedVel, botSkillGlobalPosition.getVelMax()));
					break;
				case FAST_GLOBAL_POSITION:
					BotSkillFastGlobalPosition botSkillFastGlobalPosition = (BotSkillFastGlobalPosition) getMatchCtrl()
							.getSkill();
					botSkillFastGlobalPosition
							.setVelMax(Math.min(limitedVel, botSkillFastGlobalPosition.getVelMax()));
					break;
				default:
					break;
			}
		}
		
		if (getMoveCon().isEmergencyBreak())
		{
			DrawableCircle dc = new DrawableCircle(getPos(), 100, Color.pink);
			dc.setFill(true);
			getShapes().get(ESkillShapesLayer.PATH).add(dc);
			
			setMotorsOff();
		}
		
		String botSkillName = getMatchCtrl().getSkill().getType().name();
		String text = getType().name() + "\n" +
				getCurrentState().getIdentifier() + "\n" +
				botSkillName;
		DrawableAnnotation dAnno = new DrawableAnnotation(getPos(), text);
		dAnno.setColor(Color.red);
		dAnno.setFontHeight(50);
		dAnno.setCenterHorizontally(true);
		dAnno.setOffset(Vector2.fromY(150));
		
		getShapes().get(ESkillShapesLayer.SKILL_NAMES).add(dAnno);
	}
	
	
	protected void beforeStateUpdate()
	{
	}
	
	
	protected void afterStateUpdate()
	{
	}
	
	
	protected void updateKickerDribbler(final KickerDribblerCommands kickerDribblerOutput)
	{
	}
	
	
	@Override
	public final void update(final WorldFrameWrapper wfw, final ABot bot)
	{
		super.update(wfw, bot);
		if (wfw == null)
		{
			throw new IllegalArgumentException("WorldFrameWrapper must be non-null for move-skills!");
		}
		worldFrame = wfw.getWorldFrame(EAiTeam.primary(bot.getColor()));
		gameState = GameState.Builder.create().withGameState(wfw.getGameState()).withOurTeam(bot.getColor()).build();
		assert worldFrame != null;
		ITrackedBot newTbot = worldFrame.getBot(bot.getBotId());
		if (newTbot != null)
		{
			tBot = newTbot;
		} else if (tBot == null)
		{
			tBot = TrackedBot.stub(bot.getBotId(), worldFrame.getTimestamp());
		}
	}
	
	
	@Override
	public BotAiInformation getBotAiInfo()
	{
		BotAiInformation aiInfo = super.getBotAiInfo();
		
		double curVel = getVel().getLength2();
		aiInfo.setVelocityCurrent(curVel);
		
		return aiInfo;
	}
	
	
	/**
	 * @return the worldframe
	 */
	protected final WorldFrame getWorldFrame()
	{
		return worldFrame;
	}
	
	
	/**
	 * @return the gameState
	 */
	protected final GameState getGameState()
	{
		return gameState;
	}
	
	
	protected final IVector2 getPos()
	{
		return tBot.getPos();
	}
	
	
	protected final double getAngle()
	{
		return tBot.getOrientation();
	}
	
	
	protected final IVector2 getVel()
	{
		return tBot.getVel();
	}
	
	
	protected final double getaVel()
	{
		return tBot.getAngularVel();
	}
	
	
	public final ITrackedBot getTBot()
	{
		return tBot;
	}
	
	
	public final ITrackedBall getBall()
	{
		return getWorldFrame().getBall();
	}
}

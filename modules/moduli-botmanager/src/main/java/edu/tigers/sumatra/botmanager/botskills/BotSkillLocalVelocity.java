/*
 * Copyright (c) 2009 - 2022, DHBW Mannheim - TIGERs Mannheim
 */
package edu.tigers.sumatra.botmanager.botskills;

import edu.tigers.sumatra.bot.IMoveConstraints;
import edu.tigers.sumatra.bot.MoveConstraints;
import edu.tigers.sumatra.bot.params.BotMovementLimits;
import edu.tigers.sumatra.botmanager.botskills.data.DriveLimits;
import edu.tigers.sumatra.botmanager.botskills.data.EKickerDevice;
import edu.tigers.sumatra.botmanager.botskills.data.EKickerMode;
import edu.tigers.sumatra.botmanager.botskills.data.KickerDribblerCommands;
import edu.tigers.sumatra.botmanager.serial.SerialData;
import edu.tigers.sumatra.botmanager.serial.SerialData.ESerialDataType;
import edu.tigers.sumatra.math.vector.IVector2;
import edu.tigers.sumatra.math.vector.Vector2;


/**
 * @author AndreR
 */
public class BotSkillLocalVelocity extends AMoveBotSkill
{
	@SerialData(type = ESerialDataType.INT16)
	private final int[] vel = new int[3];

	@SerialData(type = ESerialDataType.UINT8)
	private int accMax = 0;
	@SerialData(type = ESerialDataType.UINT8)
	private int accMaxW = 0;
	@SerialData(type = ESerialDataType.UINT8)
	private int jerkMax = 0;
	@SerialData(type = ESerialDataType.UINT8)
	private int jerkMaxW = 0;

	@SerialData(type = ESerialDataType.EMBEDDED)
	private KickerDribblerCommands kickerDribbler = new KickerDribblerCommands();


	/**
	 *
	 */
	private BotSkillLocalVelocity()
	{
		super(EBotSkill.LOCAL_VELOCITY);
	}


	/**
	 * @param mc
	 */
	public BotSkillLocalVelocity(final IMoveConstraints mc)
	{
		this();
		setAccMax(mc.getAccMax());
		setAccMaxW(mc.getAccMaxW());
		setJerkMaxW(mc.getJerkMaxW());
		setJerkMax(mc.getJerkMax());
	}


	/**
	 * Set velocity in bot local frame.
	 *
	 * @param xy
	 * @param orientation
	 * @param mc
	 */
	public BotSkillLocalVelocity(final IVector2 xy, final double orientation, final IMoveConstraints mc)
	{
		this(mc);

		vel[0] = (int) (xy.x() * 1000.0);
		vel[1] = (int) (xy.y() * 1000.0);
		vel[2] = (int) (orientation * 1000.0);
	}


	/**
	 * Set velocity in bot local frame.
	 * Used by bot skills panel.
	 *
	 * @param xy
	 * @param orientation
	 * @param accMax
	 * @param accMaxW
	 * @param jerkMax
	 * @param jerkMaxW
	 * @param dribbleSpeed
	 * @param dribbleForce
	 * @param kickSpeed
	 * @param kickDevice
	 * @param kickMode
	 */
	@SuppressWarnings("squid:S00107")
	public BotSkillLocalVelocity(final IVector2 xy, final double orientation,
			final double accMax, final double accMaxW, final double jerkMax, final double jerkMaxW,
			final double dribbleSpeed, final double dribbleForce, final double kickSpeed, final EKickerDevice kickDevice,
			final EKickerMode kickMode)
	{
		this();

		setVelXy(xy);
		setVelW(orientation);

		setAccMax(accMax);
		setAccMaxW(accMaxW);
		setJerkMaxW(jerkMaxW);
		setJerkMax(jerkMax);

		kickerDribbler.setDribbler(dribbleSpeed, dribbleForce);
		kickerDribbler.setKick(kickSpeed, kickDevice, kickMode);
	}


	/**
	 * @return
	 */
	public double getX()
	{
		return vel[0] / 1000.0;
	}


	/**
	 * @return
	 */
	public double getY()
	{
		return vel[1] / 1000.0;
	}


	/**
	 * @return
	 */
	public double getW()
	{
		return vel[2] / 1000.0;
	}


	public void setVelXy(final IVector2 xy)
	{
		vel[0] = (int) (xy.x() * 1000.0);
		vel[1] = (int) (xy.y() * 1000.0);
	}


	public void setVelW(final double w)
	{
		vel[2] = (int) (w * 1000.0);
	}


	/**
	 * Max: 10m/s²
	 *
	 * @param val
	 */
	public final void setAccMax(final double val)
	{
		accMax = DriveLimits.toUInt8(val, DriveLimits.MAX_ACC);
	}


	/**
	 * Max: 100rad/s²
	 *
	 * @param val
	 */
	public final void setAccMaxW(final double val)
	{
		accMaxW = DriveLimits.toUInt8(val, DriveLimits.MAX_ACC_W);
	}


	public double getAccMax()
	{
		return DriveLimits.toDouble(accMax, DriveLimits.MAX_ACC);
	}


	public double getAccMaxW()
	{
		return DriveLimits.toDouble(accMaxW, DriveLimits.MAX_ACC_W);
	}


	/**
	 * Max: 10m/s²
	 *
	 * @param val
	 */
	public final void setJerkMax(final double val)
	{
		jerkMax = DriveLimits.toUInt8(val, DriveLimits.MAX_JERK);
	}


	/**
	 * Max: 100rad/s²
	 *
	 * @param val
	 */
	public final void setJerkMaxW(final double val)
	{
		jerkMaxW = DriveLimits.toUInt8(val, DriveLimits.MAX_JERK_W);
	}


	/**
	 * @return
	 */
	public double getJerkMax()
	{
		return DriveLimits.toDouble(jerkMax, DriveLimits.MAX_JERK);
	}


	/**
	 * @return
	 */
	public double getJerkMaxW()
	{
		return DriveLimits.toDouble(jerkMaxW, DriveLimits.MAX_JERK_W);
	}


	@Override
	public MoveConstraints getMoveConstraints()
	{
		MoveConstraints moveCon = new MoveConstraints(new BotMovementLimits());
		moveCon.setAccMax(getAccMax());
		moveCon.setAccMaxW(getAccMaxW());
		moveCon.setJerkMax(getJerkMax());
		moveCon.setJerkMaxW(getJerkMaxW());

		return moveCon;
	}


	/**
	 * @return the kickerDribbler
	 */
	@Override
	public KickerDribblerCommands getKickerDribbler()
	{
		return kickerDribbler;
	}


	/**
	 * @param kickerDribbler the kickerDribbler to set
	 */
	@Override
	public void setKickerDribbler(final KickerDribblerCommands kickerDribbler)
	{
		this.kickerDribbler = kickerDribbler;
	}


	public IVector2 getVelXy()
	{
		return Vector2.fromXY(getX(), getY());
	}
}

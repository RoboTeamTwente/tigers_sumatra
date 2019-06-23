/*
 * *********************************************************
 * Copyright (c) 2009 - 2010, DHBW Mannheim - Tigers Mannheim
 * Project: TIGERS - Sumatra
 * Date: 07.08.2010
 * Author(s): AndreR
 * *********************************************************
 */
package edu.dhbw.mannheim.tigers.sumatra.model.modules.impls.skillsystem.skills;

import java.util.LinkedList;
import java.util.List;

import edu.dhbw.mannheim.tigers.sumatra.model.data.shapes.vector.IVector2;
import edu.dhbw.mannheim.tigers.sumatra.model.data.shapes.vector.Vector2;
import edu.dhbw.mannheim.tigers.sumatra.model.data.trackedobjects.TrackedTigerBot;
import edu.dhbw.mannheim.tigers.sumatra.model.modules.impls.botmanager.commands.ACommand;
import edu.dhbw.mannheim.tigers.sumatra.model.modules.impls.skillsystem.ESkillName;


/**
 * Simple straight move that times out after a specified time.
 * Mainly for testing purposes.
 * 
 * @author AndreR
 * 
 */
public class StraightMoveSkill extends AMoveSkill
{
	/** mm */
	private final int		distance;
	/** rad */
	private final float	angle;
	
	
	/**
	 * 
	 * @param distance [mm]
	 * @param angle [rad]
	 */
	public StraightMoveSkill(int distance, float angle)
	{
		super(ESkillName.STRAIGHT_MOVE);
		
		this.distance = distance;
		this.angle = angle;
	}
	
	
	@Override
	public List<ACommand> doCalcEntryActions(TrackedTigerBot bot, List<ACommand> cmds)
	{
		List<IVector2> path = new LinkedList<IVector2>();
		path.add(bot.getPos().addNew(new Vector2(bot.getAngle() + angle).multiply(distance)));
		createSpline(bot, path, bot.getAngle());
		return cmds;
	}
	
	
	@Override
	protected void periodicProcess(TrackedTigerBot bot, List<ACommand> cmds)
	{
	}
	
	
	@Override
	protected boolean isComplete(TrackedTigerBot bot)
	{
		return super.isComplete(bot);
	}
}

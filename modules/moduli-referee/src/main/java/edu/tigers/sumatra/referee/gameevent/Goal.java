/*
 * Copyright (c) 2009 - 2018, DHBW Mannheim - TIGERs Mannheim
 */

package edu.tigers.sumatra.referee.gameevent;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.sleepycat.persist.model.Persistent;

import edu.tigers.sumatra.SslGameEvent;
import edu.tigers.sumatra.ids.BotID;
import edu.tigers.sumatra.ids.ETeamColor;
import edu.tigers.sumatra.math.vector.IVector2;


@Persistent
public class Goal extends AGameEvent
{
	private final ETeamColor team;
	private final ETeamColor kickingTeam;
	private final Integer kickingBot;
	private final IVector2 location;
	private final IVector2 kickLocation;
	
	
	@SuppressWarnings("unsued") // used by berkeley
	protected Goal()
	{
		team = null;
		kickingTeam = null;
		kickingBot = null;
		location = null;
		kickLocation = null;
	}
	
	
	/**
	 * Default conversion constructor. Note: Called by reflection!
	 *
	 * @param event a protobuf event
	 */
	public Goal(SslGameEvent.GameEvent event)
	{
		super(event);
		this.team = toTeamColor(event.getGoal().getByTeam());
		this.kickingTeam = toTeamColor(event.getGoal().getKickingTeam());
		this.kickingBot = event.getGoal().getKickingBot();
		this.location = toVector(event.getGoal().getLocation());
		this.kickLocation = toVector(event.getGoal().getKickLocation());
	}
	
	
	public Goal(ETeamColor forTeam, BotID bot, IVector2 location, IVector2 kickLocation)
	{
		super(EGameEvent.GOAL);
		this.team = forTeam;
		this.kickingTeam = bot == null ? null : bot.getTeamColor();
		this.kickingBot = bot == null ? null : bot.getNumber();
		this.location = location;
		this.kickLocation = kickLocation;
	}
	
	
	@Override
	public SslGameEvent.GameEvent toProtobuf()
	{
		SslGameEvent.GameEvent.Builder builder = SslGameEvent.GameEvent.newBuilder();
		builder.setType(SslGameEvent.GameEventType.GOAL);
		builder.getGoalBuilder()
				.setByTeam(getTeam(team));
		
		if (kickingTeam != null)
		{
			builder.getGoalBuilder().setKickingTeam(getTeam(kickingTeam));
		}
		
		if (kickingBot != null)
		{
			builder.getGoalBuilder().setKickingBot(kickingBot);
		}
		if (location != null)
		{
			builder.getGoalBuilder().setLocation(getLocationFromVector(location));
		}
		if (kickLocation != null)
		{
			builder.getGoalBuilder().setKickLocation(getLocationFromVector(kickLocation));
		}
		return builder.build();
	}
	
	
	@Override
	public String toString()
	{
		return String.format("Bot %d %s scored goal for %s (%s -> %s)", kickingBot, kickingTeam, team,
				formatVector(kickLocation), formatVector(location));
	}
	
	
	@Override
	public boolean equals(final Object o)
	{
		if (this == o)
			return true;
		
		if (o == null || getClass() != o.getClass())
			return false;
		
		final Goal goal = (Goal) o;
		
		return new EqualsBuilder()
				.appendSuper(super.equals(o))
				.append(team, goal.team)
				.append(kickingBot, goal.kickingBot)
				.append(kickingTeam, goal.kickingTeam)
				.append(location, goal.location)
				.append(kickLocation, goal.kickLocation)
				.isEquals();
	}
	
	
	@Override
	public int hashCode()
	{
		return new HashCodeBuilder(17, 37)
				.appendSuper(super.hashCode())
				.append(team)
				.append(kickingTeam)
				.append(kickingBot)
				.append(location)
				.append(kickLocation)
				.toHashCode();
	}
	
	
	public ETeamColor getTeam()
	{
		return team;
	}
}

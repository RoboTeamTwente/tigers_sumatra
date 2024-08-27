/*
 * Copyright (c) 2009 - 2020, DHBW Mannheim - TIGERs Mannheim
 */
package edu.tigers.sumatra.ai.pandora.plays.standard.cheerings;

import edu.tigers.sumatra.ai.pandora.plays.standard.CheeringPlay;
import edu.tigers.sumatra.ai.pandora.roles.move.MoveRole;
import edu.tigers.sumatra.botmanager.botskills.data.ESong;
import edu.tigers.sumatra.geometry.Geometry;
import edu.tigers.sumatra.math.AngleMath;
import edu.tigers.sumatra.math.circle.CircleMath;
import edu.tigers.sumatra.math.line.LineMath;
import edu.tigers.sumatra.math.vector.IVector2;
import edu.tigers.sumatra.math.vector.Vector2;
import edu.tigers.sumatra.math.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

import static edu.tigers.sumatra.math.AngleMath.PI_HALF;


public class CircleCheeringPlay implements ICheeringPlay
{
	private final IVector2 center = Geometry.getCenter();
	private final double radius = Geometry.getCenterCircle().radius();
	private CheeringPlay play = null;
	private CheeringPhase state = CheeringPhase.SETUP;
	private int passedPhases = 0;


	@Override
	public void initialize(final CheeringPlay play)
	{
		this.play = play;
	}


	@Override
	public boolean isDone()
	{
		return passedPhases >= 15;
	}


	@Override
	public List<IVector2> calcPositions()
	{
		return switch (state)
		{
			case SETUP -> setup();
			case START -> setRadius(1.5, 1.5f, 0);
			case ROTATE1, ROTATE2 -> rotate();
			case GROW -> setRadius(1.5, 1.4f, 0);
			case MOVEAGAIN -> move(500);
			case MOVEAGAIN1 -> move(-500);
			case SHRINK -> setRadius(1.5, 0.65f, 0);
			case SWITCH -> setRadius(1.2, 1.2f, 1);
		};
	}


	@Override
	public void doUpdate()
	{
		for (var role : play.getPermutedRoles())
		{
			play.setSong(role.getBotID(), ESong.CANTINA_BAND);
		}
		if (!onPosition())
		{
			return;
		}

		passedPhases++;
		switchState();
		updatePositions();
	}


	private void switchState()
	{
		switch (state)
		{
			case SWITCH, SETUP -> state = CheeringPhase.START;
			case START -> state = CheeringPhase.ROTATE1;
			case ROTATE1 -> state = CheeringPhase.ROTATE2;
			case ROTATE2 -> state = CheeringPhase.SHRINK;
			case SHRINK -> state = CheeringPhase.MOVEAGAIN;
			case MOVEAGAIN -> state = CheeringPhase.MOVEAGAIN1;
			case MOVEAGAIN1 -> state = CheeringPhase.GROW;
			case GROW -> state = CheeringPhase.SWITCH;
		}
	}


	private void updatePositions()
	{
		var roles = play.getPermutedRoles();
		var positions = calcPositions();

		for (int botId = 0; botId < roles.size(); botId++)
		{
			final MoveRole moveRole = roles.get(botId);
			moveRole.updateDestination(positions.get(botId));
		}
	}


	private boolean onPosition()
	{
		var roles = play.getPermutedRoles();
		for (var role : roles)
		{
			if (!role.isDestinationReached())
				return false;
		}
		return true;
	}


	private List<IVector2> setRadius(double param, final double factor, int n)
	{
		var roles = play.getPermutedRoles();
		var positions = new ArrayList<IVector2>();
		if (roles.size() % 2 == 1)
		{
			positions.add(Vector2.zero());
		}

		double factor1 = factor;
		double factor2 = factor * param;
		for (int i = roles.size() % 2; i < roles.size(); i++)
		{
			if (n % 2 == 0)
			{
				factor1 = factor2;
			}
			final MoveRole moveRole = roles.get(i);
			positions.add(moveRole.getDestination()
					.multiplyNew((factor1 * radius) / moveRole.getDestination().getLength()));
			n++;
			factor1 = factor;
		}
		return positions;
	}


	private List<IVector2> move(final double n)
	{
		var roles = play.getPermutedRoles();
		var positions = new ArrayList<IVector2>();

		for (MoveRole role : roles)
		{
			final MoveRole moveRole = role;
			positions.add(LineMath.stepAlongLine(moveRole.getDestination(),
					Vector2.fromXY(5000, moveRole.getPos().y()), n));
		}
		return positions;
	}


	private List<IVector2> rotate()
	{
		var roles = play.getPermutedRoles();
		var positions = new ArrayList<IVector2>();
		if (roles.size() % 2 == 1)
		{
			positions.add(Vector2.zero());
		}

		int n = 1;
		int c = play.getPermutedRoles().size();
		if (c % 2 != 0)
			c = play.getPermutedRoles().size() - 1;
		double angle1 = 1 * AngleMath.PI_TWO / c;
		double angle2 = -angle1;
		for (int i = roles.size() % 2; i < roles.size(); i++)
		{
			final MoveRole moveRole = roles.get(i);
			if (n % 2 == 0)
			{
				angle1 = angle2;
			}
			positions.add(CircleMath.stepAlongCircle(moveRole.getDestination(), center, angle1));
			angle1 = 1 * AngleMath.PI_TWO / c;
			n++;
		}
		return positions;
	}


	private List<IVector2> setup()
	{
		var roles = play.getPermutedRoles();
		var positions = new ArrayList<IVector2>();

		double angleStep = AngleMath.PI_TWO / (roles.size() - roles.size() % 2);
		if (roles.size() % 2 == 1)
		{
			positions.add(Vector2.zero());
		}

		IVector2 startOnCircle = Vector2f.fromXY(center.x(), center.y() + radius);
		double param = -PI_HALF;
		for (int i = roles.size() % 2; i < roles.size(); i++)
		{
			MoveRole moveRole = roles.get(i);
			positions.add(startOnCircle.turnNew(angleStep * i + param));
			moveRole.updateLookAtTarget(center);
		}

		return positions;
	}


	@Override
	public ECheeringPlays getType()
	{
		return ECheeringPlays.CIRCLE;
	}


	private enum CheeringPhase
	{
		SETUP,
		START,
		GROW,
		ROTATE1,
		ROTATE2,
		SHRINK,
		MOVEAGAIN,
		MOVEAGAIN1,
		SWITCH,
	}
}


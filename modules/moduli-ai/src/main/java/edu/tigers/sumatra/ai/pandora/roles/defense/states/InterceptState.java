/*
 * Copyright (c) 2009 - 2018, DHBW Mannheim - TIGERs Mannheim
 */

package edu.tigers.sumatra.ai.pandora.roles.defense.states;

import java.awt.Color;
import java.util.List;

import org.apache.commons.lang.Validate;

import edu.tigers.sumatra.ai.metis.EAiShapesLayer;
import edu.tigers.sumatra.ai.metis.defense.DefenseConstants;
import edu.tigers.sumatra.ai.metis.defense.DefenseMath;
import edu.tigers.sumatra.ai.pandora.roles.defense.ADefenseRole;
import edu.tigers.sumatra.drawable.DrawableCircle;
import edu.tigers.sumatra.drawable.DrawableLine;
import edu.tigers.sumatra.drawable.IDrawableShape;
import edu.tigers.sumatra.geometry.Geometry;
import edu.tigers.sumatra.math.circle.Circle;
import edu.tigers.sumatra.math.line.LineMath;
import edu.tigers.sumatra.math.line.v2.ILineSegment;
import edu.tigers.sumatra.math.line.v2.Lines;
import edu.tigers.sumatra.math.vector.IVector2;
import edu.tigers.sumatra.math.vector.Vector2;
import edu.tigers.sumatra.skillsystem.skills.AMoveToSkill;
import edu.tigers.sumatra.skillsystem.skills.util.InterceptorUtil;
import edu.tigers.sumatra.skillsystem.skills.util.PenAreaBoundary;
import edu.tigers.sumatra.statemachine.AState;
import edu.tigers.sumatra.wp.data.DynamicPosition;


/**
 * State used by Defender roles. Allows interception of given line defined by two dynamic positions.
 */
public class InterceptState extends AState
{
	private ADefenseRole parent;
	private AMoveToSkill skill;
	private DynamicPosition toIntercept;
	private DynamicPosition toProtect;
	private ILineSegment interceptLine;
	
	private double lookahead = 0.1;
	
	
	/**
	 * State used by defender Roles
	 * possible settings:
	 * 1. intercept line between ball and foeBot
	 * 2. intercept line between ball and goal center
	 * 3. intercept line between foeBot and goal center
	 * 4. intercept line between two foeBots
	 * 5. intercept line between two points
	 *
	 * @param parent role that executes this state
	 * @param toIntercept start point of interception line: ball or bot (or any dynamic position)
	 * @param toProtect end point of interception line: bot or goal (or any dynamic position)
	 */
	public InterceptState(ADefenseRole parent, DynamicPosition toIntercept, DynamicPosition toProtect)
	{
		this.parent = parent;
		Validate.notNull(toIntercept);
		Validate.notNull(toProtect);
		if (toIntercept.getTrackedId().equals(toProtect.getTrackedId())
				&& toIntercept.getPos().equals(toProtect.getPos()))
		{
			throw new IllegalArgumentException("toIntercept and toProtect should not be the same DynamicPosition");
		}
		this.toIntercept = toIntercept;
		this.toProtect = toProtect;
	}
	
	
	/**
	 * interceptLine needs to be set after creation!!!
	 * 
	 * @param parent role that executes this state
	 */
	public InterceptState(ADefenseRole parent)
	{
		IVector2 goalCenter = Geometry.getCenter();
		
		this.parent = parent;
		this.interceptLine = Lines.segmentFromPoints(goalCenter, DefenseMath.getBisectionGoal(goalCenter));
	}
	
	
	@Override
	public void doEntryActions()
	{
		skill = AMoveToSkill.createMoveToSkill();
		parent.setNewSkill(skill);
	}
	
	
	@Override
	public void doUpdate()
	{
		IVector2 threat;
		if (toProtect != null && toIntercept != null)
		{
			toIntercept.setLookahead(lookahead);
			toIntercept.update(parent.getWFrame());
			threat = toIntercept.getPos();
			toProtect.setLookahead(lookahead);
			toProtect.update(parent.getWFrame());
			if (toIntercept.getPos().equals(toProtect.getPos()))
			{
				toProtect.setPos(DefenseMath.getBisectionGoal(toIntercept.getPos()).addNew(Vector2.fromX(1)));
			}
			
			interceptLine = getLineToIntercept();
		} else
		{
			threat = interceptLine.getStart();
		}
		IVector2 interceptionPoint = InterceptorUtil.fastestPointOnLine(interceptLine, parent.getBot(),
				skill.getMoveCon().getMoveConstraints()).getTarget();
		
		interceptionPoint = getClosestPosInAllowedArea(interceptionPoint, threat);
		interceptionPoint = parent.getValidPositionByIcing(interceptionPoint);
		skill.getMoveCon().updateDestination(interceptionPoint);
		drawShapes(interceptLine, interceptionPoint);
	}
	
	
	private ILineSegment getLineToIntercept()
	{
		IVector2 end = isBotOrBall(toProtect)
				? LineMath.stepAlongLine(toProtect.getPos(), toIntercept.getPos(), Geometry.getBotRadius() * 3)
				: toProtect.getPos();
		IVector2 start = isBotOrBall(toIntercept) ? LineMath.stepAlongLine(toIntercept.getPos(), end,
				Geometry.getBotRadius() * 3) : toIntercept.getPos();
		return Lines.segmentFromPoints(start, end);
	}
	
	
	private boolean isBotOrBall(DynamicPosition pos)
	{
		return pos.getTrackedId().isBall() || pos.getTrackedId().isBot();
	}
	
	
	private IVector2 getClosestPosInAllowedArea(IVector2 pos, IVector2 toIntercept)
	{
		IVector2 newPos = pos;
		if (Geometry.getPenaltyAreaOur().withMargin(DefenseConstants.getMinGoOutDistance()).isPointInShapeOrBehind(pos))
		{
			newPos = PenAreaBoundary.ownWithMargin(DefenseConstants.getMinGoOutDistance()).projectPoint(pos, toIntercept);
		}
		return Geometry.getField().nearestPointInside(newPos, -2 * Geometry.getBotRadius());
	}
	
	
	private void drawShapes(ILineSegment interceptLine, IVector2 newPos)
	{
		List<IDrawableShape> shapes = parent.getAiFrame().getTacticalField().getDrawableShapes()
				.get(EAiShapesLayer.DEFENSE_INTERCEPT_STATE);
		shapes.add(new DrawableLine(interceptLine));
		shapes.add(new DrawableCircle(Circle.createCircle(newPos, Geometry.getBotRadius()), Color.GREEN));
	}
	

	protected void setLookahead(final double lookahead)
	{
		this.lookahead = lookahead;
	}
	

	protected void setInterceptLine(final ILineSegment interceptLine)
	{
		this.interceptLine = interceptLine;
	}
	
	
	public AMoveToSkill getSkill()
	{
		return skill;
	}
}

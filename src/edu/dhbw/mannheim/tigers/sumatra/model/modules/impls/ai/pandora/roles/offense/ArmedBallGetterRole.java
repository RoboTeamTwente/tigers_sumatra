/*
 * *********************************************************
 * Copyright (c) 2009 - 2010, DHBW Mannheim - Tigers Mannheim
 * Project: TIGERS - Sumatra
 * Date: 07.07.2010
 * Author(s):
 * FlorianS
 * *********************************************************
 */
package edu.dhbw.mannheim.tigers.sumatra.model.modules.impls.ai.pandora.roles.offense;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import edu.dhbw.mannheim.tigers.sumatra.model.data.IVector2;
import edu.dhbw.mannheim.tigers.sumatra.model.data.TrackedBot;
import edu.dhbw.mannheim.tigers.sumatra.model.data.Vector2;
import edu.dhbw.mannheim.tigers.sumatra.model.data.Vector2f;
import edu.dhbw.mannheim.tigers.sumatra.model.data.WorldFrame;
import edu.dhbw.mannheim.tigers.sumatra.model.modules.impls.ai.AIMath;
import edu.dhbw.mannheim.tigers.sumatra.model.modules.impls.ai.config.AIConfig;
import edu.dhbw.mannheim.tigers.sumatra.model.modules.impls.ai.data.AIInfoFrame;
import edu.dhbw.mannheim.tigers.sumatra.model.modules.impls.ai.data.types.APointMemory;
import edu.dhbw.mannheim.tigers.sumatra.model.modules.impls.ai.data.types.ValuePoint;
import edu.dhbw.mannheim.tigers.sumatra.model.modules.impls.ai.pandora.conditions.LookAtCon;
import edu.dhbw.mannheim.tigers.sumatra.model.modules.impls.ai.pandora.roles.ABaseRole;
import edu.dhbw.mannheim.tigers.sumatra.model.modules.impls.ai.pandora.roles.ERole;
import edu.dhbw.mannheim.tigers.sumatra.model.modules.impls.skillsystem.SkillFacade;


/**
 * The BallGetter goes behind the ball facing a certain point in our opponent's
 * goal and acquires ball possession. Assumes the ball just sits somewhere and
 * no-one possesses the ball. A viewPoint is the point the bot will look at when
 * it is behind the ball in order to shoot right after getting the ball. This
 * Role will be armed all the time!
 * 
 * @author FlorianS
 */
public class ArmedBallGetterRole extends ABaseRole
{
	/**  */
	private static final long	serialVersionUID		= -8374401797710010387L;
	
	private LookAtCon				lookAtCon;
	
	float								usedSpace;
	/** distance between the centers of bot and ball (when bot looks at the ball) */
	private final float			SPACE_MIN				= 0;
	/** distance between the centers of bot and ball (when bot doesn't look at the ball) */
	private final float			SPACE_MAX				= 1000;
	

	private ShooterMemory		myMemory;
	
	private final int				MEMORYSIZE				= AIConfig.getRoles().getIndirectShooter().getMemorysize();
	private final int				TRIES_PER_CYCLE		= AIConfig.getRoles().getIndirectShooter().getTriesPerCycle();
	
	private final float			GOALWIDTH				= AIConfig.getGeometry().getGoalTheir().getSize() * 0.90f;
	
	private final float			VELOCITY_TOLERANCE	= 0.2f;
	
	private boolean				stoppingBall			= false;
	
	
	// --------------------------------------------------------------------------
	// --- constructor(s) -------------------------------------------------------
	// --------------------------------------------------------------------------
	

	public ArmedBallGetterRole()
	{
		super(ERole.ARMED_BALL_GETTER);
		
		myMemory = new ShooterMemory(MEMORYSIZE, TRIES_PER_CYCLE);
		
		lookAtCon = new LookAtCon();
		addCondition(lookAtCon);
	}
	

	/**
	 * The idea of this BallGetter is the following:
	 * * We have a ball somewhere on the field
	 * * We want to "get" it, while looking at a given viewpoint (if the viewpoint is not defined, simply look at the
	 * ball)
	 */
	@Override
	public void update(AIInfoFrame currentFrame)
	{
		Vector2 botPos = new Vector2(currentFrame.worldFrame.tigerBots.get(getBotID()).pos);
		Vector2 ballPos = new Vector2(currentFrame.worldFrame.ball.pos);
		
		float currentAngleBetweenBallAndBot = AIMath.angleBetweenXAxisAndLine(botPos, ballPos);
		
		// angle between ball and bot (0-2PI);
		float angleBB = Math.abs(currentAngleBetweenBallAndBot - currentFrame.worldFrame.tigerBots.get(getBotID()).angle);
		
		// angle between ball and bot(0-PI)
		if (angleBB > Math.PI)
		{
			angleBB = (float) (Math.PI * 2 - angleBB);
		}
		
		// calculate desired distance between bot and ball. distance depends on the angle between bot and ball
		if (stoppingBall)
		{
			usedSpace = 0;
			
		} else
		{
			usedSpace = (float) (SPACE_MIN + (SPACE_MAX - SPACE_MIN) * (angleBB / Math.PI));
			
		}
		

		destCon.updateDestination(calculateDestination(currentFrame));
		lookAtCon.updateTarget(myMemory.generateBestPoint(currentFrame));
		

		currentFrame.addDebugPoint(lookAtCon.getLookAtTarget());
	}
	

	@Override
	public void calculateSkills(WorldFrame wFrame, SkillFacade skills)
	{
		boolean correctPosition = destCon.checkCondition(wFrame);
		boolean correctAngle = lookAtCon.checkCondition(wFrame);
		
		if (!correctPosition || !correctAngle)
		{
			//if (!stoppingBall)
			//{
				skills.moveTo(destCon.getDestination(), lookAtCon.getLookAtTarget());
			//} else
			//{
			//	skills.moveTo(destCon.getDestination());
			//}
			
		}
		
		float angle = wFrame.tigerBots.get(getBotID()).angle;
		if (angle < AIMath.PI_HALF && angle > -AIMath.PI_HALF )
		{
			skills.kickArm();
		} else
		{
			skills.disarm();
		}
	}
	

	/**
	 * calculates the position, where our bot shall be send to. In later versions, you will differ between a rolling ball
	 * and a sitting ball here
	 */
	private IVector2 calculateDestination(AIInfoFrame frame)
	{
		WorldFrame worldFrame = frame.worldFrame;
		IVector2 ballPos = worldFrame.ball.pos;
		Vector2f botPos = worldFrame.tigerBots.get(getBotID()).pos;
		Vector2f ballVel = frame.worldFrame.ball.vel;
//		Vector2f botVel = worldFrame.tigerBots.get(getBotID()).vel;
		float distance = AIMath.distancePP(botPos, ballPos);
		
		if (frame.worldFrame.ball.vel.getLength2() < VELOCITY_TOLERANCE
				|| frame.worldFrame.ball.vel.scaleToNew(1).x > 0.5 || frame.worldFrame.ball.vel.scaleToNew(1).x < -0.5)
		{
			stoppingBall = false;
			System.out.println("normal");
			return AIMath.stepAlongLine(ballPos, lookAtCon.getLookAtTarget(), -usedSpace);
			
		} else
		{
			/*
			 * calculate where the ball will be after a short period of time in order to
			 * find out whether it is coming closer or not
			 */
			Vector2 ballMovement;
			ballMovement = ballVel.scaleToNew(1.0f);
			Vector2 ballPosNearFuture = ballPos.addNew(ballMovement);
			
			Vector2 destination;
			

			/*
			 * knock-out criteria
			 * a moving ball cannot be reached when it is either
			 * - moving away from bot
			 * - moving too fast
			 * - an obstacle is between ball and destination point
			 */

			// distance between bot and ball
			float currentDistance = distance;
			float futureDistance = AIMath.distancePP(botPos, ballPosNearFuture);
			
			// estimated time of arrival at destination point
			// float etaBall = AIMath.distancePP(destination, ballPos) / ballVel.getLength2();
			// float etaBot = AIMath.distancePP(destination, botPos) / botVel.getLength2();
			
			// check knock out criteria
			if ((futureDistance > currentDistance))// || (etaBot > etaBall))
			{
				

				if (ballVel.getLength2() > 0.7f)
				{
					System.out.println("von uns weg, langsam");
					stoppingBall = true;
					
					destination = ballPos.addNew(ballMovement.scaleToNew(1.0f).multiply(1000 * ballVel.getLength2()));
					destination.x = destination.x - (400 - (400 * ballMovement.x));
					destination.y = destination.y - (400 - (400 * ballMovement.y));
					
					return destination;
				} else
				{
					System.out.println("von uns weg, langsam");
					stoppingBall = true;
					
					destination = ballPos.addNew(ballMovement.scaleToNew(1.0f).multiply(1000 * ballVel.getLength2()));
					
						destination.x = destination.x - (400 - (400 * ballMovement.x));
						destination.y = destination.y - (400 - (400 * ballMovement.y));
					
					return destination;
				}
				

			} else
			{
				//if (ballVel.getLength2() > 0.5f)
				//{
					System.out.println("auf uns zu, schnell");
					stoppingBall = true;
					destination = ballPos.addNew(ballMovement.scaleToNew(1.0f).multiply(1000 * ballVel.getLength2()));
					// destination = AIMath.leadPointOnLine(botPos, ballPos, ballPos.addNew(ballVel));
					return destination;
//				} else
//				{
//					System.out.println("auf uns zu, langsam");
//					stoppingBall = true;
//					// destination = AIMath.leadPointOnLine(botPos, ballPos, ballPos.addNew(ballVel));
//					destination = ballPos.addNew(ballMovement.scaleToNew(1.0f).multiply(1000 * ballVel.getLength2()));
//					
//					destination.x = destination.x - (400 - (400 * ballMovement.x));
//					destination.y = destination.y - (400 - (400 * ballMovement.y));
//					
//					return destination;
//				}
				
			}
		}
		
	}
	

	public void setDestTolerance(float newTolerance)
	{
		destCon.setTolerance(newTolerance);
	}
	
	// --- inner class Shooter-Memory extends AMemory---
	
	private class ShooterMemory extends APointMemory
	{
		
		public ShooterMemory(int memorysize, int triesPerCycle)
		{
			super(memorysize, triesPerCycle);
		}
		

		@Override
		public float evaluatePoint(ValuePoint valuePoint, AIInfoFrame currentFrame)
		{
			// speedhack :P
			
			// vs enemy team
			WorldFrame worldFrame = currentFrame.worldFrame;
			Vector2 end = valuePoint;
			Vector2 start = new Vector2(worldFrame.ball.pos);
			
			List<TrackedBot> botsToCheck = new ArrayList<TrackedBot>(5);
			
			for (Entry<Integer, TrackedBot> entry : worldFrame.foeBots.entrySet())
			{
				TrackedBot bot = entry.getValue();
				botsToCheck.add(bot);
				
			}
			
			float distanceStartEnd = AIMath.distancePP(start, end) + 150; // + 150 for safety, don't wanna forget one
			float minimumDistance = 1000000f;
			
			for (TrackedBot bot : botsToCheck)
			{
				float distanceBotStart = AIMath.distancePP(bot, start);
				float distanceBotEnd = AIMath.distancePP(bot, end);
				if (!(distanceStartEnd < distanceBotStart || distanceStartEnd < distanceBotEnd))
				{
					// only check those bots that possibly can be in between start and end
					float distanceBotLine = AIMath.distancePL(bot.pos, start, end);
					if (distanceBotLine < minimumDistance)
					{
						minimumDistance = distanceBotLine;
					}
				}
			}
			
			return minimumDistance;
			
		}
		

		@Override
		public ValuePoint generateNewPoint(AIInfoFrame currentFrame)
		{
			return new ValuePoint(AIConfig.getGeometry().getGoalTheir().getGoalCenter().x, (float) Math.random()
					* GOALWIDTH - GOALWIDTH / 2.0f);
		}
	}
}
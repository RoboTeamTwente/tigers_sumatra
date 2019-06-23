/*
 * Copyright (c) 2009 - 2017, DHBW Mannheim - TIGERs Mannheim
 */

package edu.tigers.sumatra.ai.pandora.plays.learning;


import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.github.g3force.configurable.Configurable;
import com.github.g3force.instanceables.InstanceableClass.NotCreateableException;

import edu.tigers.sumatra.ai.data.EAiShapesLayer;
import edu.tigers.sumatra.ai.data.frames.AthenaAiFrame;
import edu.tigers.sumatra.ai.data.frames.MetisAiFrame;
import edu.tigers.sumatra.ai.pandora.plays.APlay;
import edu.tigers.sumatra.ai.pandora.plays.EPlay;
import edu.tigers.sumatra.ai.pandora.plays.learning.lcase.ALearningCase;
import edu.tigers.sumatra.ai.pandora.plays.learning.lcase.ELearningCase;
import edu.tigers.sumatra.ai.pandora.roles.ARole;
import edu.tigers.sumatra.ai.pandora.roles.ERole;
import edu.tigers.sumatra.ai.pandora.roles.move.MoveRole;
import edu.tigers.sumatra.drawable.DrawableAnnotation;
import edu.tigers.sumatra.math.vector.AVector2;
import edu.tigers.sumatra.math.vector.IVector2;
import edu.tigers.sumatra.referee.data.GameState;


/**
 * The LearningPlay is there to learn things that have to be learned only
 * one time for each environment.
 * 
 * @author Mark Geiger <Mark.Geiger@dlr.de>
 */
public class LearningPlay extends APlay
{
	private static final Logger log = Logger.getLogger(LearningPlay.class
			.getName());
	
	@Configurable(defValue = "ROBOT_MOVEMENT")
	private static ELearningCase learningType = ELearningCase.ROBOT_MOVEMENT;
	
	@Configurable(defValue = "true")
	private static boolean runAll = true;
	
	private final List<ELearningCase> learningCase = new ArrayList<>();
	private int currentLearningCase = 0;
	private ALearningCase activeCase = null;
	
	
	// --------------------------------------------------------------------------
	// --- variables and constants ----------------------------------------------
	// --------------------------------------------------------------------------
	
	/**
	 * Creates a new LearningPlay
	 */
	public LearningPlay()
	{
		super(EPlay.LEARNING_PLAY);
		learningCase.addAll(Arrays.asList(ELearningCase.values()));
	}
	
	
	// --------------------------------------------------------------------------
	// --- constructors ---------------------------------------------------------
	// --------------------------------------------------------------------------
	
	
	// --------------------------------------------------------------------------
	// --- methods --------------------------------------------------------------
	// --------------------------------------------------------------------------
	
	
	@Override
	protected ARole onRemoveRole(final MetisAiFrame frame)
	{
		return getLastRole();
	}
	
	
	@Override
	protected ARole onAddRole(final MetisAiFrame frame)
	{
		return new MoveRole();
	}
	
	
	@Override
	public void updateBeforeRoles(final AthenaAiFrame frame)
	{
		// Not implemented
	}
	
	
	@Override
	protected void onGameStateChanged(final GameState gameState)
	{
		// Not implemented
	}
	
	
	@Override
	protected void doUpdate(final AthenaAiFrame frame)
	{
		if (currentLearningCase < learningCase.size())
		{
			ELearningCase ecase = learningCase.get(currentLearningCase);
			if (!runAll)
			{
				ecase = learningType;
			}
			try
			{
				if (activeCase == null)
				{
					activeCase = (ALearningCase) ecase.getInstanceableClass().newInstance();
					log.info("starting new LearningCase: " + ecase);
					log.info("waiting to get ready:\n" + activeCase.getReadyCriteria());
				}
				
				if (activeCase.isActive(frame))
				{
					activeCase.update(getRoles(), frame);
					updateRoleAssignment(activeCase);
					
					if (activeCase.isFinished(frame))
					{
						activeCase.setActive(false);
						activeCase = null;
						currentLearningCase++;
						if (!runAll)
						{
							currentLearningCase = Integer.MAX_VALUE;
						}
						List<ARole> roles = new ArrayList<>(getRoles());
						for (ARole role : roles)
						{
							switchRoles(role, new MoveRole());
						}
					}
				} else if (activeCase.isReady(frame, getRoles()))
				{
					activeCase.setActive(true);
				}
				
				IVector2 pos = AVector2.ZERO_VECTOR;
				DrawableAnnotation text = new DrawableAnnotation(pos, "still learning, active case: " + ecase, Color.black);
				frame.getTacticalField().getDrawableShapes().get(EAiShapesLayer.LEARNING).add(text);
				
			} catch (NotCreateableException err)
			{
				log.error("Critical error in LearningPlay, could not instanciate LearningCase", err);
			}
		} else
		{
			// lerning finished
			IVector2 pos = AVector2.ZERO_VECTOR;
			DrawableAnnotation text = new DrawableAnnotation(pos, "Finished learning!", Color.black);
			frame.getTacticalField().getDrawableShapes().get(EAiShapesLayer.LEARNING).add(text);
		}
		
	}
	
	
	@SuppressWarnings("squid:MethodCyclomaticComplexity")
	private void updateRoleAssignment(final ALearningCase acase)
	{
		List<ERole> wantedRolesSorted = activeCase.getActiveRoleTypes().stream().sorted()
				.collect(Collectors.toList());
		List<ERole> activeRolesSorted = new ArrayList<>();
		for (ARole role : getRoles())
		{
			activeRolesSorted.add(role.getType());
		}
		activeRolesSorted = activeRolesSorted.stream().sorted().collect(Collectors.toList());
		Map<ERole, Integer> activeStatus = new EnumMap<>(ERole.class);
		for (ERole role : activeRolesSorted)
		{
			if (!activeStatus.containsKey(role))
			{
				activeStatus.put(role, 1);
			} else
			{
				activeStatus.put(role, activeStatus.get(role) + 1);
			}
		}
		Map<ERole, Integer> wantedStatus = new EnumMap<>(ERole.class);
		for (ERole role : wantedRolesSorted)
		{
			if (!wantedStatus.containsKey(role))
			{
				wantedStatus.put(role, 1);
			} else
			{
				wantedStatus.put(role, wantedStatus.get(role) + 1);
			}
		}
		
		List<ERole> takeAway = new ArrayList<>();
		List<ERole> add = new ArrayList<>();
		
		Set<ERole> mergedERoles = new HashSet<>(activeStatus.keySet());
		mergedERoles.addAll(new HashSet<>(wantedStatus.keySet()));
		
		for (ERole key : mergedERoles)
		{
			int wantedNumberOfRoles = 0;
			if (wantedStatus.containsKey(key))
			{
				wantedNumberOfRoles = wantedStatus.get(key);
			}
			int activeNumberOfRoles = 0;
			if (activeStatus.containsKey(key))
			{
				activeNumberOfRoles = activeStatus.get(key);
			}
			
			int numberOfRoles = activeNumberOfRoles - wantedNumberOfRoles;
			while (numberOfRoles > 0)
			{
				takeAway.add(key);
				numberOfRoles--;
			}
			while (numberOfRoles < 0)
			{
				add.add(key);
				numberOfRoles++;
			}
		}
		
		List<ARole> roles = new ArrayList<>(getRoles());
		for (ARole role : roles)
		{
			if (takeAway.contains(role.getType()))
			{
				takeAway.remove(role.getType());
				if (!add.isEmpty())
				{
					ARole newRole;
					try
					{
						newRole = (ARole) add.get(0).getInstanceableClass().newDefaultInstance();
						switchRoles(role, newRole);
					} catch (NotCreateableException err)
					{
						log.error("Critical error in LearningPlay, could not instanciate Role", err);
					}
				} else
				{
					switchRoles(role, new MoveRole());
				}
			}
		}
	}
	
	// --------------------------------------------------------------------------
	// --- getter/setter --------------------------------------------------------
	// --------------------------------------------------------------------------
	
}

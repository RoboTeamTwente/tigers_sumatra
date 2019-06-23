/*
 * *********************************************************
 * Copyright (c) 2009 - 2011, DHBW Mannheim - Tigers Mannheim
 * Project: TIGERS - Sumatra
 * Date: 10.11.2011
 * Author(s): osteinbrecher
 * 
 * *********************************************************
 */
package edu.dhbw.mannheim.tigers.sumatra.test.junit.model.data.trackedobjects;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import edu.dhbw.mannheim.tigers.sumatra.model.data.trackedobjects.ids.AObjectID;
import edu.dhbw.mannheim.tigers.sumatra.model.data.trackedobjects.ids.BallID;
import edu.dhbw.mannheim.tigers.sumatra.model.data.trackedobjects.ids.BotID;


/**
 * This is a unit test for {@link AObjectID} and sub classes.
 * 
 * @author Oliver Steinbrecher
 * 
 */
public class ObjectIDTester
{
	// --------------------------------------------------------------------------
	// --- variables and constants ----------------------------------------------
	// --------------------------------------------------------------------------
	
	
	// --------------------------------------------------------------------------
	// --- constructors ---------------------------------------------------------
	// --------------------------------------------------------------------------
	
	
	// --------------------------------------------------------------------------
	// --- methods --------------------------------------------------------------
	// --------------------------------------------------------------------------
	/**
	 */
	@Test
	public void testID()
	{
		final BotID botId = new BotID(1);
		
		BotID botId2 = botId;
		assertTrue(botId.equals(botId2));
		
		botId2 = new BotID(1);
		assertTrue(botId.equals(botId2));
		
		botId2 = new BotID(2);
		assertFalse(botId.equals(botId2));
		
		assertFalse(botId.equals(null));
		assertFalse(botId.equals(new BallID()));
		assertFalse(botId.equals(this));
		
		// System.out.println(botId);
		// System.out.println(new BallID());
	}
	
	// --------------------------------------------------------------------------
	// --- getter/setter --------------------------------------------------------
	// --------------------------------------------------------------------------
}

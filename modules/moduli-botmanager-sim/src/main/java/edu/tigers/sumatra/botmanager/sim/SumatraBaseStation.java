/*
 * Copyright (c) 2009 - 2018, DHBW Mannheim - TIGERs Mannheim
 */
package edu.tigers.sumatra.botmanager.sim;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.tigers.sumatra.botmanager.basestation.ABaseStation;
import edu.tigers.sumatra.botmanager.botskills.data.MatchCommand;
import edu.tigers.sumatra.ids.BotID;
import edu.tigers.sumatra.model.SumatraModel;
import edu.tigers.sumatra.sim.ASumatraSimulator;
import edu.tigers.sumatra.sim.ISimulatorActionCallback;
import edu.tigers.sumatra.sim.dynamics.bot.SimBotAction;
import edu.tigers.sumatra.sim.dynamics.bot.SimBotState;
import edu.tigers.sumatra.vision.AVisionFilter;


/**
 * Base station implementation for local sumatra simulation
 */
public class SumatraBaseStation extends ABaseStation implements ISimulatorActionCallback
{
	private final Map<BotID, SumatraBot> bots = new HashMap<>();
	private final Map<BotID, MatchCommand> currentMatchCommands = new HashMap<>();
	
	
	@Override
	public void updateConnectedBotList(Map<BotID, SimBotState> botStates)
	{
		Set<BotID> newBots = new HashSet<>(botStates.keySet());
		newBots.removeAll(bots.keySet());
		newBots.forEach(this::addBot);
		
		Set<BotID> removedBots = new HashSet<>(bots.keySet());
		removedBots.removeAll(botStates.keySet());
		removedBots.forEach(this::removeBot);
	}
	
	
	@Override
	public Map<BotID, SimBotAction> nextSimBotActions(Map<BotID, SimBotState> botStates, final long timestamp)
	{
		Map<BotID, SimBotAction> map = new HashMap<>();
		for (Map.Entry<BotID, MatchCommand> entry : currentMatchCommands.entrySet())
		{
			BotID botID = entry.getKey();
			MatchCommand matchCommand = entry.getValue();
			SumatraBot bot = bots.get(botID);
			SimBotState botState = botStates.get(botID);
			if (matchCommand != null && bot != null && botState != null)
			{
				SimBotAction simBotAction = bot.simulate(botState, matchCommand, timestamp);
				map.put(bot.getBotId(), simBotAction);
			}
		}
		return map;
	}
	
	
	@Override
	public void acceptMatchCommand(final BotID botId, final MatchCommand matchCommand)
	{
		currentMatchCommands.put(botId, matchCommand);
	}
	
	
	@Override
	public void connect()
	{
		ASumatraSimulator sim = (ASumatraSimulator) SumatraModel.getInstance().getModule(AVisionFilter.class);
		sim.addSimulatorActionCallback(this);
	}
	
	
	@Override
	public void disconnect()
	{
		ASumatraSimulator sim = (ASumatraSimulator) SumatraModel.getInstance().getModule(AVisionFilter.class);
		sim.removeSimulatorActionCallback(this);
	}
	
	
	@Override
	public void addBot(final BotID botID)
	{
		final SumatraBot sumatraBot = new SumatraBot(botID, this);
		bots.put(botID, sumatraBot);
		botOnline(sumatraBot);
	}
	
	
	@Override
	public void removeBot(final BotID botID)
	{
		botOffline(botID);
		bots.remove(botID);
	}
}

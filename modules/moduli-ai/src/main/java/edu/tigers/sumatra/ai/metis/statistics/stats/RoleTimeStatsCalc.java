/*
 * Copyright (c) 2009 - 2020, DHBW Mannheim - TIGERs Mannheim
 */

package edu.tigers.sumatra.ai.metis.statistics.stats;

import edu.tigers.sumatra.ai.BaseAiFrame;
import edu.tigers.sumatra.ai.athena.IPlayStrategy;
import edu.tigers.sumatra.ai.pandora.plays.EPlay;
import edu.tigers.sumatra.ai.pandora.roles.ARole;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Relative time a robot is assigned to offense/support/defense, and relative number of bots per play
 */
public class RoleTimeStatsCalc extends AStatsCalc
{
	private final Map<EPlay, Map<Integer, Percentage>> playMapBots = new EnumMap<>(EPlay.class);
	private final Map<EPlay, Percentage> playMapGeneral = new EnumMap<>(EPlay.class);
	private final Map<EPlay, Integer> playCounts = new EnumMap<>(EPlay.class);
	private int totalRoleCount = 0;

	private final Map<EPlay, EMatchStatistics> playToStatType = new EnumMap<>(EPlay.class);


	public RoleTimeStatsCalc()
	{
		playToStatType.put(EPlay.OFFENSIVE, EMatchStatistics.FRAMES_AS_OFFENSIVE);
		playToStatType.put(EPlay.SUPPORT, EMatchStatistics.FRAMES_AS_SUPPORT);
		playToStatType.put(EPlay.DEFENSIVE, EMatchStatistics.FRAMES_AS_DEFENDER);

		playToStatType.forEach((play, stats) -> playMapBots.put(play, new HashMap<>()));
		playToStatType.forEach((play, stats) -> playMapGeneral.put(play, new Percentage()));
	}


	@Override
	public void saveStatsToMatchStatistics(final MatchStats matchStatistics)
	{
		playToStatType.forEach((play, stat) -> matchStatistics
				.putStatisticData(stat, new StatisticData(
						playMapBots.get(play),
						playMapGeneral.get(play))));
	}


	@Override
	public void onStatisticUpdate(final BaseAiFrame baseAiFrame)
	{
		IPlayStrategy previousPlayStrategy = baseAiFrame.getPrevFrame().getPlayStrategy();

		totalRoleCount += playMapGeneral.keySet().stream()
				.map(previousPlayStrategy::getActiveRoles)
				.mapToInt(Collection::size)
				.sum();

		for (Map.Entry<EPlay, Percentage> entry : playMapGeneral.entrySet())
		{
			EPlay playType = entry.getKey();
			Percentage playPercentage = entry.getValue();

			Map<Integer, Percentage> perBotPercentage = playMapBots.get(playType);
			List<ARole> roles = previousPlayStrategy.getActiveRoles(playType);

			// increase role count of play
			int playRoleCount = playCounts.getOrDefault(playType, 0) + roles.size();
			playCounts.put(playType, playRoleCount);

			for (ARole role : roles)
			{
				// increase number of roles of play
				playPercentage.inc();

				// increase number of assignments for each bot per number of roles of play
				perBotPercentage.computeIfAbsent(role.getBotID().getNumber(), a -> new Percentage()).inc();
				perBotPercentage.values().forEach(p -> p.setAll(playRoleCount));
			}

			playPercentage.setAll(totalRoleCount);
		}
	}
}

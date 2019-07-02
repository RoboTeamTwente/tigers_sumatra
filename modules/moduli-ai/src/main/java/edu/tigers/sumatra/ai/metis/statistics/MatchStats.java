/*
 * Copyright (c) 2009 - 2017, DHBW Mannheim - TIGERs Mannheim
 */

package edu.tigers.sumatra.ai.metis.statistics;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.simple.JSONObject;

import com.sleepycat.persist.model.Persistent;

import edu.tigers.sumatra.ai.metis.ballpossession.EBallPossession;


/**
 * Data holder for MatchStatisticsCalc elements.
 */
@Persistent
public class MatchStats
{
	private final Map<EBallPossession, Percentage> ballPossessionGeneral = new EnumMap<>(EBallPossession.class);
	private final Map<EMatchStatistics, StatisticData> statistics = new EnumMap<>(EMatchStatistics.class);
	
	
	/**
	 * Default
	 */
	public MatchStats()
	{
		for (EBallPossession bp : EBallPossession.values())
		{
			ballPossessionGeneral.put(bp, new Percentage());
		}
	}
	
	
	/**
	 * @return the statistics
	 */
	public Map<EMatchStatistics, StatisticData> getStatistics()
	{
		return statistics;
	}
	
	
	/**
	 * This adds a specific type of Statistic data to be displayed in the statisticsPanel
	 *
	 * @param key The Statistic Type to be put
	 * @param value The Statistic to be put
	 */
	public void putStatisticData(final EMatchStatistics key, final StatisticData value)
	{
		statistics.put(key, value);
	}
	
	
	/**
	 * @return the ballPossessionGeneral
	 */
	public Map<EBallPossession, Percentage> getBallPossessionGeneral()
	{
		return ballPossessionGeneral;
	}
	
	
	/**
	 * @return all contained bots
	 */
	public Set<Integer> getAllBots()
	{
		return statistics.values().stream()
				.map(StatisticData::getContainedBotIds)
				.flatMap(Collection::stream)
				.collect(Collectors.toSet());
	}
	
	
	@SuppressWarnings("unchecked")
	public JSONObject toJSON()
	{
		JSONObject jsonObject = new JSONObject();
		for (Map.Entry<EBallPossession, Percentage> entry : ballPossessionGeneral.entrySet())
		{
			jsonObject.put(entry.getKey().name(), Double.toString(entry.getValue().getPercent()));
		}
		for (Map.Entry<EMatchStatistics, StatisticData> entry : statistics.entrySet())
		{
			jsonObject.put(entry.getKey(), entry.getValue().formattedGeneralStatistic());
			for (Map.Entry<Integer, String> e : entry.getValue().formattedBotStatistics().entrySet())
			{
				jsonObject.put(entry.getKey().toString() + e.getKey().toString(), e.getValue());
			}
		}
		return jsonObject;
	}
}

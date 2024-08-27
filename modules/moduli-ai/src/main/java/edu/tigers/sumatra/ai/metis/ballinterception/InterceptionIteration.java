/*
 * Copyright (c) 2009 - 2024, DHBW Mannheim - TIGERs Mannheim
 */

package edu.tigers.sumatra.ai.metis.ballinterception;

import com.sleepycat.persist.model.Persistent;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;


@Value
@Persistent
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class InterceptionIteration
{
	double ballTravelTime;
	double slackTime;
	double includedSlackTimeBonus;


	@SuppressWarnings("unused") // berkeley
	protected InterceptionIteration()
	{
		ballTravelTime = 0;
		slackTime = 0;
		includedSlackTimeBonus = 0;
	}
}

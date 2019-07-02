/*
 * *********************************************************
 * Copyright (c) 2009 - 2014, DHBW Mannheim - Tigers Mannheim
 * Project: TIGERS - Sumatra
 * Date: Jan 12, 2014
 * Author(s): Nicolai Ommer <nicolai.ommer@gmail.com>
 * *********************************************************
 */
package edu.tigers.sumatra.botoverview;

import edu.tigers.sumatra.views.ASumatraView;
import edu.tigers.sumatra.views.ESumatraViewType;
import edu.tigers.sumatra.views.ISumatraViewPresenter;


/**
 * This view shows information about each bot
 */
public class BotOverviewView extends ASumatraView
{
	public BotOverviewView()
	{
		super(ESumatraViewType.BOT_OVERVIEW);
	}


	@Override
	protected ISumatraViewPresenter createPresenter()
	{
		return new BotOverviewPresenter();
	}
}

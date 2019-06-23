/*
 * *********************************************************
 * Copyright (c) 2009 - 2010, DHBW Mannheim - Tigers Mannheim
 * Project: TIGERS - Sumatra
 * Date: 12.10.2010
 * Author(s): Yakisoba
 * 
 * *********************************************************
 */
package edu.dhbw.mannheim.tigers.sumatra.model.modules.impls.worldpredictor.oextkal.benchmarking;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import edu.dhbw.mannheim.tigers.sumatra.model.modules.impls.worldpredictor.WPConfig;
import edu.dhbw.mannheim.tigers.sumatra.model.modules.impls.worldpredictor.oextkal.data.BallMotionResult;
import edu.dhbw.mannheim.tigers.sumatra.model.modules.impls.worldpredictor.oextkal.data.RobotMotionResult;
import edu.dhbw.mannheim.tigers.sumatra.model.modules.impls.worldpredictor.oextkal.data.RobotMotionResult_V2;
import edu.dhbw.mannheim.tigers.sumatra.model.modules.impls.worldpredictor.oextkal.data.WPCamBall;
import edu.dhbw.mannheim.tigers.sumatra.model.modules.impls.worldpredictor.oextkal.data.WPCamBot;


/**
 * - writes current and predicted states of one tracked object
 * 
 * @author MarenK
 * 
 */
public class Precision
{
	// --------------------------------------------------------------------------
	// --- variables and constants ----------------------------------------------
	// --------------------------------------------------------------------------
	private static Precision	instance;
	
	private int						trackId;
	private int[]					id;
	private double[]				timestamp;
	private double[][]			pos;
	private double[]				rot;
	
	private final int				SAVED_ITEMS	= 10000;
	private int						count;
	private double					normTime;
	private int						stepCount;
	
	private boolean				doubledTimestamp;
	
	private FileOutputStream	fop;
	private File					f;
	
	
	// --------------------------------------------------------------------------
	// --- constructors ---------------------------------------------------------
	// --------------------------------------------------------------------------
	private Precision()
	{
		f = new File(WPConfig.DEBUGFILE);
		
		id = new int[SAVED_ITEMS];
		timestamp = new double[SAVED_ITEMS];
		pos = new double[SAVED_ITEMS][2];
		rot = new double[SAVED_ITEMS];
		
		count = 0;
		trackId = 0;
		normTime = -1;
		
		doubledTimestamp = false;
	}
	

	public static Precision getInstance()
	{
		if (instance == null)
		{
			instance = new Precision();
		}
		return instance;
	}
	

	// --------------------------------------------------------------------------
	// --- methods --------------------------------------------------------------
	// --------------------------------------------------------------------------
	public void clear()
	{
		
		for (int i = 0; i < SAVED_ITEMS; i++)
		{
			id[i] = 0;
			timestamp[i] = 0;
			pos[i][0] = 0;
			pos[i][1] = 0;
			rot[i] = 0;
		}
		count = 0;
	}
	
	@SuppressWarnings(value = { "unused" })
	public void addBall(double timestamp, BallMotionResult ball)
	{
		if (!WPConfig.DEBUG || trackId != 0 || doubledTimestamp)
			return;
		if (isFull())
		{
			write();
			clear();
		}
		
		if (count == 0 && normTime == -1)
			normTime = timestamp;
		
		id[count] = 0;
		this.timestamp[count] = timestamp;
		pos[count][0] = ball.x;
		pos[count][1] = ball.y;
		rot[count] = 0;
		
		count++;
	}
	
	@SuppressWarnings(value = { "unused" })
	public void addCamBall(double timestamp, WPCamBall ball)
	{
		if (!WPConfig.DEBUG || trackId != 0)
			return;

		// doppelte camframes
		if (count > (stepCount+1) && timestamp == this.timestamp[count - (stepCount +2)])
		{
			doubledTimestamp = true;
			return;
		} else
			doubledTimestamp = false;
		
		if (isFull())
		{
			write();
			clear();
		}
		
		if (count == 0 && normTime == -1)
			normTime = timestamp;
		
		id[count] = 0;
		this.timestamp[count] = timestamp;
		pos[count][0] = ball.x;
		pos[count][1] = ball.y;
		rot[count] = 0;
		
		count++;
	}
	
	@SuppressWarnings(value = { "unused" })
	public void addBot(double timestamp, RobotMotionResult bot, int botID)
	{
		if (!WPConfig.DEBUG || trackId != botID || doubledTimestamp)
			return;
		if (isFull())
		{
			write();
			clear();
		}
		if (count == 0 && normTime == -1)
			normTime = timestamp;
		
		id[count] = botID;
		this.timestamp[count] = timestamp;
		pos[count][0] = bot.x;
		pos[count][1] = bot.y;
		rot[count] = bot.orientation;
		
		count++;
	}
	
	@SuppressWarnings(value = { "unused" })
	public void addBot(double timestamp, RobotMotionResult_V2 bot, int botID)
	{
		if (!WPConfig.DEBUG || trackId != botID || doubledTimestamp)
			return;
		if (isFull())
		{
			write();
			clear();
		}
		if (count == 0 && normTime == -1)
			normTime = timestamp;
		
		id[count] = botID;
		this.timestamp[count] = timestamp;
		pos[count][0] = bot.x;
		pos[count][1] = bot.y;
		rot[count] = bot.orientation;
		
		count++;
	}

	@SuppressWarnings(value = { "unused" })
	public void addCamBot(double timestamp, WPCamBot bot, int botID)
	{
		if (!WPConfig.DEBUG || trackId != botID)
			return;
		
	// doppelte und zu alte camframes
		if (count > (stepCount+1) && timestamp == this.timestamp[count - (stepCount +2)])
		{
			doubledTimestamp = true;
			return;
		} else
		{
			doubledTimestamp = false;
		}
		
		if (isFull())
		{
			write();
			clear();
		}
		
		if (count == 0 && normTime == -1)
			normTime = timestamp;
		
		id[count] = bot.id;
		this.timestamp[count] = timestamp;
		pos[count][0] = bot.x;
		pos[count][1] = bot.y;
		rot[count] = bot.orientation;
		
		count++;
	}
	

	public void write()
	{
		String str;
		doubledTimestamp = false;
		for (int i = stepCount + 1; i < count - 2; i++)
		{
			str = "" + id[i];
			for (int j = 0; j <= stepCount + 1; j++, i++)
			{
				str += " " + (timestamp[i] - normTime) + " " + pos[i][0] + " " + pos[i][1] + " " + rot[i];
			}
			
			// decrement i because we increment i in above for() one times too often
			i--;
			
			str += " \n";
			try
			{
				fop.write(str.getBytes());
			} catch (IOException err)
			{
				err.printStackTrace();
			}
		}
		
	}
	

	public void close()
	{
		try
		{
			fop.close();
			normTime = -1;
		} catch (IOException err)
		{
			err.printStackTrace();
		}
		
	}
	

	public void open(int stepcount)
	{
		try
		{
			fop = new FileOutputStream(f);
			trackId = WPConfig.DEBUGID;
			this.stepCount = stepcount;
		} catch (FileNotFoundException err)
		{
			err.printStackTrace();
		}
	}
	

	// --------------------------------------------------------------------------
	// --- getter/setter --------------------------------------------------------
	// --------------------------------------------------------------------------
	
	private boolean isFull()
	{
		return ((count + stepCount) > SAVED_ITEMS);
	}
}

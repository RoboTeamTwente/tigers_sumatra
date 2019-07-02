/*
 * Copyright (c) 2009 - 2018, DHBW Mannheim - TIGERs Mannheim
 */
package edu.tigers.sumatra.botparams;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import edu.tigers.moduli.AModule;
import edu.tigers.sumatra.bot.params.BotParams;
import edu.tigers.sumatra.bot.params.IBotParams;
import edu.tigers.sumatra.botparams.BotParamsDatabase.IBotParamsDatabaseObserver;


/**
 * BotParams module.
 * 
 * @author AndreR <andre@ryll.cc>
 */
public class BotParamsManager extends AModule implements IBotParamsDatabaseObserver, BotParamsProvider
{
	private static final Logger log = Logger.getLogger(BotParamsManager.class.getName());
	private static final String DATABASE_FILE = "config/botParamsDatabase.json";
	
	private BotParamsDatabase database;
	private final List<IBotParamsManagerObserver> observers = new CopyOnWriteArrayList<>();
	
	
	@Override
	public void initModule()
	{
		database = loadDatabase();
		database.addObserver(this);
	}
	
	
	@Override
	public void stopModule()
	{
		saveDatabase();
	}
	
	
	/**
	 * Only used internally for presenter.
	 * 
	 * @return
	 */
	public BotParamsDatabase getDatabase()
	{
		return database;
	}
	
	
	@Override
	public IBotParams get(final EBotParamLabel label)
	{
		return database.getSelectedParams(label);
	}
	
	
	private void saveDatabase()
	{
		File file = Paths.get(DATABASE_FILE).toFile();
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		mapper.setDefaultPrettyPrinter(new DefaultPrettyPrinter()
				.withObjectIndenter(new DefaultIndenter().withLinefeed("\n"))
				.withoutSpacesInObjectEntries());
		
		try
		{
			mapper.writeValue(file, database);
		} catch (IOException e)
		{
			log.error("", e);
		}
	}
	
	
	private BotParamsDatabase loadDatabase()
	{
		File file = Paths.get(DATABASE_FILE).toFile();
		if (file.exists())
		{
			log.debug("Open existing bot params database");
			ObjectMapper mapper = new ObjectMapper();
			mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
			
			try
			{
				return mapper.readValue(file, BotParamsDatabase.class);
			} catch (IOException e)
			{
				log.error("Could not read from database: " + file, e);
			}
		}
		log.info("Initializing empty bot params database");
		return new BotParamsDatabase();
	}
	
	
	@Override
	public void onEntryAdded(final String entry, final BotParams newParams)
	{
		saveDatabase();
	}
	
	
	@Override
	public void onEntryUpdated(final String entry, final BotParams newParams)
	{
		saveDatabase();
		
		database.getSelectedParams().entrySet().stream()
				.filter(e -> e.getValue().equals(entry))
				.forEach(e -> notifyBotParamsUpdated(e.getKey(), newParams));
	}
	
	
	@Override
	public void onBotParamLabelUpdated(final EBotParamLabel label, final String newEntry)
	{
		saveDatabase();
		
		notifyBotParamsUpdated(label, get(label));
	}
	
	
	/**
	 * @param observer
	 */
	public void addObserver(final IBotParamsManagerObserver observer)
	{
		observers.add(observer);
	}
	
	
	/**
	 * @param observer
	 */
	public void removeObserver(final IBotParamsManagerObserver observer)
	{
		observers.remove(observer);
	}
	
	
	private void notifyBotParamsUpdated(final EBotParamLabel label, final IBotParams params)
	{
		for (IBotParamsManagerObserver observer : observers)
		{
			observer.onBotParamsUpdated(label, params);
		}
	}
	
	
	/**
	 * BotParamsManager observer.
	 */
	@FunctionalInterface
	public interface IBotParamsManagerObserver
	{
		/**
		 * Bot parameters of a specific label have changed.
		 * 
		 * @param label
		 * @param params
		 */
		void onBotParamsUpdated(EBotParamLabel label, IBotParams params);
	}
}

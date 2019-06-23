/*
 * Copyright (c) 2009 - 2017, DHBW Mannheim - TIGERs Mannheim
 */
package edu.dhbw.mannheim.tigers.sumatra.view.ball;

import java.util.List;


/**
 * @author Nicolai Ommer <nicolai.ommer@gmail.com>
 */
public interface IBallAnalyserPanelObserver
{
	/**
	 * @param filename
	 */
	void onSave(String filename);
	
	
	/**
	 * @param record
	 * @param stopAutomatically
	 */
	void onRecord(boolean record, boolean stopAutomatically);
	
	
	/**
	 * @param selectedFiles
	 */
	void onDelete(List<String> selectedFiles);
	
	
	/**
	 * @param selectedFiles
	 */
	void onPlot(List<String> selectedFiles);
	
	
	/**
	 * @param selectedFiles
	 */
	void onCreateBallModel(List<String> selectedFiles);
	
	
	/**
	 * @param selectedFiles
	 */
	void onCreateBallAndKickModel(List<String> selectedFiles);
	
	
	/**
	 * @param selectedFiles
	 */
	void onNewSelectedFile(List<String> selectedFiles);
	
	
	/**
	 * @param selectedFiles
	 */
	void onCopy(List<String> selectedFiles);
	
	
	/**
	 * @param selectedFiles
	 */
	void onKalman(List<String> selectedFiles);
	
}

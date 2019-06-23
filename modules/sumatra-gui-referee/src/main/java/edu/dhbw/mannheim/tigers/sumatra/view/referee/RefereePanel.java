/*
 * Copyright (c) 2009 - 2017, DHBW Mannheim - TIGERs Mannheim
 */
package edu.dhbw.mannheim.tigers.sumatra.view.referee;

import java.awt.BorderLayout;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import edu.tigers.sumatra.ids.ETeamColor;
import edu.tigers.sumatra.views.ISumatraView;
import net.miginfocom.swing.MigLayout;


/**
 * Referee view.
 * 
 * @author Malte, DionH, FriederB
 */
public class RefereePanel extends JPanel implements ISumatraView
{
	// --------------------------------------------------------------------------
	// --- variables and constants ----------------------------------------------
	// --------------------------------------------------------------------------
	private static final long serialVersionUID = 5362158568331526086L;
	
	private final ShowRefereeMsgPanel showRefereeMsgPanel;
	private final CommonCommandsPanel commonCommandsPanel;
	private final ChangeStatePanel changeStatePanel;
	private final Map<ETeamColor, TeamPanel> teamsPanel = new EnumMap<>(ETeamColor.class);
	
	
	/** Constructor. */
	public RefereePanel()
	{
		setLayout(new BorderLayout());
		
		JPanel componentPanel = new JPanel();
		componentPanel.setLayout(new MigLayout("wrap 2", "[fill]10[fill]", ""));
		
		showRefereeMsgPanel = new ShowRefereeMsgPanel();
		commonCommandsPanel = new CommonCommandsPanel();
		changeStatePanel = new ChangeStatePanel();
		teamsPanel.put(ETeamColor.YELLOW, new TeamPanel(ETeamColor.YELLOW));
		teamsPanel.put(ETeamColor.BLUE, new TeamPanel(ETeamColor.BLUE));
		
		componentPanel.add(showRefereeMsgPanel, "spany 2, aligny top");
		componentPanel.add(commonCommandsPanel);
		componentPanel.add(changeStatePanel);
		componentPanel.add(teamsPanel.get(ETeamColor.YELLOW));
		componentPanel.add(teamsPanel.get(ETeamColor.BLUE));
		
		JScrollPane scrollPane = new JScrollPane(componentPanel);
		add(scrollPane, BorderLayout.CENTER);
	}
	
	
	/**
	 * @return the showRefereeMsgPanel
	 */
	public ShowRefereeMsgPanel getShowRefereeMsgPanel()
	{
		return showRefereeMsgPanel;
	}
	
	
	/**
	 * @param enable
	 */
	public void setEnable(final boolean enable)
	{
		commonCommandsPanel.setEnable(enable);
		changeStatePanel.setEnable(enable);
		teamsPanel.values().forEach(t -> t.setEnable(enable));
	}
	
	
	@Override
	public List<JMenu> getCustomMenus()
	{
		return null;
	}
	
	
	@Override
	public void onShown()
	{
		// nothing to do
	}
	
	
	@Override
	public void onHidden()
	{
		// nothing to do
	}
	
	
	@Override
	public void onFocused()
	{
		// nothing to do
	}
	
	
	@Override
	public void onFocusLost()
	{
		// nothing to do
	}
	
	
	/**
	 * @return the commonCommandsPanel
	 */
	public CommonCommandsPanel getCommonCommandsPanel()
	{
		return commonCommandsPanel;
	}
	
	
	/**
	 * @return the changeStatePanel
	 */
	public ChangeStatePanel getChangeStatePanel()
	{
		return changeStatePanel;
	}
	
	
	/**
	 * @return the teamsPanel
	 */
	public Map<ETeamColor, TeamPanel> getTeamsPanel()
	{
		return teamsPanel;
	}
}

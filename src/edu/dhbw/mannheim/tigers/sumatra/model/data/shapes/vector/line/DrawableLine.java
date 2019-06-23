/*
 * *********************************************************
 * Copyright (c) 2009 - 2013, DHBW Mannheim - Tigers Mannheim
 * Project: TIGERS - Sumatra
 * Date: Jan 14, 2013
 * Author(s): Nicolai Ommer <nicolai.ommer@gmail.com>
 * 
 * *********************************************************
 */
package edu.dhbw.mannheim.tigers.sumatra.model.data.shapes.vector.line;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;

import javax.persistence.Embeddable;

import edu.dhbw.mannheim.tigers.sumatra.model.data.math.GeoMath;
import edu.dhbw.mannheim.tigers.sumatra.model.data.shapes.IDrawableShape;
import edu.dhbw.mannheim.tigers.sumatra.model.data.shapes.vector.IVector2;
import edu.dhbw.mannheim.tigers.sumatra.model.data.shapes.vector.Vector2;
import edu.dhbw.mannheim.tigers.sumatra.view.visualizer.internals.field.FieldPanel;


/**
 * This is a Line connected to a color
 * 
 * @author Nicolai Ommer <nicolai.ommer@gmail.com>
 * 
 */
@Embeddable
public class DrawableLine extends Line implements IDrawableShape
{
	
	// --------------------------------------------------------------------------
	// --- variables and constants ----------------------------------------------
	// --------------------------------------------------------------------------
	
	/**  */
	private static final long	serialVersionUID			= 7006413582126880420L;
	
	private static final float	ARROW_HEAD_SIZE			= 7f;
	private static final float	ARROW_HEAD_SIZE_ANGLE	= 0.5f;
	private static final float	ARROW_TEXT_OFFSET_LINE	= 0f;
	private static final float	ARROW_TEXT_OFFSET_DIST	= 10f;
	
	private Color					color							= Color.red;
	private String					text							= "";
	private ETextLocation		textLocation				= ETextLocation.HEAD;
	private boolean				drawArrowHead				= true;
	private transient Stroke	stroke						= new BasicStroke(1);
	
	/**
	 * Specifies the location of the text string
	 */
	public enum ETextLocation
	{
		/**  */
		HEAD,
		/**  */
		CENTER,
		/**  */
		TAIL;
	}
	
	
	// --------------------------------------------------------------------------
	// --- constructors ---------------------------------------------------------
	// --------------------------------------------------------------------------
	
	
	/**
	 * Drawable line from normal line
	 * 
	 * @param line
	 * @param color
	 */
	public DrawableLine(final ILine line, final Color color)
	{
		this(line, color, true);
	}
	
	
	/**
	 * Drawable line from normal line
	 * 
	 * @param line
	 * @param color
	 * @param drawArrowHead
	 */
	public DrawableLine(final ILine line, final Color color, final boolean drawArrowHead)
	{
		super(line);
		this.color = color;
		this.drawArrowHead = drawArrowHead;
	}
	
	
	/**
	 * Drawable line from normal line
	 * 
	 * @param line
	 */
	public DrawableLine(final ILine line)
	{
		this(line, Color.red, true);
	}
	
	
	// --------------------------------------------------------------------------
	// --- methods --------------------------------------------------------------
	// --------------------------------------------------------------------------
	
	
	@Override
	public void paintShape(Graphics2D g)
	{
		g.setColor(getColor());
		g.setStroke(getStroke());
		
		// draw line
		final IVector2 lineStart = FieldPanel.transformToGuiCoordinates(supportVector());
		final IVector2 lineEnd = FieldPanel.transformToGuiCoordinates(directionVector().addNew(supportVector()));
		g.drawLine((int) lineStart.x(), (int) lineStart.y(), (int) lineEnd.x(), (int) lineEnd.y());
		
		// draw head
		if (isDrawArrowHead())
		{
			final IVector2 arrayHeadBase = lineStart.subtractNew(lineEnd).scaleToNew(ARROW_HEAD_SIZE);
			final IVector2 arrayHeadLeftEnd = arrayHeadBase.turnNew(-ARROW_HEAD_SIZE_ANGLE).add(lineEnd);
			final IVector2 arrayHeadRightEnd = arrayHeadBase.turnNew(ARROW_HEAD_SIZE_ANGLE).add(lineEnd);
			
			g.drawLine((int) lineEnd.x(), (int) lineEnd.y(), (int) arrayHeadLeftEnd.x(), (int) arrayHeadLeftEnd.y());
			g.drawLine((int) lineEnd.x(), (int) lineEnd.y(), (int) arrayHeadRightEnd.x(), (int) arrayHeadRightEnd.y());
		}
		
		// draw text
		if (!text.isEmpty())
		{
			Vector2 base = null;
			switch (textLocation)
			{
				case HEAD:
					base = GeoMath.stepAlongLine(lineEnd, lineStart, ARROW_TEXT_OFFSET_LINE);
					break;
				case TAIL:
					base = GeoMath.stepAlongLine(lineStart, lineEnd, ARROW_TEXT_OFFSET_LINE);
					break;
				case CENTER:
					base = GeoMath.stepAlongLine(lineStart, lineEnd, GeoMath.distancePP(lineStart, lineEnd) / 2);
					break;
			}
			if (base != null)
			{
				Vector2 dir = new Vector2(-base.y(), base.x()).scaleTo(ARROW_TEXT_OFFSET_DIST);
				if (dir.x() < 1)
				{
					dir.multiply(-1);
				}
				Vector2 txtbase = base.addNew(dir);
				g.setStroke(new BasicStroke(1));
				g.drawString(text, txtbase.x, txtbase.y);
			}
		}
	}
	
	
	// --------------------------------------------------------------------------
	// --- getter/setter --------------------------------------------------------
	// --------------------------------------------------------------------------
	
	/**
	 * @return the color
	 */
	public Color getColor()
	{
		return color;
	}
	
	
	/**
	 * @param color the color to set
	 */
	public void setColor(Color color)
	{
		this.color = color;
	}
	
	
	/**
	 * @return the text
	 */
	public final String getText()
	{
		return text;
	}
	
	
	/**
	 * @param text the text to set
	 */
	public final void setText(String text)
	{
		this.text = text;
	}
	
	
	/**
	 * @return the textLocation
	 */
	public final ETextLocation getTextLocation()
	{
		return textLocation;
	}
	
	
	/**
	 * @param textLocation the textLocation to set
	 */
	public final void setTextLocation(ETextLocation textLocation)
	{
		this.textLocation = textLocation;
	}
	
	
	/**
	 * @return the drawArrowHead
	 */
	public final boolean isDrawArrowHead()
	{
		return drawArrowHead;
	}
	
	
	/**
	 * @param drawArrowHead the drawArrowHead to set
	 */
	public final void setDrawArrowHead(boolean drawArrowHead)
	{
		this.drawArrowHead = drawArrowHead;
	}
	
	
	/**
	 * @return the stroke
	 */
	public final Stroke getStroke()
	{
		if (stroke == null)
		{
			stroke = new BasicStroke(1);
		}
		return stroke;
	}
	
	
	/**
	 * @param stroke the stroke to set
	 */
	public final void setStroke(Stroke stroke)
	{
		this.stroke = stroke;
	}
	
}

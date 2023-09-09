/* ===========================================================
 * JFreeChart : a free chart library for the Java(tm) platform
 * ===========================================================
 *
 * (C) Copyright 2000-2012, by Object Refinery Limited and Contributors.
 *
 * Project Info:  http://www.jfree.org/jfreechart/index.html
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * [Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.]
 *
 * ------------------
 * LevelRenderer.java
 * ------------------
 * (C) Copyright 2004-2012, by Object Refinery Limited.
 *
 * Original Author:  David Gilbert (for Object Refinery Limited);
 * Contributor(s):   Peter Kolb (patch 2511330);
 *
 * Changes
 * -------
 * 09-Jan-2004 : Version 1 (DG);
 * 05-Nov-2004 : Modified drawItem() signature (DG);
 * 20-Apr-2005 : Renamed CategoryLabelGenerator
 *               --> CategoryItemLabelGenerator (DG);
 * ------------- JFREECHART 1.0.x ---------------------------------------------
 * 23-Jan-2006 : Renamed getMaxItemWidth() --> getMaximumItemWidth() (DG);
 * 13-May-2008 : Code clean-up (DG);
 * 26-Jun-2008 : Added crosshair support (DG);
 * 23-Jan-2009 : Set more appropriate default shape in legend (DG);
 * 23-Jan-2009 : Added support for seriesVisible flags - see patch
 *               2511330 (PK)
 * 17-Jun-2012 : Removed JCommon dependencies (DG);
 *
 */

package org.jfree.chart.renderer.category;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;

import org.jfree.chart.HashUtilities;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.util.PublicCloneable;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.event.RendererChangeEvent;
import org.jfree.chart.labels.CategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.data.category.CategoryDataset;

/**
 * A {@link CategoryItemRenderer} that draws individual data items as
 * horizontal lines, spaced in the same way as bars in a bar chart.  The
 * example shown here is generated by the
 * <code>OverlaidBarChartDemo2.java</code> program included in the JFreeChart
 * Demo Collection:
 * <br><br>
 * <img src="../../../../../images/LevelRendererSample.png"
 * alt="LevelRendererSample.png" />
 */
public class LevelRenderer extends AbstractCategoryItemRenderer
        implements Cloneable, PublicCloneable, Serializable {

    /** For serialization. */
    private static final long serialVersionUID = -8204856624355025117L;

    /** The default item margin percentage. */
    public static final double DEFAULT_ITEM_MARGIN = 0.20;

    /** The margin between items within a category. */
    private double itemMargin;

    /** The maximum item width as a percentage of the available space. */
    private double maxItemWidth;

    /**
     * Creates a new renderer with default settings.
     */
    public LevelRenderer() {
        super();
        this.itemMargin = DEFAULT_ITEM_MARGIN;
        this.maxItemWidth = 1.0;  // 100 percent, so it will not apply unless
                                  // changed
        setDefaultLegendShape(new Rectangle2D.Float(-5.0f, -1.0f, 10.0f, 2.0f));
        // set the outline paint to fully transparent, then the legend shape
        // will just have the same colour as the lines drawn by the renderer
        setDefaultOutlinePaint(new Color(0, 0, 0, 0));
    }

    /**
     * Returns the item margin.
     *
     * @return The margin.
     *
     * @see #setItemMargin(double)
     */
    public double getItemMargin() {
        return this.itemMargin;
    }

    /**
     * Sets the item margin and sends a {@link RendererChangeEvent} to all
     * registered listeners.  The value is expressed as a percentage of the
     * available width for plotting all the bars, with the resulting amount to
     * be distributed between all the bars evenly.
     *
     * @param percent  the new margin.
     *
     * @see #getItemMargin()
     */
    public void setItemMargin(double percent) {
        this.itemMargin = percent;
        fireChangeEvent();
    }

    /**
     * Returns the maximum width, as a percentage of the available drawing
     * space.
     *
     * @return The maximum width.
     *
     * @see #setMaximumItemWidth(double)
     */
    public double getMaximumItemWidth() {
        return this.maxItemWidth;
    }

    /**
     * Sets the maximum item width, which is specified as a percentage of the
     * available space for all items, and sends a {@link RendererChangeEvent}
     * to all registered listeners.
     *
     * @param percent  the percent.
     *
     * @see #getMaximumItemWidth()
     */
    public void setMaximumItemWidth(double percent) {
        this.maxItemWidth = percent;
        fireChangeEvent();
    }

    /**
     * Initialises the renderer and returns a state object that will be passed
     * to subsequent calls to the drawItem method.
     * <p>
     * This method gets called once at the start of the process of drawing a
     * chart.
     *
     * @param g2  the graphics device.
     * @param dataArea  the area in which the data is to be plotted.
     * @param plot  the plot.
     * @param rendererIndex  the renderer index.
     * @param info  collects chart rendering information for return to caller.
     *
     * @return The renderer state.
     */
    @Override
    public CategoryItemRendererState initialise(Graphics2D g2,
            Rectangle2D dataArea, CategoryPlot plot, int rendererIndex,
            PlotRenderingInfo info) {

        CategoryItemRendererState state = super.initialise(g2, dataArea, plot,
                rendererIndex, info);
        calculateItemWidth(plot, dataArea, rendererIndex, state);
        return state;

    }

    /**
     * Calculates the bar width and stores it in the renderer state.
     *
     * @param plot  the plot.
     * @param dataArea  the data area.
     * @param rendererIndex  the renderer index.
     * @param state  the renderer state.
     */
    protected void calculateItemWidth(CategoryPlot plot,
            Rectangle2D dataArea, int rendererIndex,
            CategoryItemRendererState state) {

        CategoryAxis domainAxis = getDomainAxis(plot, rendererIndex);
        CategoryDataset dataset = plot.getDataset(rendererIndex);
        if (dataset != null) {
            int columns = dataset.getColumnCount();
            int rows = state.getVisibleSeriesCount() >= 0
                    ? state.getVisibleSeriesCount() : dataset.getRowCount();
            double space = 0.0;
            PlotOrientation orientation = plot.getOrientation();
            if (orientation == PlotOrientation.HORIZONTAL) {
                space = dataArea.getHeight();
            }
            else if (orientation == PlotOrientation.VERTICAL) {
                space = dataArea.getWidth();
            }
            double maxWidth = space * getMaximumItemWidth();
            double categoryMargin = 0.0;
            double currentItemMargin = 0.0;
            if (columns > 1) {
                categoryMargin = domainAxis.getCategoryMargin();
            }
            if (rows > 1) {
                currentItemMargin = getItemMargin();
            }
            double used = space * (1 - domainAxis.getLowerMargin()
                                     - domainAxis.getUpperMargin()
                                     - categoryMargin - currentItemMargin);
            if ((rows * columns) > 0) {
                state.setBarWidth(Math.min(used / (rows * columns), maxWidth));
            }
            else {
                state.setBarWidth(Math.min(used, maxWidth));
            }
        }
    }

    /**
     * Calculates the coordinate of the first "side" of a bar.  This will be
     * the minimum x-coordinate for a vertical bar, and the minimum
     * y-coordinate for a horizontal bar.
     *
     * @param plot  the plot.
     * @param orientation  the plot orientation.
     * @param dataArea  the data area.
     * @param domainAxis  the domain axis.
     * @param state  the renderer state (has the bar width precalculated).
     * @param row  the row index.
     * @param column  the column index.
     *
     * @return The coordinate.
     */
    protected double calculateBarW0(CategoryPlot plot,
                                    PlotOrientation orientation,
                                    Rectangle2D dataArea,
                                    CategoryAxis domainAxis,
                                    CategoryItemRendererState state,
                                    int row,
                                    int column) {
        // calculate bar width...
        double space;
        if (orientation == PlotOrientation.HORIZONTAL) {
            space = dataArea.getHeight();
        }
        else {
            space = dataArea.getWidth();
        }
        double barW0 = domainAxis.getCategoryStart(column, getColumnCount(),
                dataArea, plot.getDomainAxisEdge());
        int seriesCount = state.getVisibleSeriesCount();
        if (seriesCount < 0) {
            seriesCount = getRowCount();
        }
        int categoryCount = getColumnCount();
        if (seriesCount > 1) {
            double seriesGap = space * getItemMargin()
                    / (categoryCount * (seriesCount - 1));
            double seriesW = calculateSeriesWidth(space, domainAxis,
                    categoryCount, seriesCount);
            barW0 = barW0 + row * (seriesW + seriesGap)
                          + (seriesW / 2.0) - (state.getBarWidth() / 2.0);
        }
        else {
            barW0 = domainAxis.getCategoryMiddle(column, getColumnCount(),
                    dataArea, plot.getDomainAxisEdge()) - state.getBarWidth()
                    / 2.0;
        }
        return barW0;
    }

    /**
     * Draws the bar for a single (series, category) data item.
     *
     * @param g2  the graphics device.
     * @param state  the renderer state.
     * @param dataArea  the data area.
     * @param plot  the plot.
     * @param domainAxis  the domain axis.
     * @param rangeAxis  the range axis.
     * @param dataset  the dataset.
     * @param row  the row index (zero-based).
     * @param column  the column index (zero-based).
     * @param pass  the pass index.
     */
    @Override
    public void drawItem(Graphics2D g2, CategoryItemRendererState state,
            Rectangle2D dataArea, CategoryPlot plot, CategoryAxis domainAxis,
            ValueAxis rangeAxis, CategoryDataset dataset, int row, int column,
            int pass) {

        // nothing is drawn if the row index is not included in the list with
        // the indices of the visible rows...
        int visibleRow = state.getVisibleSeriesIndex(row);
        if (visibleRow < 0) {
            return;
        }

        // nothing is drawn for null values...
        Number dataValue = dataset.getValue(row, column);
        if (dataValue == null) {
            return;
        }

        double value = dataValue.doubleValue();

        PlotOrientation orientation = plot.getOrientation();
        double barW0 = calculateBarW0(plot, orientation, dataArea, domainAxis,
                state, visibleRow, column);
        RectangleEdge edge = plot.getRangeAxisEdge();
        double barL = rangeAxis.valueToJava2D(value, dataArea, edge);

        // draw the bar...
        Line2D line = null;
        double x;
        double y;
        if (orientation == PlotOrientation.HORIZONTAL) {
            x = barL;
            y = barW0 + state.getBarWidth() / 2.0;
            line = new Line2D.Double(barL, barW0, barL,
                    barW0 + state.getBarWidth());
        }
        else {
            x = barW0 + state.getBarWidth() / 2.0;
            y = barL;
            line = new Line2D.Double(barW0, barL, barW0 + state.getBarWidth(),
                    barL);
        }
        Stroke itemStroke = getItemStroke(row, column);
        Paint itemPaint = getItemPaint(row, column);
        g2.setStroke(itemStroke);
        g2.setPaint(itemPaint);
        g2.draw(line);

        CategoryItemLabelGenerator generator = getItemLabelGenerator(row,
                column);
        if (generator != null && isItemLabelVisible(row, column)) {
            drawItemLabel(g2, orientation, dataset, row, column, x, y,
                    (value < 0.0));
        }

        // submit the current data point as a crosshair candidate
        int datasetIndex = plot.indexOf(dataset);
        updateCrosshairValues(state.getCrosshairState(),
                dataset.getRowKey(row), dataset.getColumnKey(column), value,
                datasetIndex, barW0, barL, orientation);

        // collect entity and tool tip information...
        EntityCollection entities = state.getEntityCollection();
        if (entities != null) {
            addItemEntity(entities, dataset, row, column, line.getBounds());
        }

    }

    /**
     * Calculates the available space for each series.
     *
     * @param space  the space along the entire axis (in Java2D units).
     * @param axis  the category axis.
     * @param categories  the number of categories.
     * @param series  the number of series.
     *
     * @return The width of one series.
     */
    protected double calculateSeriesWidth(double space, CategoryAxis axis,
                                          int categories, int series) {
        double factor = 1.0 - getItemMargin() - axis.getLowerMargin()
                        - axis.getUpperMargin();
        if (categories > 1) {
            factor = factor - axis.getCategoryMargin();
        }
        return (space * factor) / (categories * series);
    }

    /**
     * Returns the Java2D coordinate for the middle of the specified data item.
     *
     * @param rowKey  the row key.
     * @param columnKey  the column key.
     * @param dataset  the dataset.
     * @param axis  the axis.
     * @param area  the drawing area.
     * @param edge  the edge along which the axis lies.
     *
     * @return The Java2D coordinate.
     *
     * @since 1.0.11
     */
    @Override
    public double getItemMiddle(Comparable rowKey, Comparable columnKey,
            CategoryDataset dataset, CategoryAxis axis, Rectangle2D area,
            RectangleEdge edge) {
        return axis.getCategorySeriesMiddle(columnKey, rowKey, dataset,
                this.itemMargin, area, edge);
    }

    /**
     * Tests an object for equality with this instance.
     *
     * @param obj  the object (<code>null</code> permitted).
     *
     * @return A boolean.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof LevelRenderer)) {
            return false;
        }
        LevelRenderer that = (LevelRenderer) obj;
        if (this.itemMargin != that.itemMargin) {
            return false;
        }
        if (this.maxItemWidth != that.maxItemWidth) {
            return false;
        }
        return super.equals(obj);
    }

    /**
     * Returns a hash code for this instance.
     *
     * @return A hash code.
     */
    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = HashUtilities.hashCode(hash, this.itemMargin);
        hash = HashUtilities.hashCode(hash, this.maxItemWidth);
        return hash;
    }

}

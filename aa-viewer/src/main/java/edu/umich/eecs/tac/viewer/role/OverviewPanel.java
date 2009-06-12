package edu.umich.eecs.tac.viewer.role;

import edu.umich.eecs.tac.viewer.TACAASimulationPanel;
import edu.umich.eecs.tac.viewer.ViewListener;
import edu.umich.eecs.tac.viewer.TACAAViewerConstants;
import edu.umich.eecs.tac.TACAAConstants;
import edu.umich.eecs.tac.props.RetailCatalog;

import java.awt.*;

import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.data.xy.XYSeries;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.ui.RectangleInsets;
import com.botbox.util.ArrayUtils;
import se.sics.isl.transport.Transportable;
import se.sics.tasim.viewer.TickListener;

import javax.swing.*;

/**
 * @author Patrick Jordan
 */
public class OverviewPanel extends SimulationTabPanel {
	private XYSeriesCollection seriescollection;

	private int[] agents;
	private int[] roles;
	private int[] participants;
	private XYSeries[] series;
	private String[] names;
	private int agentCount;
	private int currentDay;

	public OverviewPanel(TACAASimulationPanel simulationPanel) {
		super(simulationPanel);
        setBorder(BorderFactory.createTitledBorder("Advertiser Profits"));
        setBackground(TACAAViewerConstants.CHART_BACKGROUND);
        
		agents = new int[0];
		roles = new int[0];
		participants = new int[0];
		series = new XYSeries[0];
		names = new String[0];
		currentDay = 0;
		initialize();

		getSimulationPanel().addTickListener(new DayListener());
		getSimulationPanel().addViewListener(new BankStatusListener());
	}

	protected void initialize() {
		setLayout(new GridLayout(1, 1));
		seriescollection = new XYSeriesCollection();
		JFreeChart chart = createChart(seriescollection);
		ChartPanel chartpanel = new ChartPanel(chart, false);
		chartpanel.setMouseZoomable(true, false);

		add(chartpanel);
	}

	private JFreeChart createChart(XYDataset xydataset) {
		JFreeChart jfreechart = ChartFactory.createXYLineChart(
				null, "Day", "$", xydataset,
				PlotOrientation.VERTICAL, true, true, false);
		jfreechart.setBackgroundPaint(TACAAViewerConstants.CHART_BACKGROUND);
		XYPlot xyplot = (XYPlot) jfreechart.getPlot();
		xyplot.setBackgroundPaint(TACAAViewerConstants.CHART_BACKGROUND);
		xyplot.setDomainGridlinePaint(Color.GRAY);
		xyplot.setRangeGridlinePaint(Color.GRAY);
		xyplot.setAxisOffset(new RectangleInsets(5D, 5D, 5D, 5D));
		//xyplot.setDomainCrosshairVisible(true);
		//xyplot.setRangeCrosshairVisible(true);

        LegendTitle legendTitle = jfreechart.getLegend();
        legendTitle.setBackgroundPaint(TACAAViewerConstants.CHART_BACKGROUND);
        legendTitle.setFrame(BlockBorder.NONE);
		XYItemRenderer xyitemrenderer = xyplot.getRenderer();

		xyitemrenderer.setBaseStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));

        xyplot.setOutlineVisible(false);
        
		if (xyitemrenderer instanceof XYLineAndShapeRenderer) {
			XYLineAndShapeRenderer xylineandshaperenderer = (XYLineAndShapeRenderer) xyitemrenderer;
			xylineandshaperenderer.setBaseShapesVisible(false);

            for(int i = 0; i < 8; i++){
                xylineandshaperenderer.setSeriesPaint(i, getSimulationPanel().legendColors[i]);
            }

		}
		return jfreechart;
	}

	protected void addAgent(int agent) {
		int index = ArrayUtils.indexOf(agents, 0, agentCount, agent);
		if (index < 0) {
			doAddAgent(agent);
		}
	}

	private int doAddAgent(int agent) {
		if (agentCount == participants.length) {
			int newSize = agentCount + 8;
			agents = ArrayUtils.setSize(agents, newSize);
			roles = ArrayUtils.setSize(roles, newSize);
			participants = ArrayUtils.setSize(participants, newSize);
			series = (XYSeries[]) ArrayUtils.setSize(series, newSize);
			names = (String[]) ArrayUtils.setSize(names, newSize);
		}

		agents[agentCount] = agent;

		return agentCount++;
	}

	private void setAgent(int agent, int role, String name, int participantID) {
		addAgent(agent);

		int index = ArrayUtils.indexOf(agents, 0, agentCount, agent);
		roles[index] = role;
		names[index] = name;
		participants[index] = participantID;

		if (series[index] == null && TACAAConstants.ADVERTISER == roles[index]) {
			series[index] = new XYSeries(name);
			seriescollection.addSeries(series[index]);
		}
	}

	protected void participant(int agent, int role, String name,
			int participantID) {
		setAgent(agent, role, name, participantID);
	}

	protected void dataUpdated(int agent, int type, double value) {
		int index = ArrayUtils.indexOf(agents, 0, agentCount, agent);
		if (index < 0 || series[index] == null
				|| type != TACAAConstants.DU_BANK_ACCOUNT) {
			return;
		}

		series[index].addOrUpdate(currentDay, value);
	}

	protected void tick(long serverTime) {
	}

	protected void simulationTick(long serverTime, int simulationDate) {
		currentDay = simulationDate;
	}

	protected class DayListener implements TickListener {

		public void tick(long serverTime) {
			OverviewPanel.this.tick(serverTime);
		}

		public void simulationTick(long serverTime, int simulationDate) {
			OverviewPanel.this.simulationTick(serverTime, simulationDate);
		}
	}

	protected class BankStatusListener implements ViewListener {

		public void dataUpdated(int agent, int type, int value) {

		}

		public void dataUpdated(int agent, int type, long value) {

		}

		public void dataUpdated(int agent, int type, float value) {

		}

		public void dataUpdated(int agent, int type, double value) {
			OverviewPanel.this.dataUpdated(agent, type, value);
		}

		public void dataUpdated(int agent, int type, String value) {

		}

		public void dataUpdated(int agent, int type, Transportable value) {

		}

		public void dataUpdated(int type, Transportable value) {

		}

		public void participant(int agent, int role, String name,
				int participantID) {
			OverviewPanel.this.participant(agent, role, name, participantID);
		}
	}
}
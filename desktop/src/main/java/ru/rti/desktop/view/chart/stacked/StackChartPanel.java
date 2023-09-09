package ru.rti.desktop.view.chart.stacked;

import java.awt.BorderLayout;
import java.awt.Color;
import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.JPanel;
import lombok.extern.log4j.Log4j2;
import org.fbase.core.FStore;
import org.fbase.model.output.StackedColumn;
import org.fbase.model.profile.CProfile;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.util.IDetailPanel;
import ru.rti.desktop.config.prototype.chart.WorkspaceChartModule;
import ru.rti.desktop.config.prototype.query.WorkspaceQueryComponent;
import ru.rti.desktop.exception.NotFoundException;
import ru.rti.desktop.manager.ProfileManager;
import ru.rti.desktop.model.ProfileTaskQueryKey;
import ru.rti.desktop.model.chart.CategoryTableXYDatasetRealTime;
import ru.rti.desktop.model.config.Metric;
import ru.rti.desktop.model.function.ChartType;
import ru.rti.desktop.model.info.QueryInfo;
import ru.rti.desktop.model.info.gui.ChartInfo;
import ru.rti.desktop.model.view.ProcessType;
import ru.rti.desktop.model.view.RangeChartHistory;
import ru.rti.desktop.router.event.EventListener;
import ru.rti.desktop.router.listener.CollectStartStopListener;
import ru.rti.desktop.router.listener.ShowLocalHistoryListener;
import ru.rti.desktop.state.SqlQueryState;
import ru.rti.desktop.view.chart.DetailChart;
import ru.rti.desktop.view.chart.FunctionDataHandler;
import ru.rti.desktop.view.chart.HelperChart;
import ru.rti.desktop.view.chart.StackedChart;
import ru.rti.desktop.view.chart.stacked.function.AsIsMetricFunctionHandler;
import ru.rti.desktop.view.chart.stacked.function.AverageMetricFunctionHandler;
import ru.rti.desktop.view.chart.stacked.function.CountMetricFunctionHandler;
import ru.rti.desktop.view.chart.stacked.function.SumMetricFunctionHandler;

@Log4j2
public abstract class StackChartPanel extends JPanel implements DetailChart, HelperChart,
        CollectStartStopListener, ShowLocalHistoryListener {

    protected static int MAX_POINT_PER_GRAPH = 300;

    private final WorkspaceQueryComponent workspaceQueryComponent;
    protected final CategoryTableXYDatasetRealTime categoryTableXYDatasetRealTime;
    protected final ProfileTaskQueryKey profileTaskQueryKey;
    protected final QueryInfo queryInfo;
    protected final ChartInfo chartInfo;
    protected final ProcessType processType;

    protected final Metric metric;
    protected FunctionDataHandler dataHandler;

    protected StackedChart stackedChart;
    private ChartPanel chartPanel;
    private JFreeChart jFreeChart;
    private DateAxis dateAxis;

    private String xAxisLabel = "xAxisLabel";
    private String yAxisLabel = "Value";
    protected int legendFontSize = 12;

    protected Set<String> series;

    protected long clientBegin;
    protected int batchSize;

    protected long begin;
    protected long end;

    protected double range;

    @Inject
    @Named("eventListener")
    EventListener eventListener;

    @Inject
    @Named("profileManager")
    ProfileManager profileManager;

    @Inject
    @Named("localDB")
    FStore fStore;

    @Inject
    @Named("sqlQueryState")
    SqlQueryState sqlQueryState;

    public StackChartPanel(WorkspaceQueryComponent workspaceQueryComponent,
                           CategoryTableXYDatasetRealTime categoryTableXYDatasetRealTime,
                           ProfileTaskQueryKey profileTaskQueryKey,
                           QueryInfo queryInfo,
                           ChartInfo chartInfo,
                           ProcessType processType,
                           Metric metric) {
        this.workspaceQueryComponent = workspaceQueryComponent;
        this.workspaceQueryComponent.initChart(new WorkspaceChartModule(this)).inject(this);
        this.categoryTableXYDatasetRealTime = categoryTableXYDatasetRealTime;
        this.profileTaskQueryKey = profileTaskQueryKey;
        this.queryInfo = queryInfo;
        this.chartInfo = chartInfo;
        this.processType = processType;
        this.metric = metric;

        this.initFunctionDataHandler(metric);

        if (metric.getYAxis().getCsType() == null) {
            throw new NotFoundException("Column storage type is undefined for column profile: " + metric.getYAxis());
        }

        this.series = new LinkedHashSet<>();

        if (ProcessType.REAL_TIME.equals(processType)) {
            this.eventListener.addCollectStartStopListener(profileTaskQueryKey, this);
            this.eventListener.addShowLocalHistoryListener(this);
        }

        this.range = (double) getRangeRealTime(chartInfo) / MAX_POINT_PER_GRAPH;

        this.batchSize = Math.toIntExact(Math.round((double) (getRangeRealTime(chartInfo) / 1000) / MAX_POINT_PER_GRAPH));

        this.stackedChart = new StackedChart(getChartPanel(this.categoryTableXYDatasetRealTime));
        this.stackedChart.setLegendFontSize(legendFontSize);
        this.stackedChart.initialize();
    }

    private void initFunctionDataHandler(Metric metric) {
        switch (metric.getMetricFunction()) {
            case ASIS -> dataHandler = new AsIsMetricFunctionHandler(metric, queryInfo, fStore);
            case COUNT -> dataHandler = new CountMetricFunctionHandler(metric, queryInfo, fStore);
            case SUM -> dataHandler = new SumMetricFunctionHandler(metric, queryInfo, fStore);
            case AVERAGE -> dataHandler = new AverageMetricFunctionHandler(metric, queryInfo, fStore);
        }
    }

    public abstract void initialize();

    protected abstract void loadData();

    protected void initializeRealTime() {
        this.loadData();

        this.setLayout(new BorderLayout());
        this.add("Center", stackedChart.getChartPanel());
    }

    protected void initializeHistory() {
        if (RangeChartHistory.WEEK.equals(chartInfo.getRangeHistory())
                || RangeChartHistory.MONTH.equals(chartInfo.getRangeHistory())) {
            this.stackedChart.setDateAxisWeekAndMore();
        }

        this.loadData();

        this.setLayout(new BorderLayout());
        this.add("Center", stackedChart.getChartPanel());
    }

    public Map<String, Color> getSeriesColorMap() {
        return stackedChart.getSeriesColorMap();
    }

    protected ChartPanel getChartPanel(CategoryTableXYDatasetRealTime categoryTableXYDataset) {
        xAxisLabel = metric.getYAxis().getColName();
        dateAxis = new DateAxis();
        jFreeChart = ChartFactory.createStackedXYAreaChart("", xAxisLabel, yAxisLabel, categoryTableXYDataset,
                PlotOrientation.VERTICAL, dateAxis, false, true, false);
        chartPanel = new ChartPanel(jFreeChart);

        return chartPanel;
    }

    protected void fillSeries(List<StackedColumn> sColumnList) {
        sColumnList.stream()
            .map(StackedColumn::getKeyCount)
            .map(Map::keySet)
            .flatMap(Collection::stream)
            .forEach(series::add);
    }

    protected void fillWithEmptyStackedChart(long beginRangeFill, long endRangeFill) {
        log.info("### Fill empty ###");
        log.info(toLocalDateTimeOfEpochMilli(beginRangeFill));
        log.info(toLocalDateTimeOfEpochMilli(endRangeFill));

        if (ChartType.LINEAR.equals(metric.getChartType())) {
            if (beginRangeFill > endRangeFill) {
                try {
                    stackedChart.addSeriesValue(beginRangeFill, 0.0, metric.getYAxis().getColName());
                } catch (Exception exception) {
                    log.info(exception);
                }
            }

            for (long x = beginRangeFill; x <= endRangeFill; x += Math.round(range)) {
                try {
                    stackedChart.addSeriesValue(x, 0.0, metric.getYAxis().getColName());
                } catch (Exception exception) {
                    log.info(exception);
                }
            }
        } else {
            if (beginRangeFill > endRangeFill) {
                series.forEach(seriesName -> {
                    try {
                        stackedChart.addSeriesValue(beginRangeFill, 0.0, seriesName);
                    } catch (Exception exception) {
                        log.info(exception);
                    }
                });
            }

            for (long x = beginRangeFill; x <= endRangeFill; x += Math.round(range)) {
                long finalX = x;
                series.forEach(seriesName -> {
                    try {
                        stackedChart.addSeriesValue(finalX, 0.0, seriesName);
                    } catch (Exception exception) {
                        log.info(exception);
                    }
                });
            }
        }
    }

    protected String getHourMinSec(long duration) {
        if (duration < 0) {
            return "Local time lagging behind than server one at: " + getDurationAbs(Math.abs(duration));
        } else if (duration > 0) {
            return "Local time ahead of server one at: " + getDurationAbs(duration);
        } else {
            return "Local and server time are synchronous";
        }
    }

    private String getDurationAbs(long duration) {
        Duration d = Duration.ofMillis(duration);

        long HH = d.toHours();
        long MM = d.toMinutesPart();
        long SS = d.toSecondsPart();
        long MS = d.toMillisPart();
        return String.format("%02d hour %02d minute %02d seconds %03d milliseconds", HH, MM, SS, MS);
    }

    @Override
    public void fireOnStartCollect(ProfileTaskQueryKey profileTaskQueryKey) {
        log.info("Start collect for " + profileTaskQueryKey);
        begin = sqlQueryState.getLastTimestamp(profileTaskQueryKey);
    }

    @Override
    public void fireOnStopCollect(ProfileTaskQueryKey profileTaskQueryKey) {
        log.info("Stop collect for " + profileTaskQueryKey);

        try {
            end = sqlQueryState.getLastTimestamp(profileTaskQueryKey);
            this.loadData();
        } catch (Exception e) {
            log.catching(e);
        }
    }

    @Override
    public void addChartListenerReleaseMouse(IDetailPanel l) {
        stackedChart.addChartListenerReleaseMouse(l);
    }

    @Override
    public void removeChartListenerReleaseMouse(IDetailPanel l) {
        stackedChart.removeChartListenerReleaseMouse(l);
    }

    @Override
    public void fireOnShowHistory(QueryInfo queryInfo, CProfile cProfile, long begin, long end) {
        log.info("fireOnShowHistory " + cProfile);
    }
}

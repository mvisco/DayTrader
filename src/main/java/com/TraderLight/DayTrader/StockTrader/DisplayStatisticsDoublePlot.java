package com.TraderLight.DayTrader.StockTrader;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

import com.TraderLight.DayTrader.MarketDataProvider.Level1Quote;

public class DisplayStatisticsDoublePlot extends ApplicationFrame  {
	
    /** The number of subplots. */
	// Subplot 1 price,  mean, subplot 2 volume 	
    public static final int SUBPLOT_COUNT = 2;
    public static final int datasetInOnePlot = 2;
    
    /** The datasets. */
    private TimeSeriesCollection[] datasets;
    
    /** The most recent value added to series 1. */
    private double[] lastValue = new double[SUBPLOT_COUNT];
    private int volValue = 0;
    
    
    public DisplayStatisticsDoublePlot(final String title) {

        super(title);
        
        final CombinedDomainXYPlot plot = new CombinedDomainXYPlot(new DateAxis("Time"));
        
        this.datasets = new TimeSeriesCollection[SUBPLOT_COUNT+1];
        
        for (int i = 0; i < SUBPLOT_COUNT; i++) {
            // this.lastValue[i] = 100.0;
     
			if (i==0) {
				// Price and mean sub-plot
				final TimeSeries series1 = new TimeSeries("Price");
				this.datasets[0] = new TimeSeriesCollection(series1);
				final TimeSeries series2 = new TimeSeries("Mean");
				this.datasets[1] = new TimeSeriesCollection(series2);				
				final NumberAxis rangeAxis = new NumberAxis("Price");
				rangeAxis.setAutoRangeIncludesZero(false);
				final XYPlot subplot = new XYPlot(null, null, rangeAxis, new StandardXYItemRenderer());					
				subplot.setBackgroundPaint(Color.lightGray);
				subplot.setDomainGridlinePaint(Color.white);
				subplot.setRangeGridlinePaint(Color.white);	
				subplot.setDataset(0, datasets[0]);
				subplot.setDataset(1,datasets[1]);
				subplot.setRenderer(1, new StandardXYItemRenderer());
				plot.add(subplot);
								
			} else {
				// volume sub-plot
				final TimeSeries series = new TimeSeries("Volume");
				this.datasets[2] = new TimeSeriesCollection(series);
				final NumberAxis rangeAxis = new NumberAxis("Volume");
				rangeAxis.setAutoRangeIncludesZero(false);
				final XYPlot subplot = new XYPlot(
                    this.datasets[2], null, rangeAxis, new StandardXYItemRenderer()
			    );
				subplot.setBackgroundPaint(Color.lightGray);
				subplot.setDomainGridlinePaint(Color.white);
				subplot.setRangeGridlinePaint(Color.white);
				plot.add(subplot);				
			}
        }
        
        final JFreeChart chart = new JFreeChart("Display Statistics", plot);
        chart.setBorderPaint(Color.black);
        chart.setBorderVisible(true);
        chart.setBackgroundPaint(Color.white);
      
        plot.setBackgroundPaint(Color.lightGray);
        plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinePaint(Color.white);
        final ValueAxis axis = plot.getDomainAxis();
        axis.setAutoRange(true);
        axis.setFixedAutoRange(60000.0);  // 60 seconds
      
        final JPanel content = new JPanel(new BorderLayout());

        final ChartPanel chartPanel = new ChartPanel(chart);
        content.add(chartPanel);

        final JPanel buttonPanel = new JPanel(new FlowLayout());
      
      
        content.add(buttonPanel, BorderLayout.SOUTH);
        chartPanel.setPreferredSize(new java.awt.Dimension(500, 470));
        chartPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        setContentPane(content);

    }
    
    public  void updateDatasets (double price, double mean, int volume) {
    	
    	
        for (int i = 0; i < SUBPLOT_COUNT; i++) {
        	final Millisecond now = new Millisecond();
            // System.out.println("Now = " + now.toString());
            
            if (i==0) {              
                this.lastValue[0] = price;
                this.datasets[0].getSeries(0).add(new Millisecond(), this.lastValue[0]);  
                this.lastValue[1] = mean;
                this.datasets[1].getSeries(0).add(new Millisecond(), this.lastValue[1]);
                
            
            }
            else {
            	this.volValue = volume;
                this.datasets[2].getSeries(0).add(new Millisecond(), this.volValue);
            }
        }   	
    	
    }
    


    
    /**
     * Starting point for the demonstration application.
     *
     * @param args  ignored.
     */
    public static void main(final String[] args) {

        final DisplayStatisticsDoublePlot demo = new DisplayStatisticsDoublePlot("DisplayStatistics");
        demo.pack();
        RefineryUtilities.centerFrameOnScreen(demo);
        demo.setVisible(true);
        
        double price = 780.00;
        double mean = 778.00;
        int volume = 100;
        
        // update datasets
        for (int i=0; i<100; i++) {
        	
        	demo.updateDatasets(price + i, mean, volume + i*100);
        	
        	try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	
        }

        System.exit(0);
    }


}

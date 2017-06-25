package com.TraderLight.DayTrader.Strategy;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.jfree.util.Log;

import com.TraderLight.DayTrader.MarketDataProvider.Level1Quote;

public class StrategyMeanAnalysis {
	
	
	DescriptiveStatistics stats;
	DescriptiveStatistics diff_stats;
	Level1Quote prevQuote;
	double min = 0;
	double max = 0;
	double zero_value = 0.005; 
	int min_index;
	int max_index;
	int zero_index=0;
	
	 StrategyMeanAnalysis() {
		 
		 stats = new DescriptiveStatistics();
		 diff_stats = new DescriptiveStatistics();
		 prevQuote = null;
		 
	 }
	
	 void addQuote(Level1Quote newQuote) {
			double diff;
			// we do not want to add to sample space quotes that do not have difference in volumes. The reason is that 
			// there have been no trades between these two quotes so from a mean calculation stand point we do not want to include them.			
			if (prevQuote == null) {
				// this should happen only once when the program is started and the strategy receives the first message				
				stats.addValue(newQuote.getLast());
				prevQuote = newQuote;
			}			
			if (newQuote.getVolume() != prevQuote.getVolume()) {
				stats.addValue(newQuote.getLast());
			}			
			// this should not happen but just in case the sample space is empty add the last one
			if (stats.getN() == 0) {
				stats.addValue(newQuote.getLast());			
			}
			
			prevQuote = newQuote;
			diff = newQuote.getLast() - stats.getMean();
			diff_stats.addValue(diff);
			
			if (diff <= diff_stats.getMin()) {
				min = diff;
				min_index = (int)diff_stats.getN()-1;
			}
			if ( diff >= diff_stats.getMax()) {
				max = diff;
				max_index = (int)diff_stats.getN()-1;
			}
			if ( (diff >= -zero_value ) && (diff <= zero_value )) {
				
				if ( zero_index == 0 ) {
				    zero_index = (int)diff_stats.getN()-1;
				}
			}
			
			
	}
	
	boolean didMeanRevert() {
		

	    // The following  is the  algorithm to find the index of the root of the diff_stats contained between the min and max 
	    // whichever happens first. The idea is that it will give the time on when the diff_stats is 0 after either a min or a max 
	    // has been reached.
		if ( (min >=0 && max >= 0)) {
			return false;
		}
		
		
		if ( min_index > max_index) {
		    if ((zero_index < min_index) && (zero_index > max_index)) {
			    Log.info("Min, Max " + diff_stats.getElement(min_index) + " " + diff_stats.getElement(max_index));
			    return true;
		} else 
			if ((zero_index  > min_index) && (zero_index < max_index)) {
				Log.info("Min, Max " + diff_stats.getElement(min_index) + " " + diff_stats.getElement(max_index));
 			   return true;
			}
			
		}
		return false;
		

	    
	}
	
	double calculateMean() {
		return stats.getMean();
	}

}

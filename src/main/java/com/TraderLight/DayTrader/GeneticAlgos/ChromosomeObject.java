package com.TraderLight.DayTrader.GeneticAlgos;

import java.util.Random;

import org.apache.commons.math3.genetics.Chromosome;
import org.apache.log4j.Logger;

import com.TraderLight.DayTrader.StockTrader.Logging;

public class ChromosomeObject extends Chromosome{
	
	double objective_change;
	double loss;
	double profit;
	double fitness_value;
	public static final Logger log = Logging.getLogger(true);
	
	public ChromosomeObject() {
		
	}
	
	public void setChromosomeGenes(double objective_change,double profit, double loss) {
		
		this.objective_change = objective_change;
		this.profit = profit;
		this.loss=loss;
	}

	@Override
	public double fitness() {
		
		return this.fitness_value;
	}
	
	public double generateRandomDouble(double infLimit, double supLimit ) {
		
		double random = new Random().nextDouble();
		double randomValue = infLimit + (supLimit - infLimit) * random;
		//log.info(randomValue);
		int randomInt;
		if (randomValue > 1) {
			// use 1 decimal digit
			randomInt = (int) Math.round(randomValue*10);
			randomValue = randomInt/10.0;
		} else {
			// use 2 decimal digits
			randomInt = (int) Math.round(randomValue*100);
			randomValue = randomInt/100.0;
		}
		
		return randomValue;
	}
	
	public void setFitness(double fitnessValue) {
		this.fitness_value = fitnessValue;
	}
	
	public void printChromosome() {
		log.info("This is the Chromosome");
		log.info("objective_change " + objective_change);
		log.info("profit " + profit);
		log.info("loss " + loss);
		log.info("fitness_value " + fitness_value);
		log.info(" END OF CHROMOSOME DISPLAY");
		log.info(" ");
	}
	
	public static void main(String[] args) {
		
		ChromosomeObject c = new ChromosomeObject();
		log.info("Random value is " + c.generateRandomDouble(0.3,2));
		
	}
	
	
}

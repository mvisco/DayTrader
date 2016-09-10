package com.TraderLight.DayTrader.GeneticAlgos;

import java.util.List;


import org.apache.commons.math3.exception.NotPositiveException;
import org.apache.commons.math3.exception.NullArgumentException;
import org.apache.commons.math3.exception.NumberIsTooLargeException;
import org.apache.commons.math3.genetics.Chromosome;


public class PopulationChromosomes {
	
	
	List<ChromosomeObject> chromosomes;
	int populationLimit;

	
	public PopulationChromosomes(List<ChromosomeObject> chromosomes, int populationLimit) throws NullArgumentException, 
	                                                                                       NotPositiveException, NumberIsTooLargeException {
		this.chromosomes = chromosomes;
		this.populationLimit = populationLimit;
		
	}

	public void addChromosome(ChromosomeObject chromosome) {
	chromosomes.add(chromosome);
	}

	public List<ChromosomeObject> getChromosomes() {
		return chromosomes;
	}

	public int getPopulationLimit() {
		return populationLimit;
	}
	
	public Chromosome getFittest(List<ChromosomeObject> listChromosomes) {
		
		double fitness=Double.MIN_VALUE;
		ChromosomeObject fittest=null;
		
		for (ChromosomeObject c : listChromosomes) {
			if (c.getFitness() > fitness) {
				fitness=c.getFitness();
				fittest=c;
			}
		}
		return fittest;
	}
	
    public void sortChromosomes() {
    	
    	// chromosomes sorted from  lowest to highest fitness using insertion sort algorithm
    	
    	ChromosomeObject tempChromosome;
    	
    	for (int i = 1; i < this.chromosomes.size(); i++) {
    		tempChromosome = chromosomes.get(i);
    		int j =  i;
    		while ( (j>0) && (chromosomes.get(j-1).fitness() > tempChromosome.fitness()) ){
    			// swap chromosomes
    			chromosomes.set(j,chromosomes.get(j-1));
    			j--;   			
    		}
    		chromosomes.set(j,tempChromosome);
    		
    	}
    	   	
        return ;  	
    }
	
    public void printChromosomes() {
    	
    	for (ChromosomeObject c : chromosomes) {
    		c.printChromosome();
    	}
    }
	
	
}

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
//import java.util.StringJoiner;



import ec.util.MersenneTwisterFast;

import org.apache.commons.math3.distribution.PoissonDistribution;

import sim.engine.SimState;
import sim.engine.Steppable;


/**
 * 
 * @author lizramsey
 *collects demand, population, and number of agents after each time step (at 0.01, 1.01, etc.)
 *puts those into a txt file
 *for all input files, include an extra line of values at the bottom; i.e.
 *if there are 100 timesteps required, add one extra line to make 101 timesteps, so the dataCollector
 *can collect data at 100.1
 *
 */
public class DataCollector implements Steppable{
	public static double CumulativeDemand;
	public int percentConservers;
	public static String txtFileInput;
	public static int numAgents;
	public static int population;
	public static int modelPopulation;
	public static int numConservers;

	public DataCollector(){

	}

	public void step(SimState state) {
		JaipurResidentialWUOriginal jaipurWaterUse = (JaipurResidentialWUOriginal) state;
		double ratio = getConserverRatioThisTimeStep();
		//	    System.out.println("total# conservers: " + numConservers +  " total# agents: " + numAgents +
		//	    		"     ratio: " + ratio);

		updateTxtFile(JaipurResidentialWUOriginal.getCurrentJob() + "\t" + state.schedule.getTime() + "\t" +
				modelPopulation + "\t" + numAgents + "\t" + ratio +"\t"+ CumulativeDemand +"\n");
		System.out.println(state.schedule.getTime() + "\t" + CumulativeDemand);
		numAgents = 0;
		CumulativeDemand = 0.0;
		modelPopulation = 0;
		numConservers = 0;
		ratio = 0;
	}

	public static void updateTxtFile(String s){
		txtFileInput = txtFileInput + s;
	}

	public double getConserverRatioThisTimeStep(){
		double numConserversDub = (double)(numConservers);
		double numAgentsDub = (double)(numAgents);
		double ratio = numConserversDub/numAgentsDub;
		return ratio;
	}

	//	public int getNumConservers(List<HHwPlumbing> allHouseholdsThisStep){
	//		int numConservers = 0;
	//		for(HHwPlumbing hh : allHouseholdsThisStep){
	//			if(hh.isConserver){
	//				numConservers ++;
	//			}
	//		}
	//		return numConservers;
	//	}
	//	
	//	public int getNumAgents(List<HHwPlumbing> allHouseholdsThisStep){
	//		int numAgents = 0;
	//		for (HHwPlumbing hh: allHouseholdsThisStep){
	//			numAgents++;
	//		}
	//		return numAgents;
	//	}
}
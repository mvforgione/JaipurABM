

import java.io.*;
import java.util.*;

import sim.engine.*;
import ec.util.MersenneTwisterFast;

import java.util.Iterator;

import org.apache.commons.collections15.Factory;

import agape.generators.RandGenerator;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseGraph; 
import edu.uci.ics.jung.graph.util.Pair;

/**
 *
 * @author lizramsey
 *
 */


public class JaipurResidentialWUSmallWorldNetwork extends JaipurResidentialWUOriginal{
	public static String txtFileInput;
	//public static int jobs = 25; //number of runs needed
	public static int jobs = 3;
	public static int numStepsInMain = 10;
	//public static int numStepsInMain = 240;	//Update this for each run as you add more timesteps in excel doc 
	//also,should be double the number of lines of data in excel file, since datacollector needs its own step
	public static int currentJob = 0;
	public static double percentageConserverCurrent = 0.0;
	public static long discretization = (long) 1.0;
	public static int population;
	public static int numCurrentAgents = 0;
	public static double averageHouseholdSize = 5.1;
	//public static String populationCSVfile = "/Users/lizramsey/Documents/workspace/JaipurABM/src/AgentPopulation.csv";
	public static String populationCSVfile = "/Users/lizramsey/Documents/workspace/JaipurABM/src/PopTest.csv";
	//public static String outputFileName = "/Users/lizramsey/Documents/workspace/JaipurABM/GeneratedTXTs/utilfxntest_cons_5_withComms_with_delta_a_7_b_3_value_test_3May2016.txt";
	public static String outputFileName = "/Users/lizramsey/Documents/workspace/JaipurABM/GeneratedTXTs/testingFile.txt";
	public static int numMonthsInYear = 12;
	private static double rewiringP = 0.109; 	//from calculations based on mobile phone study & Barrat & Weigt paper
	private static int vertexNumber = 1;
	public static List<HHwPlumbingSmallWorldNetwork> houseHoldAgents = new ArrayList<HHwPlumbingSmallWorldNetwork>();
	private static List<HHwPlumbingSmallWorldNetwork> newAgentsAtThisTimeStep = new ArrayList<HHwPlumbingSmallWorldNetwork>();
	//public Continuous2D jaipurcity = new Continuous2D(discretization, northSouthBounds, eastWestBounds);


	public JaipurResidentialWUSmallWorldNetwork(long seed){
		super(seed);
	}


	
	public void start() {
		DataCollector dc = new DataCollector();
		double totalDemand = 0.0;
		schedule.clear();
		int[][] populationArray = scanInputCSV.popScan(populationCSVfile);
		super.start();
		createAgentPopulation(populationArray);
		schedule.scheduleRepeating(0.1, dc); //puts DataCollector on schedule
		//System.out.println("testing start method");
		}


	public static void main(String[] args) {
		
		SimState state = new JaipurResidentialWUSmallWorldNetwork(System.currentTimeMillis()); // MyModel is our SimState subclass
		for(int job = 0; job < jobs; job++){
			state.schedule.clear();
			state.setJob(job);
			currentJob++;
			state.start();
			txtFileInput = "Utility Function Values\ta value\t" + UtilityFunctionOriginal.a + "\ta prime value\t" + UtilityFunctionOriginal.aPrime +
					"\tb value\t" + UtilityFunctionOriginal.b + "\tb prime value\t" + UtilityFunctionOriginal.bPrime + "\n\n";
			txtFileInput =  txtFileInput + "Job Number\tTime Step\tModel Population\t# of Agents\tRatio of Conservers to Total Agents\tTotal Demand\n";		//create .txt file for outputting results
			do
				if (!state.schedule.step(state)) {
					break;
				}
			while(state.schedule.getSteps() < numStepsInMain);
			state.finish();
		}
		txtFileInput = txtFileInput + DataCollector.txtFileInput;
		generateTxtFile(txtFileInput);
		System.out.println("all runs finished, exiting");
		System.exit(0);
	}

	public static int getCurrentJob(){
		return currentJob;
	}



	public void createAgentPopulation(int [][] populationArray){
		
		int numTimeSteps = populationArray.length;
		for (int i = 0; i < numTimeSteps; i++){											//for each time step i
			newAgentsAtThisTimeStep.clear();											//remove all old agents from new agent array
			double double_i = (double) (i);
			population = populationArray[i][1];
			int numNewAgents = getNumNewAgents(populationArray, double_i);
			for (int j = 0; j < numNewAgents; j++){	//create number of new agents required
				//System.out.println(" num newAgents: " + getNumNewAgents(populationArray, double_i));
				HHwPlumbingSmallWorldNetwork newAgent = (HHwPlumbingSmallWorldNetwork) createNewAgent(populationArray, double_i);
				HHwPlumbingSmallWorldNetwork.houseHoldAgents.add(newAgent);
				//System.out.println("testing pre schedule " + this.vertexNumber);
				schedule.scheduleRepeating(double_i, newAgent);

			}
		}
		
	}
	
	public RandGenerator createNetwork(){
		RandGenerator newGenerator = new RandGenerator();
		return newGenerator;
	}
	
	
	/*
    Create number of agents at [n] time step and store in agent array.
     */
	public HHwPlumbingOriginal createNewAgent(int[][] populationArray, double timeStep) {
		HHwPlumbingSmallWorldNetwork hh = new HHwPlumbingSmallWorldNetwork(vertexNumber, timeStep);   //passes that property array to the new agents
		vertexNumber++;
		houseHoldAgents.add(hh);                    //Add household agent object to our household agent list
		return hh;
	}

	
	public static double getCumulativeDemand(){
		double totalDemand = 0.0;
		for(HHwPlumbingSmallWorldNetwork hh: houseHoldAgents){
			totalDemand = totalDemand + hh.monthlyDemand;
		}
		return totalDemand;
	}

	public int getNumNewAgents(int[][] population, double timeStep){
		int intTimeStep = (int) timeStep;
		int popCurrentTimeStep;
		int popPreviousTimeStep;
		int currentNumHouseholds;
		int previousNumHouseholds;
		int numNewHouseholds;
		int numTotalTimeSteps = population.length;
		if(intTimeStep < 0 || intTimeStep > numTotalTimeSteps - 1){ //red flag that schedule isn't running correctly
			//may cause errors when running schedule if you don't keep tabs on iterations
			System.out.println("incorrect time step");
			return -1;
		}
		popCurrentTimeStep = population[intTimeStep][1];
		double curNumHouseholds = popCurrentTimeStep / averageHouseholdSize;
		int currentNumHouseholdsInt = (int) (curNumHouseholds);
		if (intTimeStep > 1 || intTimeStep == 1){
			popPreviousTimeStep = population[intTimeStep - 1][1];
			double prevNumHouseholds = popPreviousTimeStep / averageHouseholdSize;
			double numNewHouseholdDouble = curNumHouseholds - prevNumHouseholds;
			numNewHouseholds = (int) (numNewHouseholdDouble);
			return numNewHouseholds;
		}else{
			numNewHouseholds = currentNumHouseholdsInt;
			return numNewHouseholds;
		}
	}

	private static void generateTxtFile(String input){
		for (int i = 0; i < jobs; i ++){	
			PrintStream outPrint = null;
			try{
			//	outputFileName = outputFileName;
				outPrint = new PrintStream(new File(outputFileName));
			}
			catch (FileNotFoundException e){
				System.out.println(outputFileName + " (No such file or directory)");
				System.exit(1);
			}
			outPrint.println(input);
		}
	}



}



	

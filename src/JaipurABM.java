

import java.io.*;
import java.util.*;

import org.apache.commons.collections15.Factory;
import org.graphstream.algorithm.generator.Generator;
import org.graphstream.algorithm.generator.WattsStrogatzGenerator;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;

import sim.engine.*;
import ec.util.MersenneTwisterFast;

/**
 *
 * @author lizramsey
 *
 */


public class JaipurABM extends SimState{

	//graphStructure acts as the seed to determine which social network to implement
	//"original" is for network broken down by friends, acquaintances, and families, selected randomly
	//"kleinbergSmallWorldNetwork" is obviously for Kleinberg Small World
	//add strings here as you write more code
	private static String graphStructure = "Kleinberg small world network";

	public static int numTotalAgents = 0;
	public static String txtFileInput;
	//public static int jobs = 25; //number of runs needed
	public static int jobs = 2;
	public static int numStepsInMain = 10;
	//public static int numStepsInMain = 240;	//Update this for each run as you add more timesteps in excel doc 
	//also,should be double the number of lines of data in excel file, since datacollector needs its own step
	public static int currentJob = 0;
	public static double percentageConserverCurrent = 0.0;
	public static int population;
	public static int numCurrentAgents = 0;
	public static double averageHouseholdSize = 5.1;
	//public static String populationCSVfile = "/Users/lizramsey/Documents/workspace/JaipurABM/src/AgentPopulation.csv";
	public static String populationCSVfile= "/Users/lizramsey/Documents/workspace/JaipurABM/src/PopTest.csv";
	//public static String outputFileName = "/Users/lizramsey/Documents/workspace/JaipurABM/GeneratedTXTs/utilfxntest_cons_5_withComms_with_delta_a_7_b_3_value_test_3May2016.txt";
	public static String outputFileName = "/Users/lizramsey/Documents/workspace/JaipurABM/GeneratedTXTs/testingFile.txt";
	public static int numMonthsInYear = 12;
	private static int vertexNumber = 1;

	public static List<Household> network = new ArrayList<Household>();
	public static List<Household> newAgentsAtThisTimeStepOriginalNetwork = new ArrayList<Household>();



	public JaipurABM(long seed){
		super(seed);
	}

	public static void main(String[] args) {
		SimState state = new JaipurABM(System.currentTimeMillis());
		for(int job = 0; job < jobs; job++){
			System.out.println("JOB NUMBER " + job);
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


	public void start() {
		DataCollector dc = new DataCollector();
		double totalDemand = 0.0;
		schedule.clear();
		int[][] populationArray = scanInputCSV.popScan(populationCSVfile);
		super.start();
		createAgentPopulation(populationArray);
		if(!graphStructure.equalsIgnoreCase("original")){	//the original network allows each household to store its own network;
			//all others take their network structure from the ABM itself
			if(graphStructure.equalsIgnoreCase("kleinberg small world network")){
				//call kleinberg generator
				Graph KBSWgraph = generateKleinbergSmallWorldSocialNetwork();
				createSocialNetwork(KBSWgraph);
			}
			else if(graphStructure.equalsIgnoreCase("some other network")){
				//call some other network, continue this for as many networks as you have
			}
			else{
				System.out.println("no network structure identified, exiting");
				System.exit(1);
			}

		}
		schedule.scheduleRepeating(0.1, dc); //puts DataCollector on schedule
	}

	//	public static int getCurrentJob(){
	//		return currentJob;
	//	}

	/*
    Create number of agents at [n] time step and store in agent array.
	 */
	public Household createNewAgent(int[][] populationArray, double timeStep) {
		Household hh = new Household(vertexNumber, timeStep);   //passes that property array to the new agents
		vertexNumber++;
		network.add(hh);         //Add household agent object to our household agent list
		return hh;
	}

	protected void createAgentPopulation(int [][] populationArray){
		int numTimeSteps = populationArray.length;
		for (int i = 0; i < numTimeSteps; i++){											//for each time step i
			newAgentsAtThisTimeStepOriginalNetwork.clear();											//remove all old agents from new agent array
			double double_i = (double) (i);
			population = populationArray[i][1];
			int numNewAgents = getNumNewAgents(populationArray, double_i);
			for (int j = 0; j < numNewAgents; j++){	//create number of new agents required
				//System.out.println(" num newAgents: " + getNumNewAgents(populationArray, double_i));
				Household newAgent = createNewAgent(populationArray, double_i);
				Household.houseHoldAgents.add(newAgent);

				schedule.scheduleRepeating(double_i, newAgent);
				numTotalAgents++;
				System.out.println("numTotalAgents test: " + numTotalAgents);
			}

		}

	}


	public static double getCumulativeDemand(){
		double totalDemand = 0.0;
		for(Household hh: network){
			totalDemand = totalDemand + hh.monthlyDemand;
		}
		return totalDemand;
	}

	protected int getNumNewAgents(int[][] population, double timeStep){
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

	protected static void generateTxtFile(String input){
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

	public static int getCurrentJob() {
		return currentJob;
	}

	public static String getGraphStructure() {
		return graphStructure;
	}

	public Graph generateKleinbergSmallWorldSocialNetwork(){
		System.out.println("hits KSW");
		Graph graph = new SingleGraph("JaipurResidents");
		if(numTotalAgents > 1){
			Generator gen = new WattsStrogatzGenerator(numTotalAgents, 2, 0.5);//n number of agents, num connections k, rewiring probability beta
			gen.addSink(graph);
			gen.begin();
			while(gen.nextEvents()){
				sleep();
			}
			gen.end();

			graph.display(false);
			return graph;
		}
		else{
			System.out.println("numTotalAgents is failing");
			System.exit(0);
			return graph;
		}
//		for (Node n: graph){
//			System.out.println("Text vertex: " + n.toString());
//			Collection<Edge> edgeCollection = n.getLeavingEdgeSet();
//			System.out.println(edgeCollection);
//			//System.out.println(Edge.getOpposite(n));
//			for (Edge edge: edgeCollection){
//				System.out.println(edge.getOpposite(n));
//			}
//		}
	}

	protected void sleep() {
		try { Thread.sleep(300); } catch (Exception e) {}
	}
	
	private void createSocialNetwork(Graph graph){
		//go through all households in the model
		for (Household hh : network){
			//get the current household's name
			String agentName = hh.getVertexName();
			//for this given node in the graph, find the corresponding agent
			for (Node n: graph){
				String nodePseudonym = "vert" + (n.getIndex()+1);
				//System.out.println("testing name for nodes: " +nodePseudonym)
				
				//if the node corresponds with the agent, then we get all the agent's connections and store them in the acq array
				if(agentName.equals(nodePseudonym)){
					hh.acquaintances = findNeighborArray(n);
				}

			}
			
		}
	}

	
	private ArrayList<Household> findNeighborArray(Node node){
		ArrayList<Household> neighborArray = new ArrayList<Household>(); 
		Collection<Edge> edgeCollection = node.getLeavingEdgeSet();
		for (Edge edge: edgeCollection){
			System.out.println("edge.getOpposite " + edge.getOpposite(node));
			Household neighborHousehold = findVertexHouseholdEquivalent(edge.getOpposite(node));
			neighborArray.add(neighborHousehold);
		}
		if(neighborArray.isEmpty()){
			System.out.println("Error in neighbor array");
			return null;
		}
		return neighborArray;
	}
	
//TODO: figure out how to keep this from returning null in any instance.
	
	//what's happening is that after the network is done iterating, it automatically returns null
	private Household findVertexHouseholdEquivalent(Node node){
		String vertexName = "vert" + node;
		
		for (Household hh: network){
			String comparedName = hh.getVertexName();

			if(!comparedName.equalsIgnoreCase(vertexName)){
				continue;
			}
			else if(comparedName.equalsIgnoreCase(vertexName)){
				UUID uuid = hh.getUUID();
				Household retHousehold = findHouseHoldFromUUID(uuid);
			}
			else{
				throw new IllegalArgumentException("error in VertexHouseholdEquivalent");
			}
		}
		return null;
	}
	
	private Household findHouseHoldFromUUID(UUID uuidFind){
		for (Household hh: network){
			if(hh.uuid == uuidFind){
				return hh;
			}
		}
		System.out.println("findHouseholdFromUUID isn't working");
		return null;
	}
	
}

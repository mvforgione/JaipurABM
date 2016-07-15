import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;
import org.apache.commons.math3.distribution.PoissonDistribution;
import sim.engine.SimState;
import sim.engine.Steppable;
import ec.util.MersenneTwisterFast;


/**
 * @author lizramsey
 *         approximates behaviors of households with indoor plumbing
 */

public abstract class HHParent implements Steppable {
	public static ArrayList<HHParent> houseHoldAgents = new ArrayList<>();
	public static double percentConservers = 5;
	private static String textFileInput;
	protected static final int PROBABILITY_OF_FRIENDSHIP = 100; //Probability an agent will befriend another agent.
	protected static MersenneTwisterFast rng = new MersenneTwisterFast();    //Random number generator for cycling agents during friendship assignment.

	public String simplifyingCodeTrack;

	//end uses and values
	//TODO: figure out initializing values from the literature later
	private int numDualFlushToilets;

	//personal attributes for each Household Agent
	private String neighborhood;
	private int independentLikelihoodDFInstall;
	private int friendLikelihoodDFInstall;
	private int familyLikelihoodDFInstall;
	private int phedLikelihoodDFInstall;
	private int religiousOfficialLikelihoodDFInstall;
	public int householdSize;
	public boolean isConserver;
	public boolean hasDualFlushToilets;
	protected String vertexName;
	public double monthlyDemand;
	private double personalUtility;
	private double probabilityOfConserving;
	public int maxNumFamilyMembers;
	public int maxNumCloseFriends;
	public int maxNumConnections;
	public int maxNumAcquaintances;

	protected UUID uuid;//Unique ID for this particular agent
	protected ArrayList<HHParent> acquaintances = new ArrayList<HHParent>();
	protected ArrayList<HHParent> closeFriends = new ArrayList<HHParent>();
	protected ArrayList<HHParent> respectedFamilyMembers = new ArrayList<HHParent>();//An array of IDs for agents that are in this agent's network

	protected ArrayList<HHParent> acqAlreadySpokenTo = new ArrayList<HHParent>();
	protected ArrayList<HHParent> friendsAlreadySpokenTo = new ArrayList<HHParent>();
	protected ArrayList<HHParent> famAlreadySpokenTo = new ArrayList<HHParent>();
	protected double timeStepBorn;

	protected abstract void prepareStep(SimState state);
	public abstract void setNetworkSize();
	public abstract void talkToNetwork();


	/**
	 * Default constructor
	 */
	public HHParent(){}

	/**
	 * 
	 * @param vertexNumber
	 * @param timeStep
	 */
	public HHParent(int vertexNumber, double timeStep) {
		independentLikelihoodDFInstall = ValueGenerator.getValueLikert(2.88, 1.46);//ind likelihood DFI, from survey
		friendLikelihoodDFInstall = ValueGenerator.getValueLikert(3.12, 1.43);//friend likelihood DFI, from survey
		familyLikelihoodDFInstall = ValueGenerator.getValueLikert(3.8, 1.48);//family likelihood DFI, from survey
		phedLikelihoodDFInstall = ValueGenerator.getValueLikert(3.07, 1.49);//phed likelihood DFI, from survey
		religiousOfficialLikelihoodDFInstall = ValueGenerator.getValueLikert(2.32, 1.45);//religious official likelihood DFI, from survey
		householdSize = ValueGenerator.getValueWithRange(5.1, 2.61, 1, 20);//household size, from 2011 census
		hasDualFlushToilets = true;
		isConserver = HHParent.generateConservationStatus();
		vertexName	= "vert" + vertexNumber;
		//        //TODO: remove this after testing
		        maxNumConnections = 8;
		        maxNumFamilyMembers = 4;
		        maxNumCloseFriends = 2;
//		maxNumConnections = ValueGenerator.getValueWithRange(48.0, 27.0, 11, 185);//max num connections/social network size
//		maxNumFamilyMembers = ValueGenerator.getValueWithRange(5.48, 1.8, 0, 10);//family network, from latrine study
//		maxNumCloseFriends = ValueGenerator.getValueWithRange(2.64, 2.16, 0, 10);//friend network, from latrine study

		uuid = UUID.randomUUID(); //set uuid
		timeStepBorn = timeStep;
	}

	public void step(SimState state) {
		prepareStep(state);
		for (HHParent hh: houseHoldAgents){
			System.out.println("step fxn in HHparent test, agent " + hh.vertexName);
		}
		//sets up the simstate
		//JaipurResidentialWUOriginal jaipurWaterUse = (JaipurResidentialWUOriginal) state; 

		monthlyDemand = getThisHouseholdDemand();
		//TODO: is this the right time to put in the data collector?
		DataCollector.CumulativeDemand = DataCollector.CumulativeDemand + monthlyDemand;
		DataCollector.modelPopulation = DataCollector.modelPopulation + this.householdSize;
		DataCollector.numAgents++;
		if(isConserver){
			DataCollector.numConservers++;
		}
		talkToNetwork();
		calculateUtilityandUpdateConsumption();   
	}

	public String getVertexName() {
		return vertexName;
	}

	public void setHouseholdSize(int newHouseholdSize) {
		householdSize = newHouseholdSize;
	}

	public static boolean generateConservationStatus() {
		MersenneTwisterFast rand = new MersenneTwisterFast();
		long num = rand.nextLong(100);    //includes 0.0 and 1.0 in randomly drawn number's interval
		if (num < percentConservers) {
			return true;                            //if random number is smaller than the percent of conservers, return true
		}
		return false;
	}

	//TODO: once the model is running, edit this with realistic consumption values and months
	public double getThisHouseholdDemand() {
		double demand;
		if (!isConserver) {
			demand = householdSize * 125 * 30;
		} else {
			demand = householdSize * 100 * 30;
		}
		return demand;
	}

	public static int generateK() {
		int k = 0;
		while (k == 0 || k > 100) {
			PoissonDistribution householdDist = new PoissonDistribution(9.2);    //from Kerala mobile phone study
			//for testing only
			//PoissonDistribution householdDist = new PoissonDistribution(3);
			k = householdDist.sample();
		}
		return k;
	}

	public int getNumDualFlushToilets() {
		int dfToilets = numDualFlushToilets;
		return dfToilets;
	}

	public static int generateNumDualFlushToilets() {
		int numDualFlushToilets = 0;
		while (numDualFlushToilets == 0) {
			PoissonDistribution toiletDist = new PoissonDistribution(3, 0.2);//creates poisson distribution with a mean of 3 and a convergence of 0.2
			numDualFlushToilets = toiletDist.sample();
		}
		return numDualFlushToilets;
	}

	//NEW: refer to old code below if this screws up too badly
	public double calculateRatiosForUtilityFxn(ArrayList<HHParent> thisList, ArrayList<HHParent> communicatedList, boolean calculatingConserver){ 
		int numConserversSpokenTo = 0;
		int numNonConserversSpokenTo = 0;
		int networkSize = thisList.size();
		double ratio = 0.0;
		for (HHParent hh: communicatedList){
			if(hh.isConserver){
				numConserversSpokenTo++;
			}
		}
		numNonConserversSpokenTo = networkSize - numConserversSpokenTo;

		if(calculatingConserver){
			if(numConserversSpokenTo == 0 || networkSize == 0){
				ratio = 0.0;
			}
			else{
				double numConsDouble = (double)(numConserversSpokenTo);
				ratio = numConsDouble/networkSize;
			}
			//           System.out.println("\tratio numCons: " + ratio);
		}
		else{       
			if(numNonConserversSpokenTo == 0 || networkSize == 0){
				ratio = 0.0;
			}
			else{
				double numNonConsDouble = (double)(numNonConserversSpokenTo);
				ratio = numNonConsDouble/networkSize;
			}
			//            System.out.println("\tratio numNonCons: " + ratio);
		}
		////        System.out.println(" numFriends for " + this.getVertexName() + ": " + numConnectionsThisList
		////                + ", numConservers in that list: " + numConservers + ", ratio: " + ratio);
		//        System.out.println("agent " + this.vertexName + "'s network size: " + (respectedFamilyMembers.size() + closeFriends.size() + acquaintances.size()) 
		//        		+ "\n\tnumConservers: " + numConserversSpokenTo
		//        		+ "\n\tnumNonConservers: " + numNonConserversSpokenTo);
		//        
		return ratio;      
	}

	public void talk(ArrayList<HHParent> wholeNetwork, ArrayList<HHParent> networkTalkedToAlready){
		//select random member of arrayList, then, if he doesn't already exist in the talkedTo list, add him. if he does, return and let the next agent go
		ArrayList<HHParent> shuffledNetwork = wholeNetwork;
		Collections.shuffle(shuffledNetwork);
		if(shuffledNetwork.isEmpty()){
			System.out.println("shuffledNetwork has nobody!");
			return;
		}
		HHParent randHH = shuffledNetwork.get(0);
		//go through network of Talked to and see if the first guy in the wholeNetworkShuffled's uuid exists in it. if it doesn't, add him to the talkedto network
		for (HHParent hh : networkTalkedToAlready){
			//System.out.println(hh.uuid);
			if(hh.uuid == randHH.uuid){
				//	System.out.println(hh.uuid + " was found in network, exiting");
				return;
			}
		}
		networkTalkedToAlready.add(randHH);
	}

	public void calculateUtilityandUpdateConsumption(){
		int fullNetworkSize = acquaintances.size() + respectedFamilyMembers.size() + closeFriends.size();
		double RatioFamConservers = calculateRatiosForUtilityFxn(respectedFamilyMembers, famAlreadySpokenTo, true);
		double RatioFamNonConservers = calculateRatiosForUtilityFxn(respectedFamilyMembers, famAlreadySpokenTo, false);
		double RatioFriendConservers = calculateRatiosForUtilityFxn(closeFriends, friendsAlreadySpokenTo, true);
		double RatioFriendNonConservers = calculateRatiosForUtilityFxn(closeFriends, friendsAlreadySpokenTo, false);
		double RatioAcqConservers = calculateRatiosForUtilityFxn(acquaintances, acqAlreadySpokenTo, true);
		double RatioAcqNonConservers = calculateRatiosForUtilityFxn(acquaintances, acqAlreadySpokenTo, false);
		//sensitivity analysis for a and b w/o my delta introduction
		int famDelta = calculateDelta(this.independentLikelihoodDFInstall, this.familyLikelihoodDFInstall);
		int friendDelta = calculateDelta(this.independentLikelihoodDFInstall, this.friendLikelihoodDFInstall);
		if(isConserver){
			double randNum = rng.nextDouble(true, true);
//			System.out.println("conservers:");
//			System.out.println("fam ratio: " + RatioFamConservers + " friend ratio: " + RatioFriendConservers + "acq ratio: " + RatioAcqConservers);
			double utilStay = UtilityFunctionOriginal.calculateUtilityForConserverStayingConserver(RatioFamConservers,
					famDelta, RatioFriendConservers, friendDelta, RatioAcqConservers);
			double utilChange = UtilityFunctionOriginal.calculateUtilityForConserverBecomingNonConserver(RatioFamNonConservers,
					famDelta, RatioFriendNonConservers, friendDelta, RatioAcqNonConservers);	
			double probabilityConsToCons = ProbabilityOfBehaviorOriginal.probabilityConsToCons(utilStay, utilChange);
			double probabilityConsToNonCons = ProbabilityOfBehaviorOriginal.probabilityConsToNonCons(utilStay, utilChange);
			if (randNum > probabilityConsToCons){
				isConserver = false;
			}
		} else {
			double randNum = rng.nextDouble(true, true);
			double utilStay = UtilityFunctionOriginal.calculateUtilityForNonConserverStayingNonConserver(RatioFamNonConservers,
					famDelta, RatioFriendNonConservers, friendDelta, RatioAcqNonConservers);
			double utilChange = UtilityFunctionOriginal.calculateUtilityForNonConserverBecomingConserver(RatioFamConservers,
					famDelta, RatioFriendConservers, friendDelta, RatioAcqConservers);
			double probabilityNonConsToNonCons = ProbabilityOfBehaviorOriginal.probabilityNonConsToNonCons(utilChange, utilStay);
			if (randNum > probabilityNonConsToNonCons){
				isConserver = true;
			}
		}
	}

	public int calculateDelta(int originalNum, int newNum){
		int delta = newNum - originalNum;
		if (delta < 0){
			delta = 0;
		}
		return delta;
	}
}
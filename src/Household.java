import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.commons.collections15.Factory;
import org.apache.commons.math3.distribution.PoissonDistribution;

import sim.engine.SimState;
import sim.engine.Steppable;
import ec.util.MersenneTwisterFast;


/**
 * @author lizramsey
 *         approximates behaviors of households with indoor plumbing
 */

public class Household implements Steppable {
	public static ArrayList<Household> houseHoldAgents = new ArrayList<>();
	public static double percentConservers = 5;
	private static String textFileInput;
	protected static final int PROBABILITY_OF_FRIENDSHIP = 100; //Probability an agent will befriend another agent.
	protected static MersenneTwisterFast rng = new MersenneTwisterFast();    //Random number generator for cycling agents during friendship assignment.


	//end uses and values
	//TODO: figure out initializing values from the literature later
	private int numDualFlushToilets;

	//personal attributes for each Household Agent
	private String neighborhood;
	private int independentLikelihoodDFInstall;
	private int friendLikelihoodDFInstall;
	protected int familyLikelihoodDFInstall;
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
	protected ArrayList<Household> acquaintances = new ArrayList<Household>();
	protected ArrayList<Household> closeFriends = new ArrayList<Household>();
	protected ArrayList<Household> respectedFamilyMembers = new ArrayList<Household>();//An array of IDs for agents that are in this agent's network

	protected ArrayList<Household> acqAlreadySpokenTo = new ArrayList<Household>();
	protected ArrayList<Household> friendsAlreadySpokenTo = new ArrayList<Household>();
	protected ArrayList<Household> famAlreadySpokenTo = new ArrayList<Household>();
	protected double timeStepBorn;

	/**
	 * Default constructor
	 */
	public Household(){}

	/**
	 * 
	 * @param vertexNumber
	 * @param timeStep
	 */
	public Household(int vertexNumber, double timeStep) {
		independentLikelihoodDFInstall = ValueGenerator.getValueLikert(2.88, 1.46);//ind likelihood DFI, from survey
		friendLikelihoodDFInstall = ValueGenerator.getValueLikert(3.12, 1.43);//friend likelihood DFI, from survey
		familyLikelihoodDFInstall = ValueGenerator.getValueLikert(3.8, 1.48);//family likelihood DFI, from survey
		phedLikelihoodDFInstall = ValueGenerator.getValueLikert(3.07, 1.49);//phed likelihood DFI, from survey
		religiousOfficialLikelihoodDFInstall = ValueGenerator.getValueLikert(2.32, 1.45);//religious official likelihood DFI, from survey
		householdSize = ValueGenerator.getValueWithRange(5.1, 2.61, 1, 20);//household size, from 2011 census
		hasDualFlushToilets = true;
		isConserver = Household.generateConservationStatus();
		vertexName	= "vert" + vertexNumber;
		//TODO: remove this after testing
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
		String graphStructure = JaipurABM.getGraphStructure();
		if(graphStructure.equals("original")){
			talkToThreeNetworks();
		}
		else{
			talk(acquaintances, acqAlreadySpokenTo);
		}
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

	public double calculateRatiosForUtilityFxn(ArrayList<Household> thisList, ArrayList<Household> communicatedList, boolean calculatingConserver){ 
		int numConserversSpokenTo = 0;
		int numNonConserversSpokenTo = 0;
		int networkSize = thisList.size();
		double ratio = 0.0;
		for (Household hh: communicatedList){
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

	public void talk(ArrayList<Household> wholeNetwork, ArrayList<Household> networkTalkedToAlready){
		//select random member of arrayList, then, if he doesn't already exist in the talkedTo list, add him. if he does, return and let the next agent go
		ArrayList<Household> shuffledNetwork = wholeNetwork;
		Collections.shuffle(shuffledNetwork);
		if(shuffledNetwork.isEmpty()){
			System.out.println("shuffledNetwork has nobody!");
			return;
		}
		Household randHH = shuffledNetwork.get(0);
		//go through network of Talked to and see if the first guy in the wholeNetworkShuffled's uuid exists in it. if it doesn't, add him to the talkedto network
		for (Household hh : networkTalkedToAlready){
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

	public void setNetworkSize(){
		//check to see if our total network size is larger than sum of our friends and family numbers
		//if it is, then we set it to the sum of our networks, and that agent just has no acquaintances
		int sumFriendsNFam = maxNumCloseFriends + maxNumFamilyMembers;
		if (sumFriendsNFam > maxNumConnections){
			maxNumConnections = sumFriendsNFam;
		}   	
		maxNumAcquaintances = maxNumConnections - sumFriendsNFam;
		if (maxNumAcquaintances < 0){
			System.out.println("ERROR in setNetworkSize method");
		}
	}

	public void assignAcquaintancesToAgentAtTimeStep(double timeStep) {
		if (acquaintances.size() == maxNumAcquaintances) {
			return;
		}
		List<Household> houseHoldAgentsShuffled = new ArrayList<Household>(houseHoldAgents);
		// Shuffle this list (friends assigned to random agents)
		Collections.shuffle(houseHoldAgentsShuffled);
		for (Household hhFriend : houseHoldAgentsShuffled) {
			// Do not match if friend does not yet exist in this time step
			if (hhFriend.timeStepBorn > timeStep){
				continue;
			}
			// Check if agent has reached maximum amount of friends - exit function
			if (acquaintances.size() == maxNumAcquaintances) {
				return;
			}
			// Do not match agent to itself - loop to next agent
			// Do not match agent to a friend that has max amount of friends already - loop to next agent
			if (this.uuid == hhFriend.uuid || hhFriend.acquaintances.size() == hhFriend.maxNumAcquaintances) {
				continue;
			}
			//If agent is already a friend or family member, do not add to network again - loop to next agent
			if (doesAcquaintanceshipExist(hhFriend.uuid) == true || doesFamilyRelationshipExist(hhFriend.uuid) == true) {
				continue;
			}
			if (doesCloseFriendshipExist(hhFriend.uuid) == true){
				continue;
			}
			// Get random integer between 1 and 100. If less than or equal to friendship probability, add each friend to each other's friends arraylist
			if (rng.nextInt(100) <= PROBABILITY_OF_FRIENDSHIP) {
				acquaintances.add(hhFriend);
				hhFriend.acquaintances.add(this);
			}
		}
	}

	protected void prepareStep(SimState state){
		setNetworkSize();
//		System.out.println("agent " + this.vertexName + " maxNumConnections: " + this.maxNumConnections
//				+ " maxNumAcq: " + this.maxNumAcquaintances + " maxNumFam: " + this.maxNumFamilyMembers
//				+ " maxNumFriends: " + this.maxNumCloseFriends);
		String graphStructure = JaipurABM.getGraphStructure();
		if (graphStructure.equals("original")){
			assignFamilyToAgentAtTimeStep(state.schedule.getTime());
			assignCloseFriendsAtTimeStep(state.schedule.getTime());
			assignAcquaintancesToAgentAtTimeStep(state.schedule.getTime());
			System.out.println("acquaintances for " + this.vertexName + ":");
			for (Household hh: acquaintances){
				System.out.print(hh.vertexName + " ");
			}
			System.out.println();
			System.out.println("friends for " + this.vertexName + ":");
			for (Household hh: closeFriends){
				System.out.print(hh.vertexName + " ");
			}
			System.out.println();
			System.out.println("family for " + this.vertexName + ":");
			for (Household hh: respectedFamilyMembers){
				System.out.print(hh.vertexName + " ");
			}
		}
	}

	public void assignFamilyToAgentAtTimeStep(double timeStep){
		if (respectedFamilyMembers.size() == maxNumFamilyMembers) {
			return;
		}
		List<Household> houseHoldAgentsShuffled = new ArrayList<Household>(houseHoldAgents);
		// Shuffle this list (friends assigned to random agents)
		Collections.shuffle(houseHoldAgentsShuffled);
		for (Household hhFam : houseHoldAgentsShuffled) {
			// Do not match if agent does not yet exist in this time step
			if (hhFam.timeStepBorn > timeStep){
				continue;
			}
			if (respectedFamilyMembers.size() == maxNumFamilyMembers) {
				return;
			}
			// Do not match agent to itself - loop to next agent
			// Do not match agent to a friend that has max amount of acquaintances already - loop to next agent
			if (this.uuid == hhFam.uuid || hhFam.acquaintances.size() == hhFam.maxNumAcquaintances) {
				continue;
			}
			//If agent is already an acquaintance or family member, do not add to network again - loop to next agent
			if (doesAcquaintanceshipExist(hhFam.uuid) == true || doesFamilyRelationshipExist(hhFam.uuid) == true) {
				continue;
			}
			//If agent already is a close friend, loop to next agent
			if (doesCloseFriendshipExist(hhFam.uuid) == true){
				continue;
			}

			// Get random integer between 1 and 100. If less than or equal to friendship probability, add each friend to each other's friends arraylist
			if (rng.nextInt(100) <= PROBABILITY_OF_FRIENDSHIP) {
				respectedFamilyMembers.add(hhFam);
				hhFam.acquaintances.add(this);
			}
		}
	}

	public void assignCloseFriendsAtTimeStep(double timeStep){
		if (closeFriends.size() == maxNumCloseFriends) {
			return;
		}
		List<Household> houseHoldAgentsShuffled = new ArrayList<Household>(houseHoldAgents);
		// Shuffle this list (friends assigned to random agents)
		Collections.shuffle(houseHoldAgentsShuffled);
		for (Household hhFriend : houseHoldAgentsShuffled) {
			// Do not match if agent does not yet exist in this time step
			if (hhFriend.timeStepBorn > timeStep){
				continue;
			}
			if (closeFriends.size() == maxNumCloseFriends) {
				return;
			}
			// Do not match agent to itself - loop to next agent
			// Do not match agent to a friend that has max amount of fam members already - loop to next agent
			if (this.uuid == hhFriend.uuid || hhFriend.respectedFamilyMembers.size() == hhFriend.maxNumFamilyMembers) {
				continue;
			}
			//If agent is already an acquaintance or family member, do not add to network again - loop to next agent
			if (doesAcquaintanceshipExist(hhFriend.uuid) == true || doesFamilyRelationshipExist(hhFriend.uuid) == true) {
				continue;
			}
			if (doesCloseFriendshipExist(hhFriend.uuid) == true){
				continue;
			}
			// Get random integer between 1 and 100. If less than or equal to friendship probability, add each friend to each other's friends arraylist
			if (rng.nextInt(100) <= PROBABILITY_OF_FRIENDSHIP) {
				closeFriends.add(hhFriend);
				hhFriend.closeFriends.add(this);
			}
		}
	}

	protected boolean doesAcquaintanceshipExist(UUID friendUUID) {
		for (Household currentFriend : acquaintances) {
			if (currentFriend.uuid == friendUUID) {
				return true;
			}
		}
		return false;
	}

	protected boolean doesCloseFriendshipExist(UUID friendUUID) {
		for (Household currentFriend : closeFriends) {
			if (currentFriend.uuid == friendUUID) {
				return true;
			}
		}
		return false;
	}

	protected boolean doesFamilyRelationshipExist(UUID friendUUID) {
		for (Household currentFriend : respectedFamilyMembers) {
			if (currentFriend.uuid == friendUUID) {
				return true;
			}
		}
		return false;
	}

	public void talkToThreeNetworks(){
		int num = rng.nextInt(3) + 1;
		//if num is 1 and fam is empty or if num is 2 and friends are empty or num is 3 and acq is empty, pick a new num
		while (num == 1 && respectedFamilyMembers.isEmpty() || num == 2 && closeFriends.isEmpty() || num == 3 && acquaintances.isEmpty()){
			num = rng.nextInt(3) + 1;
		}

		//select one network to communicate with for this timestep
		if (num == 1){
			//communicate with family
			//    		System.out.println("fam spoken to before talk for " + this.vertexName + ": ");
			//    		for (HHwPlumbing2 hh: famAlreadySpokenTo){
			//    			System.out.print(hh.vertexName + " ");
			//    		}
			//    		System.out.println();
			talk(respectedFamilyMembers, famAlreadySpokenTo);
			//    		System.out.println("fam spoken to after talk for " + this.vertexName + ": ");
			//    		for (HHwPlumbing2 hh: famAlreadySpokenTo){
			//    			System.out.print(hh.vertexName + " ");
			//    		}
			//    		System.out.println();
		}
		else if (num == 2){
			//communicate with friends
			//    		System.out.println("friends spoken to before talk for " + this.vertexName + ": ");
			//    		for (HHwPlumbing2 hh: friendsAlreadySpokenTo){
			//    			System.out.print(hh.vertexName + " ");
			//    		}
			talk(closeFriends, friendsAlreadySpokenTo);
			//    		System.out.println("friends spoken to after talk for " + this.vertexName + ": ");
			//    		for (HHwPlumbing2 hh: friendsAlreadySpokenTo){
			//    			System.out.print(hh.vertexName + " ");
			//    		}
		}
		else if (num == 3){
			//    		//communicate with acq
			//    		System.out.println("acq spoken to before talk for " + this.vertexName + ": ");
			//    		for (HHwPlumbing2 hh: acqAlreadySpokenTo){
			//    			System.out.print(hh.vertexName + " ");
			//    		}
			talk(acquaintances, acqAlreadySpokenTo);
			//    		System.out.println("acq spoken to after talk for " + this.vertexName + ": ");
			//    		for (HHwPlumbing2 hh: acqAlreadySpokenTo){
			//    			System.out.print(hh.vertexName + " ");
			//    		}
		}
		else{
			System.out.println("error in rng for talk method");
			return;
		}
	}
	
	public UUID getUUID(){	
		return this.uuid;
	}


}


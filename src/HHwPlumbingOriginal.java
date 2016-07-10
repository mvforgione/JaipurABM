//package com.company;

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
 * @author lizramsey
 *         approximates behaviors of households with indoor plumbing
 */

public class HHwPlumbingOriginal implements Steppable {
	public String simplifyingCode;
    public static double percentConservers = 5;
    private static String textFileInput;
    
    //end uses and values
    //TODO: figure out initializing values from the literature later
    private int numDualFlushToilets;
	
    public static java.util.ArrayList<HHwPlumbingOriginal> houseHoldAgents = new java.util.ArrayList<>();
    private static int PROBABILITY_OF_FRIENDSHIP = 100; //Probability an agent will befriend another agent.
    private static MersenneTwisterFast rng = new MersenneTwisterFast();    //Random number generator for cycling agents during friendship assignment.
    //TODO: add in a calculate days in month function for when testing is over
    private int numDaysInMonth;


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
    private String vertexName;
    public double monthlyDemand;
    private double personalUtility;
    private double probabilityOfConserving;
    public int maxNumFamilyMembers;
    public int maxNumCloseFriends;
    public int maxNumConnections;
    public int maxNumAcquaintances;


    UUID uuid;                                                                   //Unique ID for this particular agent
    java.util.ArrayList<HHwPlumbingOriginal> acquaintances = new java.util.ArrayList<>();
    java.util.ArrayList<HHwPlumbingOriginal> closeFriends = new java.util.ArrayList<>();
    java.util.ArrayList<HHwPlumbingOriginal> respectedFamilyMembers = new java.util.ArrayList<>();		//An array of IDs for agents that are in this agent's network
    
    java.util.ArrayList<HHwPlumbingOriginal> acqAlreadySpokenTo = new java.util.ArrayList<>();
    java.util.ArrayList<HHwPlumbingOriginal> friendsAlreadySpokenTo = new java.util.ArrayList<>();
    java.util.ArrayList<HHwPlumbingOriginal> famAlreadySpokenTo = new java.util.ArrayList<>();
    private double timeStepBorn;

    public HHwPlumbingOriginal(){}
    
    public HHwPlumbingOriginal(int vertexNumber, double timeStep) {

        independentLikelihoodDFInstall = ValueGenerator.getValueLikert(2.88, 1.46);//ind likelihood DFI, from survey
        friendLikelihoodDFInstall = ValueGenerator.getValueLikert(3.12, 1.43);//friend likelihood DFI, from survey
        familyLikelihoodDFInstall = ValueGenerator.getValueLikert(3.8, 1.48);//family likelihood DFI, from survey
        phedLikelihoodDFInstall = ValueGenerator.getValueLikert(3.07, 1.49);//phed likelihood DFI, from survey
        religiousOfficialLikelihoodDFInstall = ValueGenerator.getValueLikert(2.32, 1.45);//religious official likelihood DFI, from survey
        householdSize = ValueGenerator.getValueWithRange(5.1, 2.61, 1, 20);//household size, from 2011 census
        hasDualFlushToilets = true;
        isConserver = HHwPlumbingOriginal.generateConservationStatus();
        vertexName	= "vert" + vertexNumber;
//        //TODO: remove this after testing
        maxNumConnections = 10;
        maxNumFamilyMembers = 4;
        maxNumCloseFriends = 2;
        maxNumAcquaintances = 10;
//        maxNumConnections = ValueGenerator.getValueWithRange(48.0, 27.0, 11, 185);//max num connections/social network size
//        maxNumFamilyMembers = ValueGenerator.getValueWithRange(5.48, 1.8, 0, 10);//family network, from latrine study
//        maxNumCloseFriends = ValueGenerator.getValueWithRange(2.64, 2.16, 0, 10);//friend network, from latrine study


        uuid = UUID.randomUUID();                //set uuid
        timeStepBorn = timeStep;
    }


    public void step(SimState state) {
    	setNetworkSize();
        assignFamilyToAgentAtTimeStep(state.schedule.getTime());
        assignCloseFriendsAtTimeStep(state.schedule.getTime());
        assignAcquaintancesToAgentAtTimeStep(state.schedule.getTime());
        //sets up the simstate
        JaipurResidentialWUOriginal jaipurWaterUse = (JaipurResidentialWUOriginal) state; 
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
    	//System.out.println("max num acquaintances: " + maxNumAcquaintances);
        if (acquaintances.size() == maxNumAcquaintances) {
            return;
        }
        List<HHwPlumbingOriginal> houseHoldAgentsShuffled = new ArrayList<HHwPlumbingOriginal>(houseHoldAgents);
        // Shuffle this list (friends assigned to random agents)
        Collections.shuffle(houseHoldAgentsShuffled);
        for (HHwPlumbingOriginal hhFriend : houseHoldAgentsShuffled) {
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

    public void assignFamilyToAgentAtTimeStep(double timeStep){
        if (respectedFamilyMembers.size() == maxNumFamilyMembers) {
            return;
        }
        List<HHwPlumbingOriginal> houseHoldAgentsShuffled = new ArrayList<HHwPlumbingOriginal>(houseHoldAgents);
        // Shuffle this list (friends assigned to random agents)
        Collections.shuffle(houseHoldAgentsShuffled);
        for (HHwPlumbingOriginal hhFam : houseHoldAgentsShuffled) {
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
//                System.out.println("hits third if loop; hhFam.uuid = " + hhFam.uuid + " acq. size = " + hhFam.acquaintances.size() + 
//                		" maxNumAcq = " + hhFam.maxNumAcquaintances);
            	continue;
            }
//            System.out.println("why quit here?");
            //If agent is already an acquaintance or family member, do not add to network again - loop to next agent
            if (doesAcquaintanceshipExist(hhFam.uuid) == true || doesFamilyRelationshipExist(hhFam.uuid) == true) {
            	//System.out.println("acq already: " + doesAcquaintanceshipExist(hhFam.uuid) + " fam already: " + doesFamilyRelationshipExist(hhFam.uuid));
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
        List<HHwPlumbingOriginal> houseHoldAgentsShuffled = new ArrayList<HHwPlumbingOriginal>(houseHoldAgents);
        // Shuffle this list (friends assigned to random agents)
        Collections.shuffle(houseHoldAgentsShuffled);
        for (HHwPlumbingOriginal hhFriend : houseHoldAgentsShuffled) {
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
                hhFriend.respectedFamilyMembers.add(this);
            }
        }
    }


    private boolean doesAcquaintanceshipExist(UUID friendUUID) {
        for (HHwPlumbingOriginal currentFriend : acquaintances) {
            if (currentFriend.uuid == friendUUID) {
                return true;
            }
        }
        return false;
    }

    private boolean doesCloseFriendshipExist(UUID friendUUID) {
        for (HHwPlumbingOriginal currentFriend : closeFriends) {
            if (currentFriend.uuid == friendUUID) {
                return true;
            }
        }
        return false;
    }

    private boolean doesFamilyRelationshipExist(UUID friendUUID) {
        for (HHwPlumbingOriginal currentFriend : respectedFamilyMembers) {
            if (currentFriend.uuid == friendUUID) {
                return true;
            }
        }
        return false;
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
    public double calculateRatiosForUtilityFxn(ArrayList<HHwPlumbingOriginal> thisList, ArrayList<HHwPlumbingOriginal> communicatedList, boolean calculatingConserver){ 
    	int numConserversSpokenTo = 0;
    	int numNonConserversSpokenTo = 0;
    	int networkSize = thisList.size();
        double ratio = 0.0;
        for (HHwPlumbingOriginal hh: communicatedList){
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
//    //TODO: updated to make agents consider others they haven't spoken to as nonconservers
    //SAVED so we don't screw things up too badly with no backup
//    public double calculateRatiosForUtilityFxn(ArrayList<HHwPlumbing2> thisList, ArrayList<HHwPlumbing2> communicatedList, boolean calculatingConserver){ 
//    	//NEW: edited out to test new idea
//    	//int numNonConserversSpokenTo = 0;
//    	
//    	int numConserversSpokenTo = 0;
//    	int networkSize = thisList.size();
//        double ratio = 0.0;
//        for (HHwPlumbing2 hh: communicatedList){
//            if(hh.isConserver){
//                numConserversSpokenTo++;
//            }
//            //NEW: edited out to test new idea
////            else{
////            	numNonConserversSpokenTo++;
////            }
//        }
//        //NEW: added to test new idea
//        int numNonConserversSpokenTo = respectedFamilyMembers.size() + closeFriends.size() + acquaintances.size() - numConserversSpokenTo;
//
//        if(calculatingConserver){
//           if(numConserversSpokenTo == 0 || networkSize == 0){
//                	ratio = 0.0;
//           }
//           else{
//        	   double numConsDouble = (double)(numConserversSpokenTo);
//                ratio = numConsDouble/networkSize;
//           }
////           System.out.println("\tratio numCons: " + ratio);
//        }
//        else{       
//        	if(numNonConserversSpokenTo == 0 || networkSize == 0){
//        		ratio = 0.0;
//        	}
//        	else{
//        		double numNonConsDouble = (double)(numNonConserversSpokenTo);
//        		ratio = numNonConsDouble/networkSize;
//        	}
////            System.out.println("\tratio numNonCons: " + ratio);
//        }
//////        System.out.println(" numFriends for " + this.getVertexName() + ": " + numConnectionsThisList
//////                + ", numConservers in that list: " + numConservers + ", ratio: " + ratio);
////        System.out.println("agent " + this.vertexName + "'s network size: " + (respectedFamilyMembers.size() + closeFriends.size() + acquaintances.size()) 
////        		+ "\n\tnumConservers: " + numConserversSpokenTo
////        		+ "\n\tnumNonConservers: " + numNonConserversSpokenTo);
////        
//       return ratio;      
//    }
    
    public void talkToNetwork(){
    	int num = rng.nextInt(3) + 1;
//    	System.out.println("rng: " + num);
//    	System.out.println("fam members empty: " + respectedFamilyMembers.isEmpty());
//    	System.out.println("friends empty:" + closeFriends.isEmpty());
//    	System.out.println("acquaintances empty: " + acquaintances.isEmpty());
    	//if num is 1 and fam is empty or if num is 2 and friends are empty or num is 3 and acq is empty, pick a new num
    	while (num == 1 && respectedFamilyMembers.isEmpty() || num == 2 && closeFriends.isEmpty() || num == 3 && acquaintances.isEmpty()){
    		num = rng.nextInt(3) + 1;
    	}
    	
    	//select one network to communicate with for this timestep
    	if (num == 1){
    		//communicate with family
//    		System.out.println("fam spoken to before talk for " + this.vertexName + ": ");
//    		for (HHwPlumbingOriginal hh: famAlreadySpokenTo){
//    			System.out.println(hh.vertexName + " ");
//    		}
//    		System.out.println();
    		talk(respectedFamilyMembers, famAlreadySpokenTo);
//    		System.out.println();
    	}
    	else if (num == 2){
    		//communicate with friends
//    		System.out.println("friends spoken to before talk for " + this.vertexName + ": ");
//    		for (HHwPlumbingOriginal hh: friendsAlreadySpokenTo){
//    			System.out.println(hh.vertexName + " ");
//    		}
    		talk(closeFriends, friendsAlreadySpokenTo);
//    		System.out.println("friends spoken to after talk for " + this.vertexName + ": ");
//    		for (HHwPlumbingOriginal hh: friendsAlreadySpokenTo){
//    			System.out.println(hh.vertexName + " ");
//    		}
    	}
    	else if (num == 3){
    		//communicate with acq
//    		System.out.println("acq spoken to before talk for " + this.vertexName + ": ");
//    		for (HHwPlumbingOriginal hh: acqAlreadySpokenTo){
//    			System.out.print(hh.vertexName + " ");
//    		}
    		talk(acquaintances, acqAlreadySpokenTo);
//    		System.out.println("acq spoken to after talk for " + this.vertexName + ": ");
//    		for (HHwPlumbingOriginal hh: acqAlreadySpokenTo){
//    			System.out.print(hh.vertexName + " ");
//    		}
        }
    	else{
    		System.out.println("error in rng for talk method");
    		return;
    	}
    }
    
    public void talk(ArrayList<HHwPlumbingOriginal> wholeNetwork, ArrayList<HHwPlumbingOriginal> networkTalkedToAlready){
    	//System.out.println("talk function entered");
    	//select random member of arrayList, then, if he doesn't already exist in the talkedTo list, add him. if he does, return and let the next agent go
    	ArrayList<HHwPlumbingOriginal> shuffledNetwork = wholeNetwork;
    	Collections.shuffle(shuffledNetwork);
    	if(shuffledNetwork.isEmpty()){
    		System.out.println("shuffledNetwork has nobody!");
    		return;
    	}
    	HHwPlumbingOriginal randHH = shuffledNetwork.get(0);
    	//go through network of Talked to and see if the first guy in the wholeNetworkShuffled's uuid exists in it. if it doesn't, add him to the talkedto network
    	for (HHwPlumbingOriginal hh : networkTalkedToAlready){
    		//System.out.println(hh.uuid);
    		if(hh.uuid == randHH.uuid){
    		//	System.out.println(hh.uuid + " was found in network, exiting");
    			return;
    		}
    	}
    	networkTalkedToAlready.add(randHH);
    }
    
    //bracketed out to test new utility function--this is the initial code
//    public void calculateUtilityandUpdateConsumption(){
//        double RatioFamConservers = calculateRatiosForUtilityFxn(respectedFamilyMembers, famAlreadySpokenTo, true);
//        double RatioFamNonConservers = calculateRatiosForUtilityFxn(respectedFamilyMembers, famAlreadySpokenTo, false);
//        double RatioFriendConservers = calculateRatiosForUtilityFxn(closeFriends, friendsAlreadySpokenTo, true);
//        double RatioFriendNonConservers = calculateRatiosForUtilityFxn(closeFriends, friendsAlreadySpokenTo, false);
//        double RatioAcqConservers = calculateRatiosForUtilityFxn(acquaintances, acqAlreadySpokenTo, true);
//        double RatioAcqNonConservers = calculateRatiosForUtilityFxn(acquaintances, acqAlreadySpokenTo, false);
//        //sensitivity analysis for a and b w/o my delta introduction
//        int famDelta = 0;
//        int friendDelta = 0;
////        int famDelta = calculateDelta(this.independentLikelihoodDFInstall, this.familyLikelihoodDFInstall);
////        int friendDelta = calculateDelta(this.independentLikelihoodDFInstall, this.friendLikelihoodDFInstall);
//        if(isConserver){
//    	   double randNum = rng.nextDouble(true, true);
//    	   double utilStay = UtilityFunction.calculateUtilityForConserverStayingConserver(RatioFamConservers,
//							  famDelta, RatioFriendConservers, friendDelta, RatioAcqConservers);
//    	   double utilChange = UtilityFunction.calculateUtilityForConserverBecomingNonConserver(RatioFamNonConservers,
//							  famDelta, RatioFriendNonConservers, friendDelta, RatioAcqNonConservers);	
//    	   double probabilityConsToCons = ProbabilityOfBehavior.probabilityConsToCons(utilStay, utilChange);
//			//double probabilityConsToNonCons = ProbabilityOfBehavior.probabilityConsToNonCons(utilStay, utilChange);
//    	   if (randNum > probabilityConsToCons){
//				isConserver = false;
//    	   }
//        }else{
//        	double randNum = rng.nextDouble(true, true);
//			double utilStay = UtilityFunction.calculateUtilityForNonConserverStayingNonConserver(RatioFamNonConservers,
//					  famDelta, RatioFriendNonConservers, friendDelta, RatioAcqNonConservers);
//			double utilChange = UtilityFunction.calculateUtilityForNonConserverBecomingConserver(RatioFamConservers,
//					  famDelta, RatioFriendConservers, friendDelta, RatioAcqConservers);
//			double probabilityNonConsToNonCons = ProbabilityOfBehavior.probabilityNonConsToNonCons(utilChange, utilStay);
//			if (randNum > probabilityNonConsToNonCons){
//				isConserver = true;
//			}
//        }
//    }
    //new code, trying to fix utility function nonsense
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
    	  // System.out.println("conservers:");
    	  // System.out.println("fam ratio: " + RatioFamConservers + " friend ratio: " + RatioFriendConservers + "acq ratio: " + RatioAcqConservers);
    	   double utilStay = UtilityFunctionOriginal.calculateUtilityForConserverStayingConserver(RatioFamConservers,
							  famDelta, RatioFriendConservers, friendDelta, RatioAcqConservers);
    	   double utilChange = UtilityFunctionOriginal.calculateUtilityForConserverBecomingNonConserver(RatioFamNonConservers,
							  famDelta, RatioFriendNonConservers, friendDelta, RatioAcqNonConservers);	
    	   double probabilityConsToCons = ProbabilityOfBehaviorOriginal.probabilityConsToCons(utilStay, utilChange);
    	   double probabilityConsToNonCons = ProbabilityOfBehaviorOriginal.probabilityConsToNonCons(utilStay, utilChange);
    	   if (randNum > probabilityConsToCons){
				isConserver = false;
    	   }
        }else{
        	double randNum = rng.nextDouble(true, true);
//        	double utilStay = 1;
//        	double utilChange = 0.5;
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


//waterSourcePrimary = (String) propertyArray[9];
//waterSourcePrimaryPercentage = (Double) propertyArray[10];
//waterSourceSecondary = (String) propertyArray[11];
//waterSourceSecondaryPercentage = (Double) propertyArray[12];

//emptiesTankDaily = (Boolean) propertyArray[15];
//municipalAccessDuration = (Double) propertyArray[16];
//hasRunningWater = (Boolean) propertyArray[17];	
//believesDroughUnpreventable = (Integer) propertyArray[18];
//believesGovtResponsibleForRelief = (Integer) propertyArray[19];
//believesEveryoneResponsibleForWater = (Integer) propertyArray[20]; // scale 1-5
//talksAboutWater = (Integer) propertyArray[21];
//hasROFilter = (Boolean) propertyArray[22];
//hasLimitedWatering = (Boolean) propertyArray[23];
//willLimitWateringIndependently = (Boolean) propertyArray[24];
//hasRainwaterHarvesting = (Boolean) propertyArray[25];
//yearRWHSInstalled = (Integer) propertyArray[26];
//willInstallRWHSIndependently = (Integer) propertyArray[27];
//numCoolers = (Integer) propertyArray[28];
//coolerCapacity = (Double) propertyArray[29];
//numRefills  = (Integer) propertyArray[30];
//numAdditionalCoolers  = (Integer) propertyArray[31];
//numReplacedCoolers = (Integer) propertyArray[32];
//turnsOffFaucet = (Boolean) propertyArray[33];
//willTurnOffFaucetIndependently = (Integer) propertyArray[34];
//numPourFlushToilets = (Integer) propertyArray[35];
//numRegFlushToilets = (Integer) propertyArray[36];
//numDualFlushToilets = (Integer) propertyArray[37];
//yearDFToiletInstalled = (Integer) propertyArray[38];
//willInstallDFToiletIndependently = (Integer) propertyArray[39];
//TwentyFivePercentComplianceRate = (Double) propertyArray[40];
//FiftyPercentComplianceRate = (Double) propertyArray[41];
//preferredBath = (String) propertyArray[42];
//
//hasGarden = (Boolean) propertyArray[43];
//gardenSize = (Double) propertyArray[44];
//gardenWateringFrequencyHotSeason = (Integer) propertyArray[45];
//gardenWateringFrequencyColdSeason = (Integer) propertyArray[46];
//gardenWateringFrequencyWetSeason = (Integer) propertyArray[47];
//gardenWateringMethod = (String) propertyArray[48];
//hasYard = (Boolean) propertyArray[49];
//yardSize = (Double) propertyArray[50];
//yardWateringFrequencyHotSeason = (Integer) propertyArray[51];
//yardWateringFrequencyColdSeason = (Integer) propertyArray[52];
//yardWateringFrequencyWetSeason = (Integer) propertyArray[53];
//yardWateringMethod = (String) propertyArray[54];
//ownsHome = (Boolean) propertyArray[55];
//
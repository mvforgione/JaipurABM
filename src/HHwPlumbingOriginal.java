import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import sim.engine.SimState;


public class HHwPlumbingOriginal extends HHParent {

	public HHwPlumbingOriginal(){
		super();
	}

	public HHwPlumbingOriginal(int vertexNumber, double timeStep) {
		super(vertexNumber, timeStep);
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
		List<HHParent> houseHoldAgentsShuffled = new ArrayList<HHParent>(houseHoldAgents);
		// Shuffle this list (friends assigned to random agents)
		Collections.shuffle(houseHoldAgentsShuffled);
		for (HHParent hhFriend : houseHoldAgentsShuffled) {
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

		assignFamilyToAgentAtTimeStep(state.schedule.getTime());
		assignCloseFriendsAtTimeStep(state.schedule.getTime());
		assignAcquaintancesToAgentAtTimeStep(state.schedule.getTime());
	}

	/*public void step(SimState state) {
		super.step(state);
	}*/

	public void assignFamilyToAgentAtTimeStep(double timeStep){
		if (respectedFamilyMembers.size() == maxNumFamilyMembers) {
			return;
		}
		List<HHParent> houseHoldAgentsShuffled = new ArrayList<HHParent>(houseHoldAgents);
		// Shuffle this list (friends assigned to random agents)
		Collections.shuffle(houseHoldAgentsShuffled);
		for (HHParent hhFam : houseHoldAgentsShuffled) {
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
		List<HHParent> houseHoldAgentsShuffled = new ArrayList<HHParent>(houseHoldAgents);
		// Shuffle this list (friends assigned to random agents)
		Collections.shuffle(houseHoldAgentsShuffled);
		for (HHParent hhFriend : houseHoldAgentsShuffled) {
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

	protected boolean doesAcquaintanceshipExist(UUID friendUUID) {
		for (HHParent currentFriend : acquaintances) {
			if (currentFriend.uuid == friendUUID) {
				return true;
			}
		}
		return false;
	}

	protected boolean doesCloseFriendshipExist(UUID friendUUID) {
		for (HHParent currentFriend : closeFriends) {
			if (currentFriend.uuid == friendUUID) {
				return true;
			}
		}
		return false;
	}

	protected boolean doesFamilyRelationshipExist(UUID friendUUID) {
		for (HHParent currentFriend : respectedFamilyMembers) {
			if (currentFriend.uuid == friendUUID) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void talkToNetwork(){
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
}

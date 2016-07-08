import ec.util.MersenneTwisterFast;

public class EndUseGenerator {
	

	//give agent initial bathroom status, with a probability based on the timestep
	public static boolean bathroomInHouseInitialStatus(double timeStepAgentAdded, double initialProbability, double timeStepDelta){
		//as agent is introduced, give it a probability based on the timestep of having a bathroom in the house
		MersenneTwisterFast rng = new MersenneTwisterFast();
		double num = rng.nextDouble(true, true);//includes both 0 and 1 in interval
		System.out.println("num: " + num);
		System.out.println("timeStep: " + timeStepAgentAdded);
		System.out.println("initialProbability: " + initialProbability);
		double currentProbability = initialProbability + timeStepDelta * timeStepAgentAdded;
		System.out.println("currentProbability: " + currentProbability);
		if(num < currentProbability){
			return true;
		}
		else{
			return false;
		}		
	}
	
	//add bathrooms in house if they don't have any according to probability distribution 
	//make sure you check if bathroom is in house before calling this function
	public static boolean updateBathroomInHouseStatus(double timeStep, double timeStepDelta){
		MersenneTwisterFast rng = new MersenneTwisterFast();
		double num = rng.nextDouble(true, true);//includes both 0 and 1 in interval
		if (num < timeStepDelta){
			return true;
		}
		return false;
	}
	
	
	
}

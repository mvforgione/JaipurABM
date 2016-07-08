
public class EndUseTester {

	public static void main(String[] args) {
		for(int i = 0; i < 10; i++){
			System.out.println();
			System.out.println("testing EndUseGenerator: ");
			//bathroomInHouseInitialStatus(double timeStep, double initialProbability, double timeStepDelta)
			System.out.print(EndUseGenerator.bathroomInHouseInitialStatus(i,0.5, 0.01));
		}
	}

}

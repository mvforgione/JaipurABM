import static org.junit.Assert.*;

import org.junit.Test;


public class TestProbabilityOfBehavior {

	@Test
	public void testProbabilityConsToCons(){
		double result = ProbabilityOfBehaviorOriginal.probabilityConsToCons(0.0, 0.0);
		assertTrue(result <= 1);
	}

}

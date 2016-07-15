import static org.junit.Assert.*;

import org.junit.Test;

public class TestHHParent {

	/*public static int generateNumDualFlushToilets() {
		int numDualFlushToilets = 0;
		while (numDualFlushToilets == 0) {
			PoissonDistribution toiletDist = new PoissonDistribution(3, 0.2);//creates poisson distribution with a mean of 3 and a convergence of 0.2
			numDualFlushToilets = toiletDist.sample();
		}
		return numDualFlushToilets;
	}*/

	@Test
	public void testGenerateNumDualFlushToilets() {
		int num = HHParent.generateNumDualFlushToilets();
		//assertEquals(6, num);
		assertTrue(num <= 9);
	}
	

	@Test
	public void testGenerateNumDualFlushToilets2() {
		int num = HHParent.generateNumDualFlushToilets();
		//assertEquals(6, num);
		assertTrue(num <= 9);
	}	

}

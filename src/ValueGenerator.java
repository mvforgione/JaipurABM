import org.apache.commons.math3.distribution.*;

public class ValueGenerator {
	
	public static int getValueLikert(double avg, double stdDev){
		NormalDistribution normDist = new NormalDistribution(avg,stdDev);
		double generatedValue = -1;
		while (generatedValue < 1 || generatedValue > 5){
			generatedValue = normDist.sample();
		}
		int intValue = (int) (Math.round(generatedValue));			
		return intValue;
	}
	
	public static int getValuePercentage(double avg, double stdDev){
		NormalDistribution normDist = new NormalDistribution(avg,stdDev);
		double generatedValue = -1;
		while (generatedValue < 1 || generatedValue > 100){
			generatedValue = normDist.sample();
		}
		int intValue = (int) (Math.round(generatedValue));			
		return intValue;
	}
	
	public static int getValueWithRange(double avg, double stdDev, int min, int max){
		NormalDistribution normDist = new NormalDistribution(avg,stdDev);
		double generatedValue = -1;
		while (generatedValue < min || generatedValue > max){
			generatedValue = normDist.sample();
		}
		int intValue = (int) (Math.round(generatedValue));			
		return intValue;
	}
}

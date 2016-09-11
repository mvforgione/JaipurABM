
public class UtilityFunctionOriginal {
	static double a = 0.7;
	static double aPrime = 0.3;
	static double bPrime = 0.7;
	static double b = 0.3;
	static double exogenousTerm = 0;
	//UPDATED TO FIND NEW a/b values
//	static double a = 0.6;
//	static double b = 0.4;
//	static double aPrime = 0.4;
//	static double bPrime = 0.6;
//	static double a = 0.5;
//	static double b = 0.5;
//	static double aPrime = 0.5;
//	static double bPrime = 0.5;
	
	
	public static double calculateUtilityForConserverStayingConserver(double famRatio, int famDelta, double friendRatio, 
															int friendDelta, double acqRatio){
		
		
		double aFam = updateInitialParameter(famDelta, a);
		double aFriends = updateInitialParameter(friendDelta, a);
//		double aFam = a;
//		double aFriends = a;
		
		double utility = (a * acqRatio) + (aFam * famRatio) + (aFriends * friendRatio) + exogenousTerm;
		return utility;
	}
	
	public static double calculateUtilityForConserverBecomingNonConserver(double famRatio, int famDelta, double friendRatio, 
																int friendDelta, double otherRatio){
		//UPDATED TO TEST original a/b trends
		double bFam = updateInitialParameter(famDelta, b);
		double bFriends = updateInitialParameter(friendDelta, b);
//		double bFam = b;
//		double bFriends = b;
		double utility = (b * otherRatio) + (bFam * famRatio) + (bFriends * friendRatio);
		return utility;
	}
	
	public static double calculateUtilityForNonConserverBecomingConserver(double famRatio, int famDelta, double friendRatio, 
																int friendDelta, double otherRatio){
		//UPDATED TO TEST original a/b trends
		double aPrimeFam = updateInitialParameter(famDelta, aPrime);
		double aPrimeFriends = updateInitialParameter(friendDelta, aPrime);
//		double aPrimeFam =aPrime;
//		double aPrimeFriends = aPrime;
		double utility = (aPrime * otherRatio) + (aPrime * famRatio) + (aPrimeFriends * friendRatio) + exogenousTerm;
		return utility;
	}
	
	public static double calculateUtilityForNonConserverStayingNonConserver(double famRatio, int famDelta, double friendRatio, 
			int friendDelta, double otherRatio){
		//UPDATED TO TEST original a/b trends
		double bPrimeFam = updateInitialParameter(famDelta, bPrime);
		double bPrimeFriends = updateInitialParameter(friendDelta, bPrime);
//		double bPrimeFam = bPrime;
//		double bPrimeFriends = bPrime;
		double utility = (bPrime * otherRatio) + (bPrimeFam * famRatio) + (bPrimeFriends * friendRatio);
		return utility;
	}
	
	//this changes parameters for both switching behavior and not switching, for both conservers and nonconservers
	public static double updateInitialParameter(int delta, double coefficient){
		double newValue = 0.0;
		if(delta < 2 && delta > 0){ //increase a or b by 0.05 if delta = 1
			newValue =  coefficient + 0.05;
		}
		else if(delta == 2 || delta > 2){ //increase by 0.1 if delta = 2 or more
			newValue =  coefficient + 0.1;
		}
		else{
			newValue = coefficient;
		}
		return newValue;
	}
}

import sim.engine.*;

public class Students extends SimState{
	
	public Students(long seed){
		super(seed);
	}

	public static void main(String[] args){
		doLoop(Students.class, args);
		System.exit(0);
	}
}
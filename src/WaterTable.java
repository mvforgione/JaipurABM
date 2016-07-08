
public class WaterTable {

	//construct array of levels for each year, assuming 25 year span from txt file
	//give setters that reduce water levels
	public double waterTableLevelAtYear0;
	public double[] waterLevel={waterTableLevelAtYear0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			   0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
	public double waterInputsToWaterTableLevelConversion;//ratio of water table level change to volume of water inputs/withdrawals (height/volume)

	public WaterTable(double waterTableLevelAtYear0){//you must initialize object with the level at year 0
		this.waterTableLevelAtYear0 = waterTableLevelAtYear0;
	}
	
	public double getWaterTableLevel(int year){
		for (int i = 0; i < 25; i++){
			if(year == i){
				return waterLevel[i];
			}
		}
		return -1.0;
	}
	
	public void setWaterTableLevel(int year, double precipitation, double extraction){ //updates water table level based on precipitation and extractions and previous water table level		
		double newWaterTableLevel = waterLevel[year - 1] + (precipitation - extraction) * waterInputsToWaterTableLevelConversion;
		waterLevel[year] = newWaterTableLevel;
	}
}


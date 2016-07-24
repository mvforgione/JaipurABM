

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import edu.uci.ics.jung.*;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseGraph;
import edu.uci.ics.jung.graph.SparseMultigraph;
import ec.util.MersenneTwisterFast;

import org.apache.commons.collections15.Factory;
import org.apache.commons.math3.distribution.PoissonDistribution;

import sim.engine.SimState;
import sim.engine.Steppable;


	/**
	 * @author lizramsey
	 *         approximates behaviors of households with indoor plumbing
	 */

	public class HHwPlumbingSmallWorldNetwork extends HHParent {

		@Override
		public void setNetworkSize() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void talkToNetwork() {
			// TODO Auto-generated method stub
			talk(null, null);
		}
		
		protected void prepareStep(SimState state){
			setUpSocialNetwork();
		}

		public void step(SimState state) {
			prepareStep(state);
			//sets up the simstate
			//JaipurResidentialWUOriginal jaipurWaterUse = (JaipurResidentialWUOriginal) state; 

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
		
		
		private void setUpSocialNetwork(){
			Graph<Integer, String> socialNetwork = new SparseGraph<Integer, String>();
			socialNetwork.addVertex((Integer) 1);
			socialNetwork.addVertex(3);
			
			//needs to be use population data from parent to identify numbers of members
			//then needs to calculate or call static variables for Kleinberg network setup
			//then needs to call a method to set up the factories and such nonsense for the network, right?
			//then needs to call a method to fill in social network array
			//and somewhere in here it needs to make sure that we don't remake networks at every time step, but instead
			//      only append to them somewhere
		}

		

		

	}
		
		
		
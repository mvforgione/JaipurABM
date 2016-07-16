

import java.io.*;
import java.util.*;

import sim.engine.*;
import ec.util.MersenneTwisterFast;

import java.util.Iterator;

import org.apache.commons.collections15.Factory;
import org.apache.commons.math3.distribution.PoissonDistribution;

import agape.generators.RandGenerator;
import agape.tutorials.DirectedGraphFactoryForStringInteger;
import agape.tutorials.UndirectedGraphFactoryForStringInteger;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseGraph; 
import edu.uci.ics.jung.graph.event.GraphEvent.Edge;
import edu.uci.ics.jung.graph.event.GraphEvent.Vertex;
import edu.uci.ics.jung.graph.util.Pair;
import edu.uci.ics.jung.visualization.util.VertexShapeFactory;

/**
 *
 * @author lizramsey
 *
 */


public class JaipurResidentialWUSmallWorldNetwork extends JaipurResidentialWUOriginal{
	private double meanK = 0.5;

	static int edgeCount = 0;


	public JaipurResidentialWUSmallWorldNetwork(long seed) {
		super(seed);
		// TODO Auto-generated constructor stub
	}



	public static int generateK() {
		int k = 0;
		while (k == 0 || k > 100) {
			PoissonDistribution householdDist = new PoissonDistribution(9.2);    //from Kerala mobile phone study
			//for testing only
			//PoissonDistribution householdDist = new PoissonDistribution(3);
			k = householdDist.sample();
		}
		return k;
	}


	protected void prepareStep(SimState state){
		setUpSocialNetwork();
	}

	private void setUpSocialNetwork(){
		//needs to be use population data from parent to identify numbers of members
		//then needs to calculate or call static variables for Kleinberg network setup
		//then needs to call a method to set up the factories and such nonsense for the network, right?
		//then needs to call a method to fill in social network array
		//and somewhere in here it needs to make sure that we don't remake networks at every time step, but instead
		//      only append to them somewhere

	}

//	private Factory createVertexFactory(){
//		@SuppressWarnings("rawtypes")
//		Factory<Vertex> vertexFactory =
//		return vertexFactory;
//	}

	
}





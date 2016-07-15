import static org.junit.Assert.*;

import javax.xml.ws.ServiceMode;

import org.junit.Before;
import org.junit.Test;


public class TestHHwPlumbingOriginal {

	private HHwPlumbingOriginal myPlumb = null;
	
	@Before //runs before every single test in this class
	public void setup(){
		myPlumb = new HHwPlumbingOriginal(0, 0);
	}
	
	@Test
	public void testConstructor() {
		//fail("Not yet implemented");
		assertTrue(myPlumb != null);
		
	}
	
	@Test
	public void testGetVertexName(){
		myPlumb = new HHwPlumbingOriginal(1, 1);
		
		assertTrue(myPlumb != null);
		
		myPlumb.getVertexName();
	}

}

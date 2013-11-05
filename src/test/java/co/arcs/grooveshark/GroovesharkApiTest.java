package co.arcs.grooveshark;


public abstract class GroovesharkApiTest {

	public final void afterTest() {
		// Let the API cool down between tests
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}
	}
}

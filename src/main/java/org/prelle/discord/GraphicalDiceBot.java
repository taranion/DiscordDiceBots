package org.prelle.discord;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * @author prelle
 *
 */
public class GraphicalDiceBot extends Application {
	
	private SR6Bot botSR6;
	private SpliMoBot botSpliMo;

	//-------------------------------------------------------------------
   public static void main(String[] args) {
//	   System.setProperty("testfx.robot", "glass");
//       System.setProperty("testfx.headless", "true");
       Application.launch(args);
    }

	//-------------------------------------------------------------------
	/**
	 */
	public GraphicalDiceBot() {
		botSR6 = new SR6Bot();
		botSpliMo = new SpliMoBot();
	}

	//-------------------------------------------------------------------
	/**
	 * @see javafx.application.Application#start(javafx.stage.Stage)
	 */
	@Override
	public void start(Stage primaryStage) throws Exception {
		// TODO Auto-generated method stub

	}

}

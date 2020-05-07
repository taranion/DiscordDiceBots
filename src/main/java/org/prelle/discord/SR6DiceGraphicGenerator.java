package org.prelle.discord;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

/**
 * @author prelle
 *
 */
public class SR6DiceGraphicGenerator {
	
	//-------------------------------------------------------------------
	private static class DiceResult {
		int face;
		boolean wild;
		boolean ignoredHit;
		boolean exploded;
		public DiceResult(int face, boolean wild) {
			this.face = face;
			this.wild = wild;
		}
	}
	
	public static enum ResultType {
		CRITICAL_GLITCH,
		GLITCH,
		FAILURE,
		SUCCESS,
	}
	
	public static class DiceRollResult {
		byte[] image;
		String message;
		ResultType type; 
	}
	
	private final static Random RANDOM = new Random();	
	private final static int DICE_PER_ROW = 8;
	
	private static byte[] data;
	private static Map<String,Image> imagesByName = new HashMap<String, Image>();
	
	//-------------------------------------------------------------------
	private static void explode(List<DiceResult> result, boolean wild) {
		DiceResult die = new DiceResult(RANDOM.nextInt(6)+1, false);
		die.exploded = true;
		result.add( die );
		if (die.face==6) {
			explode(result, wild);
		}
	}

	//-------------------------------------------------------------------
	private static List<DiceResult> rollDice(int dice, int wild, int edge) {
		int numNormal = dice-wild + edge;
		System.out.println("("+dice+","+wild+","+edge+") = "+numNormal+" normal dice");
		List<DiceResult> result = new ArrayList<DiceResult>();
		// Roll regular dices
		for (int i=0; i<numNormal; i++) {
			DiceResult die = new DiceResult(RANDOM.nextInt(6)+1, false); 
			result.add( die );
			if (die.face==6 && edge>0) {
				explode(result, false);
			}
		}
		// Roll wild dices
		for (int i=0; i<wild; i++) {
			DiceResult die = new DiceResult(RANDOM.nextInt(6)+1, true); 
			result.add( die );
			if (die.face==6 && edge>0) {
				explode(result, true);
			}
		}
		
		return result;
	}

	//-------------------------------------------------------------------
	public static byte[] generate(List<DiceResult> result) {
		int rows = (int) Math.ceil(result.size()/(double)DICE_PER_ROW);
		Canvas canvas = new Canvas(DICE_PER_ROW*52, rows*52);
		int x=0;
		int y=0;
		for (DiceResult die : result) {
			canvas.getGraphicsContext2D().drawImage(getResultImage(die), x*52, y*52);
			x++;
			if (x==DICE_PER_ROW) {
				y++;
				x=0;
			}
		}
//		return canvas;
		return exportPngSnapshot(canvas, Color.TRANSPARENT);
	}

	//-------------------------------------------------------------------
	public static DiceRollResult generate(EmbedBuilder embed, int dice, int wild, int edge) {
		List<DiceResult> result = rollDice(dice, wild, edge);
		
		// First of all, check of the downside of the wild dies triggers
		boolean wildDieDownside = result.stream().anyMatch(die -> die.face==1 && die.wild && !die.exploded);
		
		/*
		 * Evaluate
		 */
		int hits = 0;
		int ones = 0;
		for (DiceResult die : result) {
			switch (die.face) {
			case 1:
				if (!die.exploded)
					ones++; 
				break;
			case 5:
				if (wildDieDownside) {
					die.ignoredHit = true;
					break;
				}
			case 6:
				if (die.wild && !die.exploded)
					hits+=3;
				else
					hits++; 
				break;
			}
		}
		
		/*
		 * Prepare return value
		 */
		DiceRollResult ret = new DiceRollResult();
		ret.image = generate(result);
		if (ones>(dice/2.0)) {
			// Glitch
			if (hits==0) {
				ret.type = ResultType.CRITICAL_GLITCH;
				ret.message = "CRITICAL GLITCH!";
			} else {
				ret.message = "GLITCH, but with "+hits+" hits";
				ret.type = ResultType.GLITCH;
			}
		} else {
			if (hits==0) {
				ret.type = ResultType.FAILURE;
				ret.message = "Failure";
			} else {
				ret.type = ResultType.SUCCESS;
				ret.message = hits+" hits";
			}
		}
		
		return ret;
	}

	//-------------------------------------------------------------------
	private static Image getResultImage(DiceResult die) {
		String filename = (die.wild?"wild_die_":"die_")+die.face;
		if (die.ignoredHit)
			filename += "_ignored";
		else if (die.exploded)
			filename += "_exploded";
		filename += ".png";
		if (imagesByName.containsKey(filename))
			return imagesByName.get(filename);
		Image image = new Image(ClassLoader.getSystemResourceAsStream(filename));
		imagesByName.put(filename, image);
		return image;
	}
	
	//-------------------------------------------------------------------
	private static byte[] exportPngSnapshot(Node node, Paint backgroundFill) {
        if (node.getScene() == null) {
            Scene snapshotScene = new Scene(new Group(node));
        }

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(backgroundFill);
        
        synchronized (node) {
        Platform.runLater(() -> {
            Image chartSnapshot = node.snapshot(params, null);
            PngEncoderFX encoder = new PngEncoderFX(chartSnapshot, true);
            data = encoder.pngEncode();
            synchronized (node) {
            	node.notify();
			}           
        });
        	try {
				node.wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        return data;
//        Image chartSnapshot = node.snapshot(params, null);
//        PngEncoderFX encoder = new PngEncoderFX(chartSnapshot, true);
//        return encoder.pngEncode();
    }
}

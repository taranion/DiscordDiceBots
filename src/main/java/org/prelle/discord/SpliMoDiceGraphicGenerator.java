package org.prelle.discord;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
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
import javafx.scene.text.Font;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

/**
 * @author prelle
 *
 */
public class SpliMoDiceGraphicGenerator {
	
	//-------------------------------------------------------------------
	private static class DiceResult {
		int face;
		boolean grandmaster;
		public DiceResult(int face, boolean gm) {
			this.face = face;
			this.grandmaster = gm;
		}
	}
	
	public static enum ResultType {
		FAILURE,
		CLOSE_FAILURE,
		SUCCESS,
	}
	
	public static enum Spectacular {
		CRIT,
		NORMAL,
		TRIUMPH,		
	}
	
	public static enum RollType {
		RISK,
		NORMAL,
		SAFE,
	}
	
	public static class DiceRollResult {
		byte[] image;
		String message;
		Spectacular spectacular;
		ResultType type;
		int value;
		int result;
		int difficulty;
		int successLevel;
		String titleFormat;
		byte[] thumbnail;
	}
	
	private final static Random RANDOM = new Random();	
	
	private static byte[] data;
	private static Map<String,Image> imagesByName = new HashMap<String, Image>();

	//-------------------------------------------------------------------
	private static List<DiceResult> rollDice(boolean risk, boolean grandmaster) {
		List<DiceResult> result = new ArrayList<DiceResult>();
		int dice = risk?4:2;
		// Roll regular dices
		for (int i=0; i<dice; i++) {
			DiceResult die = new DiceResult(RANDOM.nextInt(10)+1, false); 
			result.add( die );
		}
		
		if (grandmaster)  {
			DiceResult die = new DiceResult(RANDOM.nextInt(10)+1, true); 
			result.add( die );
		}
		
		return result;
	}

	//-------------------------------------------------------------------
	public static byte[] generate(List<DiceResult> result) {
		Canvas canvas = new Canvas(5*68, 57);
		int x=0;
		for (DiceResult die : result) {
			canvas.getGraphicsContext2D().drawImage(getResultImage(die), x*68, 0);
			x++;
		}
//		return canvas;
		return exportPngSnapshot(canvas, Color.TRANSPARENT);
	}

	//-------------------------------------------------------------------
	private static byte[] generateEGImage(boolean success, int eg) {
		Canvas canvas = new Canvas(60,60);
		canvas.getGraphicsContext2D().setFill(success?Color.GREEN:Color.RED);
		canvas.getGraphicsContext2D().fillRoundRect(0, 0, 60, 60, 8, 8);
		canvas.getGraphicsContext2D().setFill(success?Color.BLACK:Color.WHITE);
		canvas.getGraphicsContext2D().setFont(Font.font("serif", 40));
		canvas.getGraphicsContext2D().fillText(String.valueOf(eg), 15, 40);
		return exportPngSnapshot(canvas, Color.TRANSPARENT);
	}

	//-------------------------------------------------------------------
	public static DiceRollResult generate(EmbedBuilder embed, int value, int goal, RollType type, boolean grandmaster) {
		List<DiceResult> result = rollDice(type==RollType.RISK, grandmaster);
		
		String title = (grandmaster?"Großmeister %s":"%s");
		if (type==RollType.RISK)
			title += " macht einen Risikowurf";
		else  if (type==RollType.SAFE)
			title += " macht einen Sicherheitswurf";
		

		DiceRollResult ret = new DiceRollResult();
		ret.image = generate(result);
		ret.spectacular = Spectacular.NORMAL;
		ret.difficulty = goal;
		ret.value = goal;
		ret.titleFormat = title;

		/*
		 * Evaluate
		 */
		List<Integer> values = new ArrayList<Integer>();
		for (DiceResult die : result) {
			values.add(die.face);
		}
		Collections.sort(values);
		int lowestSum = 0;
		int highestSum = 0;
		lowestSum = values.get(0) + values.get(1);
		highestSum = values.get(values.size()-1) + values.get(values.size()-2);
		
		int roll = 0;
		if (type==RollType.SAFE) {
			roll = values.get(values.size()-1);
		} else {
			if (lowestSum<4) {
				ret.spectacular = Spectacular.CRIT;
				roll = lowestSum;
			} else {
				roll = highestSum;
				if (highestSum>18) {
					ret.spectacular = Spectacular.TRIUMPH;
				}
			}
		}
		
		// Count success level
		ret.message = "Ziel: **"+goal+"**  Wert  **"+value+"** + Wurf **"+roll;
		int sum = roll+value;
		ret.message += "**";
		ret.successLevel = (sum - goal)/3;
		if (ret.spectacular==Spectacular.CRIT) {
			ret.successLevel = Math.min(-1, ret.successLevel-3);
			ret.message += " (Patzer)";
		} else if (ret.spectacular==Spectacular.TRIUMPH) {
			if (sum>=goal) {
				ret.successLevel += 3;			
				ret.message += " (Triumph)";
			} else {
				ret.message += " (Triumph, aber kein Erfolg)";
			}
		}
		ret.message += " = Summe **"+sum+"**";
		ret.message += " = "+ret.successLevel+" EG ";
		ret.thumbnail = generateEGImage(sum>=goal, ret.successLevel);
		if (ret.successLevel>=5) {
			ret.message += "(Herausragend gelungen)";
		} else if (ret.successLevel>=3) {
			ret.message += "(Gut gelungen)";
		} else if (ret.successLevel>=1) {
			ret.message += "(gelungen)";
		} else if (ret.successLevel==0) {
			if (sum>=goal) {
				ret.message += "(knapp gelungen)";
			} else {
				ret.message += "(knapp misslungen)";
			} 
		} else if (ret.successLevel > -3) {
			ret.message += "(misslungen)";
		} else if (ret.successLevel > -5) {
			ret.message += "(schwer misslungen)";
		} else {
			ret.message += "(Verheerend misslungen)\nWurf auf Patzertabelle nötig";
		}
		
		return ret;
	}

	//-------------------------------------------------------------------
	private static Image getResultImage(DiceResult die) {
		String filename = "D10_"+die.face;
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

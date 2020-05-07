/**
 * 
 */
package org.prelle.discord;

import java.util.Map.Entry;

/**
 * @author prelle
 *
 */
public class NoJavaFXLauncher {
    
    public static void main(String[] args) {
        System.setProperty("prism.order", "sw");
        System.setProperty("prism.text", "t2k");
        System.setProperty("java.awt.headless", "true");
        System.setProperty("glass.plattform", "Monocle");
        System.setProperty("monocle.plattform", "Headless");
//        System.setProperty("jdk.gtk.verbose", "true");
        
        for (Entry<Object, Object> entry : System.getProperties().entrySet()) {
        	String key = (String)entry.getKey();
        	if (key.contains("java") || key.contains("gtk") || key.contains("glass") || key.contains("monocle")) {
        		System.out.println(key+" \t= "+entry.getValue());
        	}
        }
        
    	GraphicalDiceBot.main(args);
    }
 
}

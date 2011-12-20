package de.intranda.testICS;

import org.apache.log4j.Logger;

public class SystemHelper {
	
	
	public static void dumpMemory()  {
		System.out.println("\n"+"Max. memory: " + Runtime.getRuntime().maxMemory()/1024/1024 + " MB" +"\n" +
		"Tot. memory: " + Runtime.getRuntime().totalMemory()/1024/1024 + " MB" + "\n" +
		"Free memory: " + Runtime.getRuntime().freeMemory()/1024/1024 + " MB");
		}
}

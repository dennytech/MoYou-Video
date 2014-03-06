package com.dennytech.wiiivideo.parser;


public interface ParseHelper {
	
	String parse(String scriptName, String source);
	
	String parseByDefault(String source);
	
	void close();

}

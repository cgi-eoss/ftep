package com.cgi.eoss.ftep.core.requesthandler;

public class Main {
	
	public static void main(String[] args){
		
		Main main =new Main();
		main.start();
		
	}

	private void start() {
		String path = "index.html";
		
		String[] names = path.split("/");
		
		System.out.println(names[names.length -1]);
	}

}

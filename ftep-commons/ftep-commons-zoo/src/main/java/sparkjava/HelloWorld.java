package sparkjava;

import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.post;

import spark.Request;
import spark.Response;

public class HelloWorld {

	public static void main(String[] args) {
		
		HelloWorld helloWorld = new HelloWorld();
		
		port(4900);

//		get("/hello", (req, res) -> helloWorld.processIncomingWebRequest(req, res));
		
		get("/call", new HelloRoute());
		
		post("/testpost", new TestPost());

	}

	String processIncomingWebRequest(Request request, Response response) {
		return "test  dd" + request.userAgent().toString();
	}
}

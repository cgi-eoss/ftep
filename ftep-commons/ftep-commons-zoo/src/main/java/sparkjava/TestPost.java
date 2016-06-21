package sparkjava;

import spark.Request;
import spark.Response;
import spark.Route;

public class TestPost implements Route {

	@Override
	public Object handle(Request request, Response response) throws Exception {
		System.out.println("fsafsadf");
		
		response.status(201); 
		response.header("Access-Control-Allow-Origin", "*");
		return "sasadas";
	}

}

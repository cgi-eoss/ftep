package sparkjava;

import spark.Request;
import spark.Response;
import spark.Route;

public class HelloRoute implements Route{

	@Override
	public Object handle(Request request, Response response) throws Exception {
		// TODO Auto-generated method stub
		return "checking call";
	}

}

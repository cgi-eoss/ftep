package mustache;

import java.io.IOException;
import java.io.PrintWriter;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

public class Test {

//	static class Identifer {
//		Identifer(String _id) {
//			this.id = "esha";
//		}
//
//		String id;
//	}

	String id = "esha";
	public static void main(String[] args) throws IOException {
		MustacheFactory mf = new DefaultMustacheFactory();
		Mustache mustache = mf.compile("template.mustache");
		mustache.execute(new PrintWriter(System.out), new Test()).flush();
	}

}

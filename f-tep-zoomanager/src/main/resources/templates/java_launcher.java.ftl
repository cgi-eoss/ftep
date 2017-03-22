import com.cgi.eoss.ftep.wps.FtepServicesClient;

import java.util.HashMap;

public class ${id} {

    public static int ${id}(HashMap conf, HashMap inputs, HashMap outputs) {
        return FtepServicesClient.launch("${id}", conf, inputs, outputs);
    }

}
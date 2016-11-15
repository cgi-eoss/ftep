package com.cgi.eoss.ftep.core.wpswrapper;

import com.cgi.eoss.ftep.core.requesthandler.RequestHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;

@Slf4j
public abstract class AbstractWrapperProc {

    private String dockerImageName;

    public AbstractWrapperProc(String dockerImgName) {
        if (null != dockerImgName && !dockerImgName.isEmpty()) {
            dockerImageName = dockerImgName;
        } else {
            LOG.error("Missing Docker Image name");
        }
    }

    RequestHandler getRequestHandler(HashMap<String, HashMap<String, String>> conf,
                                     HashMap<String, HashMap<String, Object>> inputs, HashMap<String, HashMap<String, String>> outputs) {
        RequestHandler requestHandler = new RequestHandler(conf, inputs, outputs);
        return requestHandler;
    }

    boolean isSufficientCoinsAvailable() {
        // TODO Auto-generated method stub
        return true;
    }

    boolean isValidUser() {
        // TODO Auto-generated method stub
        return true;
    }

//	boolean getInputParameter(String string) {
//		// TODO Auto-generated method stub
//		return false;
//	}

    public String getDockerImageName() {
        return dockerImageName;
    }

}

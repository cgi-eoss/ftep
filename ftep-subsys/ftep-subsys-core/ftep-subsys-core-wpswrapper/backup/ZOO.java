package org.zoo.project;
import java.util.HashMap;

public class ZOO {
    static { System.loadLibrary("ZOO"); }
    public static Integer SERVICE_SUCCEEDED=3;
    public static Integer SERVICE_FAILED=4;
    public static native String _(String a);
    public static native Integer updateStatus(HashMap conf,String pourcent,String message);
}

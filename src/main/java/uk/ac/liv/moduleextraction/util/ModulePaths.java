package uk.ac.liv.moduleextraction.util;

import java.util.Map;

public class ModulePaths {

    private static Map<String, String> envVariables = System.getenv();
    
    public static String getOntologyLocation(){
    	return envVariables.get("ONTOLOGY_LOCATION");
    }
    
    public static String getSignatureLocation(){
    	return envVariables.get("SIGNATURE_LOCATION");
    }
    
    public static String getResultLocation(){
        String loc = envVariables.get("RESULT_LOCATION");
    	return (loc==null) ? "/tmp" : loc;
    }
    
    public static String getQBFSolverLocation(){
    	return envVariables.get("QBF_LOCATION");
    }


}

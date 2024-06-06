/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lanchat;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 *
 * @author Mear
 */
class Authentication {

    final static private String LANchat_SIGNATURE 
            = "LANchat_dEId6l8OKOQqXBv5Eq4gkFGRVqF7jSHc";
    final static public int SIG_BYTES_LEN 
            = LANchat_SIGNATURE.getBytes(StandardCharsets.UTF_8).length;

    public static boolean isAuthentic(Properties inpPeerProperties) {
        String inpSig = inpPeerProperties.getProperty("Signature");
        return inpSig != null && inpSig.equals(LANchat_SIGNATURE);                   
    }
   /* public static boolean isAuthentic(byte[] inputSignatureBytes) {
        String inpSignature = new String(inputSignatureBytes,
                0, SIG_BYTES_LEN, StandardCharsets.UTF_8);
        return inpSignature.equals(LANchat_SIGNATURE);
    }
    public static boolean isAuthentic(String inputSignature) {
        return inputSignature.equals(LANchat_SIGNATURE);
    } */
    
    public static String signatureStr(){
        return LANchat_SIGNATURE;
    }
    
    public static byte[] signatureBytes() {
        return LANchat_SIGNATURE.getBytes(StandardCharsets.UTF_8);
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lanchat.net;

import java.net.ServerSocket;

/**
 *
 * @author Mear
 */
public class TCP_Server implements NetStreamServer, Runnable {
    final private ServerSocket srvSocket;
    final private TCP_ConnectionServer connSrv;
    

    @Override
    public void run() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}

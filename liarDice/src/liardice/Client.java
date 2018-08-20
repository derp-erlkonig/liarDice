/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package liardice;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author allen
 */
public class Client {
    //client vars
    private Socket me;
    private String address;
    private InetAddress adrs;
    private int port;
    private BufferedReader in;
    
    //game vars
    private boolean isConnected;
    String[] pNames;
    int tCount;
    int[] myDice;
    
    public Client(String a){
        String[] parser = a.split("\\:");
        port = Integer.parseInt(parser[2]);
        byte[] BAddress = new byte[4];
        String[] str = parser[0].split("\\.");
        for(int i = 0; i<4; i++){
            BAddress[i] = (byte) Integer.parseInt(str[i]);
        }
        System.out.println("Attempting to reach address: "+ a);
        
        try {
            adrs = InetAddress.getByAddress(BAddress);
            me = new Socket(adrs, port);
            in = new BufferedReader(
                    new InputStreamReader(me.getInputStream()));
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        if(me != null){
            isConnected = true;
            System.out.println("Successfully connected!");
        }
        
        while(isConnected){
            try {
                processInput(in.readLine());
            } catch (IOException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public void processInput(String inp){
        
    }
}

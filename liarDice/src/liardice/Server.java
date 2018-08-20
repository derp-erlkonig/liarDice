/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package liardice;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author allen
 */
public class Server {
    //game variables
    private int betValue, betAmt, valueTotal, turnCounter, startingDice, currentPlayer;
    private boolean ready, called, isPlaying, hasRecievedAnswer;
    private int[] diceRolledAll, dicePerPlayer;
    
    //server variables
    private int port, maxClients; 
    Connection[] connections;
    static int readies = 0;
    private Thread[] threads;
    
    public Server(int p, int mC, int dice){
        ready = false;
        port = p;
        maxClients = mC;
        readies = 0;
        threads = new Thread[maxClients];
        connections = new Connection[maxClients];
        dicePerPlayer = new int[maxClients];
        startingDice = dice;
    }
    
    public void start(){
        new Thread(() -> {
            try{
            //creates new server socket for hosting comms at the specified port
            ServerSocket servSock = new ServerSocket(port);
            //waits until full clients or vote or override 
            for(int i=0; i<maxClients; i++){
                System.out.println("Waiting for clients to connect...");
                connections[i] = new Connection(servSock.accept(), i, this).start();
                connections[i].send("Welcome to the server!");
                System.out.println("Client has connected from: "+connections[i].socket.toString());
                if((readies >= (connections.length/2)) || ready){
                    Connection[] temp = new Connection[i];
                    temp = Arrays.copyOf(connections, i);
                    connections = temp.clone();
                    threads = new Thread[connections.length];
                    dicePerPlayer = new int[connections.length];
                    break;
                }
            }
            
            
            System.out.println("Setup Complete! Moving to gameplay!");
            betAmt = 0;
            turnCounter = 0;
            isPlaying = true;
            int temp = 0;
            for(int i=0; i<dicePerPlayer.length; i++){
                dicePerPlayer[i] = startingDice;
                temp += startingDice;
            }
            diceRolledAll = new int[temp];
            gameplay();
        }
        catch (IOException ex) {
            System.err.print(ex);
        }}).start();
    }
    
    public void gameplay(){
        diceRolledAll= roll(0);
        inRound();
        endRound();
        int currentPlayer = turnCounter%connections.length;
        connections[currentPlayer].out.println("Your turn!");
            
        
    }
    
    
    //*processes commands obtained via comms
    public synchronized int processInput(String in, int pl){
        if(in.contains("\\/voteStart") && !isPlaying){
            readies++;
            return pl;
        }
        if(in.contains("\\/raise") && isPlaying){
            betAmt ++;
            turnCounter ++;
            return pl;
        }
        if(in.contains("\\/call") && isPlaying){
            called = true;
            return pl;
        }
        if(in.contains("\\/setName")){
            String[] parsed = in.split("\\ ");
            try{
            connections[Integer.parseInt(parsed[1])].setName(parsed[2]);
            }catch(IndexOutOfBoundsException e){
                System.out.println("Wrong usage. Use: \\/setName playerNumber \\'name\\' ");
            }
        }
        return -1;
    }
    //SERVER METHODS
    
    //*sends a command to the specified client index
    public String sendCommand(String command, int clientNum){
        connections[clientNum].send(command);
        return command;
    } 
    //*manual override to start the game
    public void ready(){
        ready = true;
    }
    //*sets max clients
    public void setMaxClients(int m){
        maxClients = m;
    }
    //*returns max clients
    public int getMaxClients(){
        return maxClients;
    }
    //*sets the port for the server to listen on
    public void setPort(int p){
        port = p;
    }
    //*returns the port the server will listen on
    public int getPort(){
        return port;
    }
    
    //GAME METHODS
    
    //*rolls an arbitrary amount of dice and returns the resulting int[]
    public int[] roll(int amt){
        int[] rolled = new int[amt];
        for(int i = 0; i < amt; i++){
            rolled[i] = (int) (Math.random()*6+1);
            //debugging
            System.out.println(rolled[i]);
        }
        return rolled;
    }
    //*distributes the dice to each player
    public void distributeDice(){
        diceRolledAll = roll(diceRolledAll.length);
        int temp = 0;
        for(int i=0; i<dicePerPlayer.length; i++){
            connections[i].send("\\/setDice "+ Arrays.copyOfRange(diceRolledAll, temp, temp + dicePerPlayer[i]));
            temp+= dicePerPlayer[i];
        }
    }
    //*does the logic while in a round of gameplay
    public void inRound(){
        
        while(!called){
            hasRecievedAnswer = false;
            currentPlayer = turnCounter%connections.length;
            connections[currentPlayer].send("Your Turn!");
            connections[currentPlayer].send("\\/displayRorC");
            while(!hasRecievedAnswer){
                try {
                    if(processInput(connections[currentPlayer].in.readLine(), currentPlayer) == currentPlayer){
                        hasRecievedAnswer = true;
                    }
                } catch (IOException ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    //*does the logic of the end of a round
    public void endRound(){
        
    }
    private static class Connection implements Runnable{
        PrintWriter out;
        BufferedReader in;
        Socket socket;
        Server serv;
        boolean isConnected;
        Thread me;
        int playerNum;
        private String name;
        
        public Connection(Socket sock, int pN, Server s) {
            name = "Unnamed";
            playerNum = pN;
            try {
                socket = sock;
                out = new PrintWriter(new BufferedOutputStream(socket.getOutputStream()));
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                serv = s;
                isConnected = true;
            } catch (IOException ex) {
                System.err.print(ex);
            }
        }
        public Connection start(){
            if(me == null){
                me = new Thread(this);
                me.start();
            }
            return this;
        }
        public void stop(){
            try {
                isConnected = false;
                me.join();
            } catch (InterruptedException ex) {
               System.err.print(ex);
            }
        }
        public void run() {
            try{
                while(isConnected){
                    serv.processInput(in.readLine(), playerNum);
                }
            }catch(Exception e){
                stop();
            }
        }
        public void send(String cmd){
            out.println(cmd);
        }
        public String getName(){
            return name;
        }
        public void setName(String nm){
            name = nm;
        }
        public int getNum(){
            return playerNum;
        }
        public void setNum(int num){
            playerNum = num;
        }
    }
}


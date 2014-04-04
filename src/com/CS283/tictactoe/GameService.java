package com.CS283.tictactoe;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.StringTokenizer;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class GameService extends IntentService {
	
	//final variables for commands
	static final int PLACE_ = 1;
	static final int END_ = 2;
	static final int RECEIVE_ = 3;
	static final int PLACE_NO_RECEIVE_ = 4;
	
	//address to server and the port we will use.
	final String serverAddress_ = "54.186.216.144";
	final int serverPort_ = 20000;

	final static int MAX_PACKET_SIZE_ = 512;
	
	final InetSocketAddress serverSocketAddress_ = new InetSocketAddress(
			serverAddress_, serverPort_);

	//messengere to respond to the activity
	private Messenger returnMessenger_;
	
	private int gameID_;

	
	public GameService(){
		super("GameService");
	}
	
	/**
	 * Gets the gameID and return messenger from the intent
	 */
	public int onStartCommand(Intent intent, int flags, int startId){
		super.onStartCommand(intent, flags, startId);
		Log.d(getClass().getSimpleName(), "GameService onServiceStarted called");
		
		returnMessenger_ = (Messenger)intent.getExtras().get("MESSENGER");
		gameID_=intent.getExtras().getInt("GAME_ID_");
		return START_NOT_STICKY;
		
	}
	
	/**
	 * Overidden method
	 * Handles incoming intents and calls the corresponding method in a new thread
	 * @param an intent containing the instruction and necessary information to execute a command
	 */
	protected void onHandleIntent(Intent intent){
		switch(intent.getExtras().getInt("TYPE")){
		case PLACE_:
			Log.d(getClass().getSimpleName(), "GameService Place called");
			new WorkerThread(intent.getExtras().getInt("TYPE"),intent.getExtras().getInt("LOCATION"),this).start();
			break;
		case PLACE_NO_RECEIVE_:
			Log.d(getClass().getSimpleName(), "GameService Place called");
			new WorkerThread(intent.getExtras().getInt("TYPE"),intent.getExtras().getInt("LOCATION"),this).start();
			break;
		case END_:
			Log.d(getClass().getSimpleName(), "GameService END called");
			new WorkerThread(intent.getExtras().getInt("TYPE"),this).start();
			break;
		case RECEIVE_:
			Log.d(getClass().getSimpleName(), "GameService RECEIVE called");
			new WorkerThread(intent.getExtras().getInt("TYPE"),this).start();
			break;

			
		}
	}
	
	
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	/**
	 *Method to receive a move
	 *Polls the server every two seconds for a new message
	 *Once a message is received the message is acked, and the return messenger
	 *is used to send the move back to the activity. 
	 */
	public void receive(){
		String command;
		DatagramSocket socket = null;
		
		
		try{
			command = "POLL";
			socket = new DatagramSocket(serverPort_);
			DatagramPacket txPacket = new DatagramPacket(command.getBytes(),command.length(),serverSocketAddress_);
			
			byte[] buf = new byte[MAX_PACKET_SIZE_];
			DatagramPacket rxPacket = new DatagramPacket(buf, buf.length);
			
			socket.setSoTimeout(2000);
			boolean notReceived = true;
			//must keep polling until we have received a message
			while(notReceived){
				try{
					socket.send(txPacket);
					socket.receive(rxPacket);
					notReceived = false;
				}catch(SocketTimeoutException e){
					e.printStackTrace();
					txPacket = new DatagramPacket(command.getBytes(),command.length(),serverSocketAddress_);
				}
			}
			

			
			String payload = new String(rxPacket.getData(),0,rxPacket.getLength());
			
			//ack packet first
			command = "ACK "+ Integer.toString(gameID_)+ " " + payload;
			txPacket = new DatagramPacket(command.getBytes(),command.length(),serverSocketAddress_);
			socket.send(txPacket);
			
			StringTokenizer st = new StringTokenizer(payload);
			st.nextToken();
			int move = Integer.parseInt(st.nextToken());
			
			//build up the message
			Message msg = Message.obtain();
			Bundle bundle = new Bundle();
			bundle.putInt("MOVE", move);
			msg.setData(bundle);
			
			socket.close();
			returnMessenger_.send(msg);
			
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch(RemoteException e){
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Send a request to the
	 * @param location
	 * @param receive - if we should receive moves. Only false for last move of game.
	 */
	public void place(int location, boolean receive){
		//build up the message
		String command = "MSG " + gameID_ +" MOVE " + location;

		DatagramSocket socket = null;
		try {
			//Send the request
			socket = new DatagramSocket(serverPort_);
			DatagramPacket txPacket = new DatagramPacket(command.getBytes(),command.length(),serverSocketAddress_);
			socket.send(txPacket);
		}catch(SocketException e){
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		socket.close();
		if(receive){
			receive();
		}
	}

	/**
	 * Sends a request to end the game to the server.
	 * Request is sent from the player that receives the last move.
	 * @param location
	 */
	public void endGame(){

		//build up the message
		String command = "END "+ gameID_;

		DatagramSocket socket = null;
		try {
			//Send the request
			socket = new DatagramSocket(serverPort_);
			DatagramPacket txPacket = new DatagramPacket(command.getBytes(),command.length(),serverSocketAddress_);
			socket.send(txPacket);
		}catch(SocketException e){
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		socket.close();
		stopSelf();
	}
	
	/**
	 * Private class Executes all network commands on a separate thread
	 * A bit of overkill as this is an intent service which already has its own thread
	 * @author Ian Mundy
	 *
	 */
	private static class WorkerThread extends Thread{
		
		int instruction_;
		int location_;
		WeakReference<GameService> service_;
		
		public WorkerThread(int instruction,GameService service){
			instruction_=instruction;
			service_=new WeakReference<GameService>(service);
		}
		
		public WorkerThread(int instruction,int location,GameService service){
			instruction_=instruction;
			location_=location;
			service_=new WeakReference<GameService>(service);
		}
		
		public void run(){
			switch(instruction_){
			case PLACE_:
				GameService service = service_.get();
				service.place(location_,true);
				service = null;
				break;
			case PLACE_NO_RECEIVE_:
				GameService nService = service_.get();
				nService.place(location_,false);
				nService = null;
				break;
			case END_:
				GameService serviceEnd = service_.get();
				serviceEnd.endGame();
				serviceEnd = null;
				break;
			case RECEIVE_:
				GameService serviceReceive = service_.get();
				serviceReceive.receive();
				serviceReceive = null;
				break;
			}
		}

		
	}
}

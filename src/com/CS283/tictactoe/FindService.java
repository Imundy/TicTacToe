package com.CS283.tictactoe;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.StringTokenizer;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class FindService extends Service {
	
	final String serverAddress_ = "54.186.216.144";
	final int serverPort_ = 20000;
			
	
	final static int MAX_PACKET_SIZE_ = 512;
	
	final InetSocketAddress serverSocketAddress_ = new InetSocketAddress(
			serverAddress_, serverPort_);
	
	Messenger returnMessenger_;

	public IBinder onBind(Intent intent) {
		return null;
	}
	
	public void onCreate(){
		super.onCreate();
	}
	
	/**
	 * Starts this service
	 * Attempts to join game in the background
	 */
	public int onStartCommand(Intent intent, int flags, int startId){
		super.onStartCommand(intent, flags, startId);
		
		Log.d(getClass().getSimpleName(), "Find Service started");
		
		returnMessenger_= (Messenger)intent.getExtras().get("MESSENGER");
		new Thread(new Runnable(){
			public void run(){
				joinGame();
			}
		}).start();
		return START_NOT_STICKY;
	}
	
	public void onDestroy(){
		super.onDestroy();
	}
	
	public void joinGame(){
		String command = "JOIN";
		DatagramSocket socket = null;
		try {
			
			//Send the request
			Log.d(getClass().getSimpleName(), "creating SOCKET");
			socket = new DatagramSocket(serverPort_);
			Log.d(getClass().getSimpleName(), "socket created...");
			DatagramPacket txPacket = new DatagramPacket(command.getBytes(),command.length(),serverSocketAddress_);
			socket.send(txPacket);
			
			Log.d(getClass().getSimpleName(), "sending JOIN request");
			
			//wait for response
			byte[] buf = new byte[MAX_PACKET_SIZE_];
			DatagramPacket rxPacket = new DatagramPacket(buf, buf.length);
			socket.receive(rxPacket);
			String payload = new String(rxPacket.getData(),0,rxPacket.getLength());
			
			Log.d(getClass().getSimpleName(), "JOIN request granted");
			//parse response and return it
			Log.d(getClass().getSimpleName(),payload);
			StringTokenizer st = new StringTokenizer(payload);
			st.nextToken();
			int gameID = Integer.parseInt(st.nextToken());
			int first = Integer.parseInt(st.nextToken());
			
			//build up the message
			Message msg = Message.obtain();
			Bundle bundle = new Bundle();
			bundle.putInt("GAME_ID", gameID);
			bundle.putInt("FIRST",first);
			msg.setData(bundle);
			
			Log.d(getClass().getSimpleName(),payload);
			
			//if we are first we have to wait for an opponent to be found.
			if(first == 0){
				Log.d(getClass().getSimpleName(), "We are first POLLING");
				command = "POLL";
				txPacket = new DatagramPacket(command.getBytes(),command.length(),serverSocketAddress_);
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
				payload = new String(rxPacket.getData(),0,rxPacket.getLength());
				command = "ACK "+ Integer.toString(gameID)+ " " + payload;
				txPacket = new DatagramPacket(command.getBytes(),command.length(),serverSocketAddress_);
				socket.send(txPacket);
			}
			socket.close();
			//send message to messenger
			returnMessenger_.send(msg);
			
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch(RemoteException e){
			e.printStackTrace();
		}
	}

}

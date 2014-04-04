package com.CS283.tictactoe;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.view.Menu;
import android.view.View;

public class MainActivity extends Activity {

	private Messenger mService_ = null;
	
	private ProgressDialog pdialog_;
	
	private IncomingHandler handler_= new IncomingHandler(this);
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	protected void onDestroy(){
		super.onDestroy();
		Intent intent =new Intent(this, FindService.class);
		stopService(intent);
	}
	
	public void findGame(View view){
		
		pdialog_ = new ProgressDialog(this);
		pdialog_.setCancelable(false);
		pdialog_.setMessage("Finding Game");
		pdialog_.show();
		
		Intent intent = new Intent(this, FindService.class);
		mService_ = new Messenger(handler_);
		intent.putExtra("MESSENGER", mService_);
		Log.d(getClass().getSimpleName(), "Find Service called");
		startService(intent);
	}
	
	public void startGame(int user, int gameID){
		pdialog_.cancel();
		//put important data in intent and start next activity
		Intent intent = new Intent(getBaseContext(),GameActivity.class);
		intent.putExtra("USER", user);
		intent.putExtra("GAME_ID", gameID);
		Log.d(getClass().getSimpleName(), "Game started " + user +" " + gameID);
		startActivity(intent);
	}
	
	static class IncomingHandler extends Handler{
		
		WeakReference<MainActivity> activity_;
		
		public IncomingHandler(MainActivity activity){
			activity_ = new WeakReference<MainActivity>(activity);
		}
		
		public void handleMessage(Message msg){
				Bundle data = msg.getData();
				int user = data.getInt("FIRST");
				int gameID = data.getInt("GAME_ID");
				MainActivity mainActivity = activity_.get();
				mainActivity.startGame(user, gameID);
				mainActivity = null;
		}
		
	}
	

	
	

}

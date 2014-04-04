package com.CS283.tictactoe;

import java.lang.ref.WeakReference;
import java.util.Arrays;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class GameActivity extends Activity {
	
	//textView displaying whose turn it is
	TextView textView_;

	Messenger mMessenger_;
	private IncomingHandler handler_ = new IncomingHandler(this);
	
	//board
	private int[] board_ = new int[9];
	
	//tells whose turn it is
	private boolean myTurn;
	
	private int user_;
	private int game_id_;

	
	@Override
	/**
	 * OnCreate starts the GameService which listens for udp packets denoting moves
	 */
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_game);
		
		textView_= (TextView)findViewById(R.id.turn);
		
		Bundle extras = getIntent().getExtras();
		mMessenger_ = new Messenger(handler_);
		game_id_ = extras.getInt("GAME_ID");
		Log.d(getClass().getSimpleName(), "ONCREATE");
		user_ = extras.getInt("USER");
		Arrays.fill(board_, -1);
		if(user_==0){
			myTurn = true;
		}else{
			textView_.setText(R.string.waiting);
			myTurn = false;
			//send a message saying we need to receive
			Intent intent = new Intent(this,GameService.class);
			intent.putExtra("TYPE", 3);
			intent.putExtra("MESSENGER", mMessenger_);
			intent.putExtra("GAME_ID", game_id_);
			startService(intent);
		}
	}
	
	protected void onStart(){
		super.onStart();
		
	}
	
	protected void onStop(){
		super.onStop();
	}
	
	/**
	 * OnDestroy stops the service if it has not already stopped.
	 */
	protected void onDestroy(){
		super.onDestroy();
		Intent intent= new Intent(this,GameService.class);
		stopService(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	
	/**
	 * Below are the set of onClick methods for the activity
	 * @param view
	 */
	public void button0(View view){
		executeMove(view, 0);
	}
	
	public void button1(View view){
		executeMove(view, 1);
	}
	
	public void button2(View view){
		executeMove(view, 2);
	}
	
	public void button3(View view){
		executeMove(view, 3);
	}
	

	public void button4(View view){
		executeMove(view, 4);
	}
	
	public void button5(View view){
		executeMove(view, 5);
	}
	
	public void button6(View view){
		executeMove(view, 6);
	}
	
	public void button7(View view){
		executeMove(view, 7);
	}
	
	public void button8(View view){
		executeMove(view, 8);
	}
	/**
	 * END onClick methods
	 */
	
	/**
	 * Executes a move the user decided to make on the screen and sends it to opponent
	 * 
	 * @param view - the imageButton the user clicked
	 * @param location - the corresponding integer value
	 */
	public void executeMove(View view,int location){
		if(myTurn && board_[location] == -1){
			myTurn = false;
			board_[location] = user_;
			ImageButton button = (ImageButton)view;
			if(user_==0){
				button.setBackgroundResource(R.drawable.o_icon);
			}else{
				button.setBackgroundResource(R.drawable.x_icon);
			}
			button.setClickable(false);
			int gameStatus = checkIfOver();
			if(gameStatus==1){
				winner(user_,location);
			}else if(gameStatus==2){
				winner(2,location);
			}else{
				waitForTurn(location);
			}
			
		}else if(!myTurn){
			Toast toast = Toast.makeText(getApplicationContext(), 
										"Not Your Turn!", 
										Toast.LENGTH_SHORT);
			toast.show();
		}
	}
	
	/**
	 * We assume all opponent moves are valid
	 *
	 * @param location - location of opponents move
	 */
	public void opponentMove(int location){
		

		if(user_==0){
			board_[location] = 1;
		}else{
			board_[location] = 0;
		}
		
		ImageButton button = new ImageButton(this);
		
		//get the correct button
		switch(location){
			case 0:
				button =(ImageButton)findViewById(R.id.ImageButton0);
				break;
			case 1:
				button =(ImageButton)findViewById(R.id.ImageButton1);
				break;
			case 2:
				button =(ImageButton)findViewById(R.id.ImageButton2);
				break;
			case 3:
				button =(ImageButton)findViewById(R.id.ImageButton3);
				break;
			case 4:
				button =(ImageButton)findViewById(R.id.ImageButton4);
				break;
			case 5:
				button =(ImageButton)findViewById(R.id.ImageButton5);
				break;
			case 6:
				button =(ImageButton)findViewById(R.id.ImageButton6);
				break;
			case 7:
				button =(ImageButton)findViewById(R.id.ImageButton7);
				break;
			case 8://8 is default case so compiler knows that the button was initialized
				button =(ImageButton)findViewById(R.id.ImageButton8);
				break;
				
		}

		if(user_==0){
			button.setBackgroundResource(R.drawable.x_icon);
		}else{
			button.setBackgroundResource(R.drawable.o_icon);
		}
		button.setClickable(false);
		
		int gameStatus=checkIfOver();
		
		if(gameStatus==0){
			myTurn = true;
		}
		else if(gameStatus==1){
			if(user_==0){
				winner(1,-1);
			}else{
				winner(0,-1);
			}
		}else if(gameStatus==2){
			winner(2,-1);
		}
		
		textView_.setText(R.string.your_turn);

	}
	
	/**
	 * Tells the server we made a move and to wait for a response
	 * @param location
	 */
	public void waitForTurn(int location){
		textView_.setText(R.string.waiting);
		Intent intent = new Intent(this,GameService.class);
		intent.putExtra("TYPE", GameService.PLACE_);
		intent.putExtra("LOCATION", location);
		intent.putExtra("MESSENGER", mMessenger_);
		intent.putExtra("GAME_ID", game_id_);
		startService(intent);
	}
	
	/**
	 * Check if the board is over
	 * @return true if any win condition is true
	 */
	private int checkIfOver(){
			if((board_[0]==0&&board_[3]==0&&board_[6]==0)||
				(board_[0]==1&&board_[3]==1&&board_[6]==1)||
				(board_[1]==0&&board_[4]==0&&board_[7]==0)||
				(board_[1]==1&&board_[4]==1&&board_[7]==1)||
				(board_[2]==0&&board_[5]==0&&board_[8]==0)||
				(board_[2]==1&&board_[5]==1&&board_[8]==1)||
				(board_[0]==0&&board_[1]==0&&board_[2]==0)||
				(board_[0]==1&&board_[1]==1&&board_[2]==1)||
				(board_[3]==0&&board_[4]==0&&board_[5]==0)||
				(board_[3]==1&&board_[4]==1&&board_[5]==1)||
				(board_[6]==0&&board_[7]==0&&board_[8]==0)||
				(board_[6]==1&&board_[7]==1&&board_[8]==1)||
				(board_[0]==0&&board_[4]==0&&board_[8]==0)||
				(board_[0]==1&&board_[4]==1&&board_[8]==1)||
				(board_[2]==0&&board_[4]==0&&board_[6]==0)||
				(board_[2]==1&&board_[4]==1&&board_[6]==1)){
				return 1;
			}else if(board_[0]!=-1&&board_[1]!=-1&&board_[2]!=-1
					&&board_[3]!=-1&&board_[4]!=-1&&board_[5]!=-1
					&&board_[6]!=-1&&board_[7]!=-1&&board_[8]!=-1){
				return 2;
			}
			return 0;
	}
	
	/**
	 * The winner sends the end message
	 * @param player
	 * @param location
	 */
	public void winner(int player, int location){
		if(location!=-1){
			Intent intent = new Intent(this,GameService.class);
			intent.putExtra("TYPE", GameService.PLACE_NO_RECEIVE_);
			intent.putExtra("LOCATION", location);
			intent.putExtra("MESSENGER", mMessenger_);
			intent.putExtra("GAME_ID", game_id_);
			startService(intent);
		}else{
			//else we received the last move and now must end the game
			Intent intent = new Intent(this,GameService.class);
			intent.putExtra("TYPE", GameService.END_);
			intent.putExtra("MESSENGER", mMessenger_);
			intent.putExtra("GAME_ID", game_id_);
			startService(intent);
		}
		
		//create and display an alertDialog telling the user the game is over
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.game_over);
		if(player==2){
			builder.setMessage(R.string.tie);
		}else if(user_==player){
			builder.setMessage(R.string.win);
		}else{
			builder.setMessage(R.string.lose);
		}
		
		builder.setPositiveButton(R.string.return_home, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				GameActivity.this.finish();
			}
		});
		
		builder.show();
	}
	
	/**
	 * Our handlder that deals with messages received form the gameService
	 * @author Ian Mundy
	 *
	 */
	static class IncomingHandler extends Handler{
		
		WeakReference<GameActivity> activity_;
		
		/**
		 * Constructor
		 * @param activity - the activity to refer back to
		 */
		public IncomingHandler(GameActivity activity){
			activity_ = new WeakReference<GameActivity>(activity);
		}
		
		/**
		 * Handdle a message from the gameService
		 */
		public void handleMessage(Message msg){
			GameActivity gameActivity = activity_.get();
			Bundle data = msg.getData();
			int location = data.getInt("MOVE");
			gameActivity.opponentMove(location);
			gameActivity = null;
		}
		
	}
	
	

}

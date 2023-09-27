package com.example.pingpong;



import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;


public class GameThread extends Thread {



    public static final int STATE_READY = 0;
    public static final int STATE_PAUSED = 1;
    public static final int STATE_RUNNING = 2;
    public static final int STATE_WIN = 3;
    public static final int STATE_LOSE = 4;

    private static final int MATCH_POINT = 3;


    private boolean mSensorsOn;

    private final Context mCtx;
    private final SurfaceHolder mSurfaceHolder;
    private final PongTable mPongTable;
    private final Handler mGameStatusHandler;
    private final Handler mScoreHandler;

    private boolean mRun = false;
    private int mGameState;
    private final Object mRunLock;


    private static final int PHYS_FPS = 60;

    public GameThread(Context mCtx, SurfaceHolder mSurfaceHolder, PongTable mPongTable, Handler mGameStatusHandler, Handler mScoreHandler) {
        this.mCtx = mCtx;
        this.mSurfaceHolder = mSurfaceHolder;
        this.mPongTable = mPongTable;
        this.mGameStatusHandler = mGameStatusHandler;
        this.mScoreHandler = mScoreHandler;
        mRunLock = new Object();
    }


    @Override
    public void run() {

        long mNextGameTick = SystemClock.uptimeMillis();
        int skipTicks = 1000/ PHYS_FPS;


        while (mRun){
            Canvas c = null;
            try {

                c = mSurfaceHolder.lockCanvas(null);
                if (c!= null){
                    synchronized (mSurfaceHolder){
                        if (mGameState == STATE_RUNNING){
                            mPongTable.update(c);
                        }
                        synchronized (mRunLock){
                            if (mRun){
                                mPongTable.draw(c);
                            }
                        }
                    }
                }

            }catch (Exception e ){
                e.printStackTrace();
            }finally {
                if (c!=null){
                    mSurfaceHolder.unlockCanvasAndPost(c);
                }
            }


            mNextGameTick += skipTicks;
            long sleepTime = mNextGameTick - SystemClock.uptimeMillis();
            if (sleepTime > 0){
                try {
                    sleep(sleepTime);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }


        }


    }


    public void setState(int state) {
        synchronized (mSurfaceHolder) {
            mGameState = state;
            Resources res = mCtx.getResources();


            switch (mGameState) {

                case STATE_READY:
                    setUpNewRound();
                    break;

                case STATE_RUNNING:
                    hideStatusText();
                    break;

                case STATE_WIN:
                    mPongTable.getPlayer().score++;
                    setScoreText(String.valueOf(mPongTable.getPlayer().score), String.valueOf(mPongTable.getmOpponent().score));
                    if (mPongTable.getPlayer().score == MATCH_POINT) {
                        setStatusText(res.getString(R.string.mode_win)); // Update the status text accordingly.
                        mRun = false; // End the game as the player has reached the match point.
                    } else {
                        setUpNewRound();
                    }
                    break;

                case STATE_LOSE:
                    mPongTable.getmOpponent().score++;
                    setScoreText(String.valueOf(mPongTable.getPlayer().score), String.valueOf(mPongTable.getmOpponent().score));
                    if (mPongTable.getmOpponent().score == MATCH_POINT) {
                        setStatusText(res.getString(R.string.mode_loss)); // Update the status text accordingly.
                        mRun = false; // End the game as the opponent has reached the match point.
                    } else {
                        setUpNewRound();
                    }
                    break;

                case STATE_PAUSED:
                    setStatusText(res.getString(R.string.mode_paused));
                    break;
            }
        }
    }



    public void setUpNewRound(){
        synchronized (mSurfaceHolder) {
            mPongTable.setupTable();

            if (mPongTable.getPlayer().score == MATCH_POINT || mPongTable.getmOpponent().score == MATCH_POINT) {
                // End the game, you can set the game state to STATE_WIN or STATE_LOSE accordingly.
                if (mPongTable.getPlayer().score == MATCH_POINT) {
                    setState(STATE_WIN);
                } else {
                    setState(STATE_LOSE);
                }
            }
        }
    }


    public void setRunning(boolean running){
        synchronized (mRunLock){
            mRun = running;
        }
    }


    public boolean SensorsOn(){
        return mSensorsOn;
    }


    public boolean isBetweenRounds(){
        return mGameState != STATE_RUNNING;
    }



    private void setStatusText(String text){

        Message msg = mGameStatusHandler.obtainMessage();
        Bundle b = new Bundle();
        b.putString("text",text);
        b.putInt("visibility", View.VISIBLE);
        msg.setData(b);
        mGameStatusHandler.sendMessage(msg);

    }

    private void hideStatusText(){

        Message msg = mGameStatusHandler.obtainMessage();
        Bundle b = new Bundle();
        b.putInt("visibility",View.INVISIBLE);
        msg.setData(b);
        mGameStatusHandler.sendMessage(msg);

    }


    public void setScoreText(String playerScore,String opponentScore){
        Message msg = mScoreHandler.obtainMessage();
        Bundle b = new Bundle();
        b.putString("player",playerScore);
        b.putString("opponent",opponentScore);
        msg.setData(b);
        mScoreHandler.sendMessage(msg);

    }


}
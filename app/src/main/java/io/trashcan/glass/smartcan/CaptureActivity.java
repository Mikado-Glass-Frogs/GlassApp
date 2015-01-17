/*
 * SmartCan : Glass App.
 * @author Ivan Smirnov (http://ivansmirnov.name)
 *
 * This is a modified version of the ZXIng sample Glass Project.
 * It scans a QR barcode in the form (Integer UID, Integer code) where
 * the uid is a unique id for a trash or recycleable item, and the code specifies
 * which it is. This activity then sends a POST request to a Node.JS server on an intel
 * edison, which controls a smart trash can.
 *
 * Feel free to use and modify this code at your own risk, under the original ZXing
 * license.
 *
 * Credits to original authors of ZXing library: 2014 ZXing authors
 */

package io.trashcan.glass.smartcan;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.view.WindowUtils;
import com.google.zxing.Result;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ParsedResultType;
import com.google.zxing.client.result.ResultParser;
import com.google.zxing.client.result.TextParsedResult;
import com.google.zxing.client.result.URIParsedResult;

import java.io.IOException;

/**
 * @author Sean Owen, heavily modified by Ivan Smirnov for PennApps 2015
 */
public final class CaptureActivity extends Activity implements SurfaceHolder.Callback {

  private static final String TAG = Constants.TAG + "Vania";
  private static final String SCAN_ACTION = "com.google.zxing.client.android.SCAN";

  private boolean hasSurface;
  private boolean returnResult;
  private SurfaceHolder holderWithCallback;
  private Camera camera;
  private DecodeRunnable decodeRunnable;
  private Result result;
  private GestureDetector mGestureDetector;

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);

    // returnResult should be true if activity was started using 
    // startActivityForResult() with SCAN_ACTION intent
    Intent intent = getIntent();
    returnResult = intent != null && SCAN_ACTION.equals(intent.getAction());

    Window window = getWindow();
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    window.requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS); // for voice menu

    mGestureDetector = createGestureDetector(this);

    setContentView(R.layout.capture);
  }

  @Override
  public synchronized void onResume() {
    super.onResume();
    SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
    SurfaceHolder surfaceHolder = surfaceView.getHolder();
    if (surfaceHolder == null) {
      throw new IllegalStateException("No SurfaceHolder?");
    }
    if (hasSurface) {
      initCamera(surfaceHolder);
    } else {
      surfaceHolder.addCallback(this);
      holderWithCallback = surfaceHolder;
    }
  }

  @Override
  public synchronized void onPause() {
    result = null;
    if (decodeRunnable != null) {
      decodeRunnable.stop();
      decodeRunnable = null;
    }
    if (camera != null) {
      camera.stopPreview();
      camera.release();
      camera = null;
    }
    if (holderWithCallback != null) {
      holderWithCallback.removeCallback(this);
      holderWithCallback = null;
    }
    super.onPause();
  }

  @Override
  public synchronized void surfaceCreated(SurfaceHolder holder) {
    Log.i(TAG, "Surface created");
    holderWithCallback = null;
    if (!hasSurface) {
      hasSurface = true;
      initCamera(holder);
    }
  }

  @Override
  public synchronized void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    // do nothing
  }

  @Override
  public synchronized void surfaceDestroyed(SurfaceHolder holder) {
    Log.i(TAG, "Surface destroyed");
    holderWithCallback = null;
    hasSurface = false;
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (result != null) {
      switch (keyCode) {
        case KeyEvent.KEYCODE_DPAD_CENTER:
          handleResult(result);
          return true;
        case KeyEvent.KEYCODE_BACK:
          reset();
          return true;
      }
    }
    return super.onKeyDown(keyCode, event);
  }

  private void initCamera(SurfaceHolder holder) {
    if (camera != null) {
      throw new IllegalStateException("Camera not null on initialization");
    }
    camera = Camera.open();
    if (camera == null) {
      throw new IllegalStateException("Camera is null");
    }

    CameraConfigurationManager.configure(camera);

    try {
      camera.setPreviewDisplay(holder);
      camera.startPreview();
    } catch (IOException e) {
      Log.e(TAG, "Cannot start preview", e);
    }

    decodeRunnable = new DecodeRunnable(this, camera);
    new Thread(decodeRunnable).start();
    reset();
  }

    void setResult(Result result) {
        Log.d("QR RESULT", "setting result");
        if (returnResult) {
            Intent scanResult = new Intent("com.google.zxing.client.android.SCAN");
            scanResult.putExtra("SCAN_RESULT", result.getText());
            String qrString = result.getText();
            Log.d("QR RESULT", qrString);
            setResult(RESULT_OK, scanResult);
            finish();
        } else {
            TextView statusView = (TextView) findViewById(R.id.status_view);
            String text = result.getText();

            // error proofing
            int uid;
            int code;
            String prompt = "";
            try {
                String[] params = text.split(",");
                if (params.length != 2) {
                    throw new Exception();
                }
                uid = Integer.parseInt(params[0]);
                code = Integer.parseInt(params[1]);
                Log.d(TAG, String.format("UID: %d, code: %d \n", uid, code));
            } catch (Exception e) { // catch format errors - not ints, no commas, etc
                code = -1; // set bad code
            }


            switch (code) {
                case 0:
                    prompt = "Alas, this is trash!";
                    break;
                case 1:
                    prompt = "Yay! This can be recycled";
                    break;
                default:
                    prompt = "Unknown code, use your best judgement";
            }

            // post to local edison - make it ready
            // TODO figure out how to POST a json in android/java
            
            
            // POST to mongo server - this user, and the uid of the item, plus the code (in case we haven't seen it yet
            // TODO

            SetStatusText(prompt);
            this.result = result;
        }
    }

    private void SetStatusText(String message) {
        TextView statusView = (TextView) findViewById(R.id.status_view);
        statusView.setText(message);
        statusView.setTextSize(TypedValue.COMPLEX_UNIT_SP, Math.max(14, 56 - message.length() / 4));
        statusView.setVisibility(View.VISIBLE);
    }

    private void handleResult(Result result) { //
    Log.d("QR RESULT", "handling result");
    ParsedResult parsed = ResultParser.parseResult(result);
    Intent intent;
    if (parsed.getType() == ParsedResultType.URI) {
      intent = new Intent(Intent.ACTION_VIEW, Uri.parse(((URIParsedResult) parsed).getURI()));
    } else {
      intent = new Intent(Intent.ACTION_WEB_SEARCH);
      intent.putExtra("query", ((TextParsedResult) parsed).getText());
    }
    startActivity(intent);
  }

  private synchronized void reset() {
    TextView statusView = (TextView) findViewById(R.id.status_view);
    statusView.setVisibility(View.GONE);
    result = null;
    decodeRunnable.startScanning();
  }

    // Gesture code
    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (mGestureDetector != null) {
            return mGestureDetector.onMotionEvent(event);
        }
        return false;
    }


    /** Detect various gestures */
    private GestureDetector createGestureDetector(Context context) {
        GestureDetector gestureDetector = new GestureDetector(context);

        //Create a base listener for generic gestures
        gestureDetector.setBaseListener( new GestureDetector.BaseListener() {
            @Override
            public boolean onGesture(Gesture gesture) {
                if (gesture == Gesture.TAP) {
                    openOptionsMenu();
                    return true;
                } else if (gesture == Gesture.TWO_TAP) {
                    // do something on two finger tap
                    return true;
                } else if (gesture == Gesture.SWIPE_RIGHT) {
                    // do something on right (forward) swipe
                    return true;
                } else if (gesture == Gesture.SWIPE_LEFT) {
                    // do something on left (backwards) swipe
                    return true;
                } else if (gesture == Gesture.SWIPE_DOWN){
                    finish();
                }
                return false;
            }
        });
        gestureDetector.setFingerListener(new GestureDetector.FingerListener() {
            @Override
            public void onFingerCountChanged(int previousCount, int currentCount) {
                // do something on finger count changes
            }
        });

        gestureDetector.setScrollListener(new GestureDetector.ScrollListener() {
            @Override
            public boolean onScroll(float displacement, float delta, float velocity) {
                // do something on scrolling
                return true;
            }
        });

        return gestureDetector;
    }

    // menu code
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS || featureId ==  Window.FEATURE_OPTIONS_PANEL) {
            switch (item.getItemId()) {
                case R.id.find_smartcan:
                    findSmartcan();
                    break;
                case R.id.cancel:
                    closeOptionsMenu();
                    break;
                case R.id.scan_again:
                    reset();
                    break;
                case R.id.quit_app:
                    quitApp();
                    break;
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    /**
     *  Locate smart trash can and direct user to it.
     *  */
    private void findSmartcan() {
        Log.d(Constants.TAG, "locating smartcan");
    }

    /** Used to quit app and clear all state. */
    private void quitApp() {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory( Intent.CATEGORY_HOME );
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(homeIntent);
    }



    /**
     * Adds a voice menu to the app
     * */
    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu){
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS || featureId ==  Window.FEATURE_OPTIONS_PANEL) {
            getMenuInflater().inflate(R.menu.main, menu);
            return true;
        }
        return super.onCreatePanelMenu(featureId, menu);
    }
}

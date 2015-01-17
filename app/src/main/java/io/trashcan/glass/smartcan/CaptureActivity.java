/*
 * Copyright (C) 2014 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * @author Sean Owen
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
    if (returnResult) {
      Intent scanResult = new Intent("com.google.zxing.client.android.SCAN");
      scanResult.putExtra("SCAN_RESULT", result.getText());
      setResult(RESULT_OK, scanResult);
      finish();
    } else {
      TextView statusView = (TextView) findViewById(R.id.status_view);
      String text = result.getText();
      statusView.setText(text);
      statusView.setTextSize(TypedValue.COMPLEX_UNIT_SP, Math.max(14, 56 - text.length() / 4));
      statusView.setVisibility(View.VISIBLE);
      this.result = result;
    }
  }

  private void handleResult(Result result) {
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

    @Override
    public void onBackPressed() {
        Log.d(Constants.TAG, "stopping scanning");
        //mBarcodePicker.stopScanning();
        //finish();
        // do something here?

    }

    // menu code
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS || featureId ==  Window.FEATURE_OPTIONS_PANEL) {
            switch (item.getItemId()) {
                case R.id.trash:
                    processAction(Constants.TRASH);
                    break;
                case R.id.recycle:
                    processAction(Constants.RECYCLE);
                    break;
                case R.id.cancel:
                    processAction(Constants.CANCEL);
                    break;
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    /**
     * process a user action - contact the server, get result, etc.
     * */
    public void processAction(String action){
        // branch on type
        switch (action) {
            case Constants.TRASH:
                Log.d(Constants.TAG, "trash");
                break;

            case Constants.RECYCLE:
                Log.d(Constants.TAG, "recycle");
                break;

            case Constants.CANCEL:
                Log.d(Constants.TAG, "cancel");
                //break;
                return;

            default:
                Log.d(Constants.TAG, "unknown action!! ABORT.");
        }

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
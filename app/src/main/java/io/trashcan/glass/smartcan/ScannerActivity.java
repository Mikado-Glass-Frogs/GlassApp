package io.trashcan.glass.smartcan;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Toast;

import com.google.android.glass.app.Card;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.view.WindowUtils;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;
import com.mirasense.scanditsdk.ScanditSDKAutoAdjustingBarcodePicker;
import com.mirasense.scanditsdk.interfaces.ScanditSDK;
import com.mirasense.scanditsdk.interfaces.ScanditSDKListener;
import com.mirasense.scanditsdk.interfaces.ScanditSDKOnScanListener;
import com.mirasense.scanditsdk.interfaces.ScanditSDKScanSession;


/**
 * Scanner Activity - detects barcode and sends data to server for processing.
 */
public class ScannerActivity extends Activity implements ScanditSDKListener, ScanditSDKOnScanListener {

    /**
     * {@link CardScrollView} to use as the main content view.
     */
    private CardScrollView mCardScroller; // replaceme
    private GestureDetector mGestureDetector;
    private View mView;

    // The main object for recognizing a displaying barcodes.
    private ScanditSDK mBarcodePicker;


    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        // keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);

        // set view
        mView = buildView();

        mCardScroller = new CardScrollView(this);
        mCardScroller.setAdapter(new CardScrollAdapter() {
            @Override
            public int getCount() {
                return 1;
            }

            @Override
            public Object getItem(int position) {
                return mView;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                return mView;
            }

            @Override
            public int getPosition(Object item) {
                if (mView.equals(item)) {
                    return 0;
                }
                return AdapterView.INVALID_POSITION;
            }
        });



        // Show menu on tap
        mCardScroller.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                openOptionsMenu();
            }
        });

        mGestureDetector = createGestureDetector(this);
        //setContentView(mCardScroller);

        ScanditSDKAutoAdjustingBarcodePicker mBarcodePicker = new ScanditSDKAutoAdjustingBarcodePicker(
                this, Constants.scanditSdkAppKey, ScanditSDKAutoAdjustingBarcodePicker.CAMERA_FACING_BACK); // or facing back?


        // TODO add menu listener

        // mBarcodePicker.getRootView().addListener

        mBarcodePicker.getOverlayView().addListener(this);

        mBarcodePicker.addOnScanListener(this);

        mBarcodePicker.startScanning();


        // set the view to the barcode picker
        setContentView(mBarcodePicker);

    }




    /**
     * Builds Scan Barcode Card
     */
    private View buildView() {
        Card card = new Card(this);
        card.setText(R.string.app_name);
        card.setImageLayout(Card.ImageLayout.LEFT);
        //card.addImage(R.drawable.logo); // add logo later? nah, make a viewport
        return card.getView();
    }


    @Override
    public void didScanBarcode(String barcode, String symbology) {
        Log.d(Constants.TAG, "didScanbarcode");
        // Remove non-relevant characters that might be displayed as rectangles
        // on some devices. Be aware that you normally do not need to do this.
        // Only special GS1 code formats contain such characters.
        String cleanedBarcode = "";
        for (int i = 0 ; i < barcode.length(); i++) {
            if (barcode.charAt(i) > 30) {
                cleanedBarcode += barcode.charAt(i);
            }
        }
        Toast.makeText(this, symbology + ": " + cleanedBarcode, Toast.LENGTH_LONG).show();
        Log.d(Constants.TAG, symbology + ": " + cleanedBarcode);
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

    /* process a user action - contact the server, get result, etc. */
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

    @Override
    public void didScan(ScanditSDKScanSession scanditSDKScanSession) {
        Log.d(Constants.TAG, "did scan");
    }

    @Override
    public void didCancel() {
        mBarcodePicker.stopScanning();
        finish();
    }



    @Override
    public void didManualSearch(String s) {
        // not used
        Log.d(Constants.TAG, "didmanualSearch");
    }

    @Override
    public void onBackPressed() {
        Log.d(Constants.TAG, "stopping scanning");
        mBarcodePicker.stopScanning();
        finish();
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (mGestureDetector != null) {
            return mGestureDetector.onMotionEvent(event);
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mBarcodePicker != null) {
            mBarcodePicker.startScanning();
        }
    }

    @Override
    protected void onPause() {
        if (mBarcodePicker != null) {
            mBarcodePicker.stopScanning();
        }
        super.onPause();
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


}


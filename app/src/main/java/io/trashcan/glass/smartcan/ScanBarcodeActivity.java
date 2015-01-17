package io.trashcan.glass.smartcan;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;

import com.google.android.glass.app.Card;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.view.WindowUtils;
import com.google.android.glass.widget.CardScrollView;

/**
 *
 * */
public class ScanBarcodeActivity extends Activity  {
    /**
     * {@link com.google.android.glass.widget.CardScrollView} to use as the main content view.
     */
    private CardScrollView mCardScroller;
    private GestureDetector mGestureDetector;
    private View mView;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        // keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);




        // Show menu on tap
        mCardScroller.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                openOptionsMenu();
            }
        });

        mGestureDetector = createGestureDetector(this);
        setContentView(mCardScroller);
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
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (mGestureDetector != null) {
            return mGestureDetector.onMotionEvent(event);
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCardScroller.activate();
    }

    @Override
    protected void onPause() {
        mCardScroller.deactivate();
        super.onPause();
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
}

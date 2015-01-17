package io.trashcan.glass.smartcan;


import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;

import com.google.android.glass.app.Card;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.view.WindowUtils;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static com.google.zxing.EncodeHintType.ERROR_CORRECTION;

/**
 * Scanner Activity - detects barcode and sends data to server for processing.
 */
public class ScannerActivity extends Activity {

    /**
     * {@link CardScrollView} to use as the main content view.
     */
    private CardScrollView mCardScroller; // replaceme
    private GestureDetector mGestureDetector;
    private View mView;






    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        Log.d(Constants.TAG, "about to read image");

        int testImageCount = 5;
        for (int i = 0; i <= testImageCount; i++) {
            Log.d(Constants.TAG, "reading image number " + Integer.toString(i));
            readImage(i);
        }


        Log.d(Constants.TAG, "done reading image");
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
        setContentView(mCardScroller);


    }


    // ====== MANUAL WORK ==========

    // perform image processing on the image identified in the parameter
    // filePath extract the data then parse it and return String
    public static String readCode(Bitmap fileData, String charset, Map hintMap) {
        String result;
        Log.d(Constants.TAG, "readCode called");

        // convert to BinaryBitmap
        BinaryBitmap data = convertToBinaryBitmap(fileData);

        Log.d(Constants.TAG, "done converting");

        Result qrCodeResult = null;
        try {
            qrCodeResult = new MultiFormatReader().decode(data,
                    hintMap);
            Log.d(Constants.TAG, "read successfully");
        } catch (NotFoundException e) {
            e.printStackTrace();
            Log.d(Constants.TAG, "not found");
            return null;
        }
        result = qrCodeResult.getText();

        return result;
    }

    private static BinaryBitmap convertToBinaryBitmap(Bitmap bMap) {
        if (bMap == null) {
            Log.d(Constants.TAG, "NULL bitmap sent to conversion. FIX!");
        }
        int[] intArray = new int[bMap.getWidth() * bMap.getHeight()];
        //copy pixel data from the Bitmap into the 'intArray' array
        bMap.getPixels(intArray, 0, bMap.getWidth(), 0, 0, bMap.getWidth(), bMap.getHeight());
        LuminanceSource source = new RGBLuminanceSource(bMap.getWidth(), bMap.getHeight(), intArray);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        return bitmap;
    }

    private String readImage(int num) {

        String charset = "UTF-8";
        Map hintMap = new HashMap();
        hintMap.put(ERROR_CORRECTION, ErrorCorrectionLevel.L);
        // TODO add QR code and 1D hint map - see barcode eye

        // access saved image
        Log.d(Constants.TAG, "getting path");
        String sdcardPath = Environment.getExternalStorageDirectory().toString();
        File imgFile = new File(sdcardPath + "/Pictures/SmartCan/test" + Integer.toString(num) +".jpg"); // TODO make path permanent
        Log.d(Constants.TAG, imgFile.getAbsolutePath());
        Bitmap fileData = null;
        if (imgFile.exists()) {
            Log.d(Constants.TAG, "file exists");
            fileData = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
        }

        String decodedCode = readCode(fileData, charset, hintMap);
        Log.d(Constants.TAG, String.format("Data read from code #%d: %s ", num, decodedCode));
        return decodedCode;
    }

    // ===============


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
                //readImage();
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


}


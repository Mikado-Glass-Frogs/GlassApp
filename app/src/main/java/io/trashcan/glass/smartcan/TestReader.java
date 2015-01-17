package io.trashcan.glass.smartcan;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.EncodeHintType;
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


/**
 * Created by root on 1/17/15.
 */
public class TestReader {

    String TAG = "TestReader";

    public static void main(String[] args) {


        //Thread t = new QRThread(0);
        // t.start();

    }


    // perform image processing on the image identified in the parameter
    // filePath extract the data then parse it and return String
    public static String readCode(Bitmap fileData, String charset, Map hintMap) {
        String result;

        // convert to BinaryBitmap
        BinaryBitmap data = convertToBinaryBitmap(fileData);

        Result qrCodeResult = null;
        try {
            qrCodeResult = new MultiFormatReader().decode(data,
                    hintMap);
        } catch (NotFoundException e) {
            e.printStackTrace();
            return null;
        }
        result = qrCodeResult.getText();

        return result;
    }

    private static BinaryBitmap convertToBinaryBitmap(Bitmap bMap) {
        int[] intArray = new int[bMap.getWidth()*bMap.getHeight()];
        //copy pixel data from the Bitmap into the 'intArray' array
        bMap.getPixels(intArray, 0, bMap.getWidth(), 0, 0, bMap.getWidth(), bMap.getHeight());
        LuminanceSource source = new RGBLuminanceSource(bMap.getWidth(), bMap.getHeight(),intArray);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        return bitmap;
    }


    // thread to handle concurrency
    public class QRThread extends Thread {

        // integer identifier
        private int num;

        // default thread constructor
        private QRThread(int num) {
            this.num = num;
        }

        public void run() {
            String charset = "UTF-8";
            Map hintMap = new HashMap();
            hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
            // TODO add QR code and 1D hint map - see barcode eye

            // access saved image
            String sdcardPath = Environment.getExternalStorageDirectory().toString();
            File imgFile = new  File(sdcardPath + "/SmartCan/image.jpg"); // TODO fix path
            Log.d(TAG, imgFile.getAbsolutePath());
            Bitmap fileData = null;
            if(imgFile.exists()) {
                fileData = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            }

            String decodedCode = readCode(fileData, charset, hintMap);

            Log.d(TAG , "Data read from code: " + decodedCode);
        }
    }
}
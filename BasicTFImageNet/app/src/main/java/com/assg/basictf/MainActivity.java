package com.assg.basictf;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    static final int REQUEST_IMAGE_CAPTURE = 1;
    Uri photoURI;
    ImageView mImageView;
    TextView mResultText;
    Button mDetectButton;

//    config values for Tensorflow classifier
    private static final int INPUT_SIZE = 224;
    private static final int IMAGE_MEAN = 117;
    private static final float IMAGE_STD = 1;
    private static final String INPUT_NAME = "input";
    private static final String OUTPUT_NAME = "output";
    private static final String MODEL_FILE = "file:///android_asset/tensorflow_inception_graph.pb";
    private static final String LABEL_FILE =
            "file:///android_asset/imagenet_comp_graph_label_strings.txt";
    private Classifier classifier;

    private Executor executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mImageView = (ImageView) findViewById(R.id.image_view);
        mDetectButton = (Button) findViewById(R.id.detect_btn);
        mResultText = (TextView) findViewById(R.id.result_text);

        initTensorFlowAndLoadModel();

        mDetectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                Bitmap bitmap = null;
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(MainActivity.this.getContentResolver(),photoURI);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(bitmap!=null) {

                    final List<Classifier.Recognition> results = classifier.recognizeImage(getResizedBitmap(bitmap));

                    if (results.size() > 0) {
                        String value = " Result is : \n";
                        for(Classifier.Recognition result: results){
                            value += result.getTitle() + " " + result.getConfidence() + "\n";
                        }
                        mResultText.setText(value);
                    }
                }
            }
        });
    }

    private void initTensorFlowAndLoadModel() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    classifier = TFImageNetClassifier.create(
                            getAssets(),
                            MODEL_FILE,
                            LABEL_FILE,
                            INPUT_SIZE,
                            IMAGE_MEAN,
                            IMAGE_STD,
                            INPUT_NAME,
                            OUTPUT_NAME);

                    makeButtonVisible();
                    Log.d(TAG, "Load Success");
                } catch (final Exception e) {
                    throw new RuntimeException("Error initializing TensorFlow!", e);
                }
            }
        });
    }

    private void makeButtonVisible() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDetectButton.setVisibility(View.VISIBLE);
            }
        });
    }

    public static Bitmap getResizedBitmap(Bitmap bm) {
//        resize only if image is large size
        int width;
        width = bm.getWidth();
        int height;
        height = bm.getHeight();

        float scaleWidth = ((float) INPUT_SIZE) / width;
        float scaleHeight = ((float) INPUT_SIZE) / height;
        // create a matrix for the manipulation
        Matrix matrix = new Matrix();
        // resize the bit map
        matrix.postScale(scaleWidth, scaleHeight);
        // recreate the new Bitmap
        return Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                classifier.close();
            }
        });
    }


    //    code to get image from camera
//    =============================
    public void clickPic(View view) {
        Intent clickPicIntent = new Intent();
        clickPicIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
        if (clickPicIntent.resolveActivity(getPackageManager()) != null) {


            File dir = new File(this.getCacheDir(), "images");
            dir.mkdirs();
            File photoFile = new File(dir.getPath() + "/capture_action.jpeg");

            photoURI = FileProvider.getUriForFile(this,
                    this.getPackageName() + ".fileprovider",
                    photoFile);
            clickPicIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

            startActivityForResult(clickPicIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            mImageView.setImageURI(null);
            mImageView.setImageURI(photoURI);
        }
    }
//    =============================
}

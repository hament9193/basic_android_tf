package com.assg.basictfstyletransfer;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Size;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import static com.assg.basictfstyletransfer.Manifest.permission.WRITE_EXTERNAL_STORAGE;


//** note image needs to be square
public class MainActivity extends AppCompatActivity implements RecyclerAdapter.RCallback{


    static final int REQUEST_IMAGE_CAPTURE = 1;
    Uri photoURI;

    RecyclerView recyclerView;
    Button clickBtn;
    Button saveBtn;
    ImageView imageViewOrig;
    ImageView imageViewTrans;
    public static int desiredSize = 1080;

    Size origSize;
    RecyclerAdapter mRecyclerAdapter;
    private int[] intValues;
    private float[] floatValues;

    Bitmap origBitmap = null;
    Bitmap transBitmap = null;


    private static final String MODEL_FILE = "file:///android_asset/stylize_quantized.pb";
    private static final String INPUT_NODE = "input";
    private static final String STYLE_NODE = "style_num";
    private static final String OUTPUT_NODE = "transformer/expand/conv3/conv/Sigmoid";
    private static final int NUM_STYLES = 26;

    private final float[] styleVals = new float[NUM_STYLES];
    private int lastStyle=1;

    private TensorFlowInferenceInterface inferenceInterface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.thumb_recycler_view);
        clickBtn = findViewById(R.id.click_btn);
        saveBtn = findViewById(R.id.save_btn);
        imageViewOrig = findViewById(R.id.image_view_orig);
        imageViewTrans = findViewById(R.id.image_view_trans);

        mRecyclerAdapter = new RecyclerAdapter(this, NUM_STYLES);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setAdapter(mRecyclerAdapter);

        inferenceInterface = new TensorFlowInferenceInterface(getAssets(), MODEL_FILE);

        clickBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickPic(view);
            }
        });

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    int result = checkSelfPermission(WRITE_EXTERNAL_STORAGE);
                    if(result != PackageManager.PERMISSION_GRANTED){
                        requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE}, 0);
                        return;
                    }
                }
                if(ImageUtils.saveBitmap(transBitmap, ""+Calendar.getInstance().getTime().getTime()+".jpg")){
                    Toast.makeText(MainActivity.this, "Saved to sdcard/BasicTFStyleTransfer.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void applyStyle(int position) {
        if (origBitmap==null){
            Toast.makeText(this, "Please take a picture first!", Toast.LENGTH_SHORT).show();
            return;
        }
        styleVals[lastStyle] = 0.0f;
        styleVals[position] = 1.0f;
        lastStyle = position;
        transBitmap = Bitmap.createScaledBitmap(origBitmap, desiredSize, desiredSize, false);
        if(transBitmap!=null){
//            make style transfer
            stylizeImage(transBitmap);
        }
    }

    private void stylizeImage(final Bitmap bitmap) {

        intValues = new int[bitmap.getHeight() * bitmap.getWidth()];

        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        floatValues = new float[bitmap.getHeight() * bitmap.getWidth() * 3];

            for (int i = 0; i < intValues.length; ++i) {
                final int val = intValues[i];
                floatValues[i * 3] = ((val >> 16) & 0xFF) / 255.0f;
                floatValues[i * 3 + 1] = ((val >> 8) & 0xFF) / 255.0f;
                floatValues[i * 3 + 2] = (val & 0xFF) / 255.0f;
            }

        // Copy the input data into TensorFlow.
        inferenceInterface.feed(
                INPUT_NODE, floatValues, 1, bitmap.getWidth(), bitmap.getHeight(), 3);
        inferenceInterface.feed(STYLE_NODE, styleVals, NUM_STYLES);

        inferenceInterface.run(new String[] {OUTPUT_NODE});
        inferenceInterface.fetch(OUTPUT_NODE, floatValues);

        for (int i = 0; i < intValues.length; ++i) {
            intValues[i] =
                    0xFF000000
                            | (((int) (floatValues[i * 3] * 255)) << 16)
                            | (((int) (floatValues[i * 3 + 1] * 255)) << 8)
                            | ((int) (floatValues[i * 3 + 2] * 255));
        }

        bitmap.setPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        transBitmap = getResizedBitmap(transBitmap, origSize);
        imageViewTrans.setImageBitmap(transBitmap);

        Toast.makeText(this, "image style completed", Toast.LENGTH_LONG).show();
    }


    public static Bitmap getResizedBitmap(Bitmap bm, Size size) {
//        resize only if image is large size
        int width;
        width = bm.getWidth();
        int height;
        height = bm.getHeight();

        float scaleWidth = ((float) size.getWidth()) / width;
        float scaleHeight = ((float) size.getHeight()) / height;
        // create a matrix for the manipulation
        Matrix matrix = new Matrix();
        // resize the bit map
        matrix.postScale(scaleWidth, scaleHeight);
        // recreate the new Bitmap
        return Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
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
            try {
                origBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(),photoURI);
                origSize = new Size(origBitmap.getWidth(), origBitmap.getHeight());
                imageViewOrig.setImageBitmap(origBitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
//    =============================
}

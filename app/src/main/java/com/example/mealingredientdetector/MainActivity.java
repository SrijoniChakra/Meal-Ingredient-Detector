package com.example.mealingredientdetector;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.lifecycle.Lifecycle;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
//import android.widget.Toolbar;
import androidx.appcompat.widget.Toolbar;
import com.google.mlkit.vision.objects.DetectedObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.task.vision.detector.Detection;
import org.tensorflow.lite.task.vision.detector.ObjectDetector;

public class MainActivity extends AppCompatActivity {
    private Button captureImageFab;
    private Button gallery;
    private ImageView inputImageView;
    private ImageView imgSampleOne;
    private ImageView imgSampleTwo;
    private ImageView imgSampleThree;
    private TextView tvPlaceholder;
    private String currentPhotoPath;
    public static final String TAG = "TFLite - ODT";
    public static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final float MAX_FONT_SIZE = 50.0F;
    private static final int CAMERA_INTENT = 111;
    int SELECT_PICTURE = 200;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        captureImageFab = findViewById(R.id.captureImageFab);
        gallery=findViewById(R.id.gallery);
        inputImageView = findViewById(R.id.imageView);
        imgSampleOne=findViewById(R.id.imgSampleOne);
        imgSampleTwo=findViewById(R.id.imgSampleTwo);
        imgSampleThree=findViewById(R.id.imgSampleThree);
        tvPlaceholder=findViewById(R.id.tvPlaceholder);
        captureImageFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchTakePictureIntent();
            }
        });

        gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                imageChooser();
            }
        });


        imgSampleOne.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setImageAndDetect(getImage(R.drawable.img_meal_one));
            }
        });

        imgSampleTwo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setImageAndDetect(getImage(R.drawable.img_meal_two));
            }
        });

        imgSampleThree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setImageAndDetect(getImage(R.drawable.img_meal_three));
            }
        });

    }

    void imageChooser() {

        // create an instance of the
        // intent of the type image
        Intent i = new Intent();
        i.setType("image/*");
        i.setAction(Intent.ACTION_GET_CONTENT);

        startActivityForResult(Intent.createChooser(i, "Select Picture"), SELECT_PICTURE);
    }


    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.mealingredientdetector.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            //Bundle extras = data.getExtras();
            //Bitmap imageBitmap = (Bitmap) extras.get("data");

            setImageAndDetect(getCapturedImage());
        }
        else if(requestCode == SELECT_PICTURE && resultCode == RESULT_OK){
            Uri selectedImageUri = data.getData();
            if (null != selectedImageUri) {
                // update the preview image in the layout
                try {
                    Bitmap bitmap1 = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                    setImageAndDetect(bitmap1);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    private Bitmap getImage(int drawable){
        Resources res = this.getResources();
        BitmapFactory.Options var = new BitmapFactory.Options();
        var.inMutable=true;
        return BitmapFactory.decodeResource(res,drawable,var);
    }

    private Bitmap getCapturedImage() {
        int targetW=inputImageView.getWidth();
        int targetH=inputImageView.getHeight();
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(currentPhotoPath, bmOptions);

        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.max(1, Math.min(photoW/targetW, photoH/targetH));

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;
        Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath, bmOptions);
        Bitmap rotatedBitmap = null;
        try {
            ExifInterface exifInterface = new ExifInterface(currentPhotoPath);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            Log.d("TFLite - ODT", "Orientation: "+orientation);
            switch(orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    Log.d("TFLite - ODT", " Rotate 90");
                    rotatedBitmap = rotateImage(bitmap, 90);
                    break;

                case ExifInterface.ORIENTATION_ROTATE_180:
                    Log.d("TFLite - ODT", " Rotate 180");
                    rotatedBitmap = rotateImage(bitmap, 180);
                    break;

                case ExifInterface.ORIENTATION_ROTATE_270:
                    Log.d("TFLite - ODT", " Rotate 270");
                    rotatedBitmap = rotateImage(bitmap, 270);
                    break;

                case ExifInterface.ORIENTATION_NORMAL:
                    Log.d("TFLite - ODT", " No Rotate");
                default:
                    rotatedBitmap = bitmap;
            }
        }catch (IOException e) {
            e.printStackTrace();
        }

        return rotatedBitmap;
    }

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }

    private void setImageAndDetect(Bitmap bitmap){
        inputImageView.setImageBitmap(bitmap);
        tvPlaceholder.setVisibility(View.INVISIBLE);

        runObjectDetection(bitmap);
    }
    private void runObjectDetection(Bitmap bitmap){
        TensorImage img = TensorImage.fromBitmap(bitmap);
        ObjectDetector.ObjectDetectorOptions options = ObjectDetector.ObjectDetectorOptions.builder().setMaxResults(5).setScoreThreshold(0.5F).build();
        try {
            ObjectDetector objectDetector = ObjectDetector.createFromFileAndOptions(this, "salad.tflite", options);
            List<Detection> results = objectDetector.detect(img);
            for(Detection detection : results) {
                RectF box = detection.getBoundingBox();
                Log.d("TFLite - ODT", "  boundingBox: (" + box.left + ", " + box.top + ") - (" + box.right + ',' + box.bottom + ')');
                List<Category> categories = detection.getCategories();
                for (Category cat : categories) {
                    Log.d("TFLite - ODT", "  Label: " +cat.getLabel());
                    int conf= (int) (cat.getScore()*(float)100);
                    Log.d("TFLite - ODT", "  Confidence: " +conf);
                }
            }
            Bitmap imgWithResult = drawDetectionResult(bitmap,results);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    inputImageView.setImageBitmap(imgWithResult);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private Bitmap drawDetectionResult(Bitmap bitmap,List<Detection> results){
        Bitmap outBitmap = bitmap.copy(Bitmap.Config.ARGB_8888,true);
        Canvas canvas = new Canvas(outBitmap);
        Paint pen = new Paint();
        Paint pen1 = new Paint();
        for(Detection detection : results) {
            List<Category> categories = detection.getCategories();
            for (Category cat : categories) {
                RectF box = detection.getBoundingBox();
                pen1.setColor(Color.RED);
                pen1.setStrokeWidth(8F);
                pen1.setStyle(Paint.Style.STROKE);
                Rect tagSize = new Rect(0, 0, 0, 0);
                pen.setTextAlign(Paint.Align.LEFT);
                pen.setStyle(Paint.Style.FILL_AND_STROKE);
                pen.setColor(Color.YELLOW);
                pen.setStrokeWidth(2F);
                pen.setTextSize(MAX_FONT_SIZE);
                int conf = (int) (cat.getScore() * (float) 100);
                pen.getTextBounds(cat.getLabel(), 0, cat.getLabel().length(), tagSize);
                Float fontSize = pen.getTextSize() * box.width()/ tagSize.width();
                if (fontSize < pen.getTextSize()) {
                    pen.setTextSize(fontSize);
                }
                Log.d(TAG, "  font: " +fontSize);
                Float margin = (box.width() - tagSize.width()) / 2.0F;
                if (margin < 0F) {
                    margin = 0F;
                }
                if (conf > 60) {
                    canvas.drawRect(box, pen1);
                    canvas.drawText(cat.getLabel(), box.left + margin, box.top, pen);
                    canvas.drawText(String.valueOf(conf), box.left + margin, box.top + tagSize.height()  * 1F, pen);
                }

            }
        }
        return outBitmap;
    }

}
package com.koi.arte;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import yuku.ambilwarna.AmbilWarnaDialog;

import static android.support.v4.content.FileProvider.getUriForFile;



public class MainActivity extends AppCompatActivity {

    //Canny 네이티브 함수
    public native void Canny(long photoAddr, long edgeAddr, long binaryAddr, long grayAddr);

    //라이브러리 로딩
    static
    {
        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV Load Error", "This app do not loaded OpenCV!");
        }
        else {

            Log.v("static :OpenCV Load", "Success Load OpenCV");

            System.loadLibrary("opencv_java3");
            System.loadLibrary("native-lib");
        }

    }

    //퍼미션배열
    private String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA};

    //퍼미션 콜백변수
    private static final int MULTIPLE_PERMISSIONS = 101; //권한 동의 여부 문의 후 CallBack 함수에 쓰일 변수



    //스크린 사이즈 변수
    int width;
    int height;

    //canvas(도화지) 객체 선언
    DrawLine drawLine;

    //context
    public static Context context;

    //btn 배열
    private int[] btns = {R.id.btnBlack};

    //색상 배열
    private int[] colors = {Color.BLACK};




    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN); //전체화면

        //백그라운드 색변경
        getWindow().getDecorView().setBackgroundColor(Color.WHITE);

        //UI 뿌리기
        setContentView(R.layout.activity_main);

        //안드로이드버전 6.0이상이면 권한묻기
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            //권한 묻기
            checkPermissions();

        }

        //컨텍스트설정
        context = MainActivity.this;

        //액션바 타이틀을 'Arte'로
        getSupportActionBar().setTitle("Arte");


        //비트맵그리기를 위한 디바이스 가로세로 사이즈 구하기
        Point pt = new Point();
        getWindowManager().getDefaultDisplay().getSize(pt);
        ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getSize(pt);
        height = pt.x;
        width = pt.y;

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        //메뉴 등록
        MenuInflater inflater=getMenuInflater();
        inflater.inflate(R.menu.menu,menu);
        return true;
    }
    Paint tmpPaint = new Paint();

    //화면 가로 세로 크기 조절
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        switch (newConfig.orientation)
        {
            case Configuration.ORIENTATION_LANDSCAPE:
                drawLine.canvas.drawRect(0,0,height,width,tmpPaint);
                break;
            case Configuration.ORIENTATION_PORTRAIT:
                drawLine.canvas.drawRect(0,0,width,height,tmpPaint);
                break;
        }

    }

    @Override
    public  boolean onOptionsItemSelected(MenuItem item)
    {
        //메뉴아이템 클릭했을때 이벤트
        switch (item.getItemId())
        {
            case R.id.btnColorPicker:
                AmbilWarnaDialog dialog= new AmbilWarnaDialog(context, Color.WHITE, new AmbilWarnaDialog.OnAmbilWarnaListener() {
                    @Override
                    public void onCancel(AmbilWarnaDialog dialog) {

                    }

                    @Override
                    public void onOk(AmbilWarnaDialog dialog, int color) {
                        drawLine.setLineColor(color);
                    }
                });
                dialog.show();
                return true;
            case R.id.btnEraser:
                drawLine.setEraser();
                return true;
            case R.id.btnAllEraser:
                tmpPaint.setColor(Color.WHITE);
                drawLine.setLineColor(Color.BLACK);
                //drawLine.invalidate();

                return true;

            case R.id.thickness:
                drawLine.setWidth();
                return true;
            case R.id.UnderDraw:
                DialogInterface.OnClickListener cameraListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        doTakephotoAction();
                    }
                };
                DialogInterface.OnClickListener albumListener= new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        doTakeAlbumAction();
                    }
                };
                DialogInterface.OnClickListener cancelLisntener=new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                };

                new AlertDialog.Builder(this)
                        .setTitle("변환할 사진및 이미지 선택")
                        .setPositiveButton("사진촬영",cameraListener)
                        .setNegativeButton("앨범선택",albumListener)
                        .setNeutralButton("취소",cancelLisntener)
                        .show();
                return true;
            case R.id.illust:

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

//----------------------------------------------------------------------------------------------------------------------------------------------------------------
    //Color button Method

    @Override
    public void onWindowFocusChanged(boolean hasFocus)
    {
        if(hasFocus && drawLine == null)
        {
            LinearLayout llCanvas = (LinearLayout)findViewById(R.id.llCanvas);
            if(llCanvas != null)
            {
                Rect rect = new Rect(0, 0,
                        llCanvas.getMeasuredWidth(), llCanvas.getMeasuredHeight());
                drawLine = new DrawLine(this, rect);
                llCanvas.addView(drawLine);
            }
            resetCurrentMode(0);
        }
        super.onWindowFocusChanged(hasFocus);
    }

    private void resetCurrentMode(int curMode)  //버튼 배경색과 글자색 체인지
    {
        for(int i=0;i<btns.length;i++)
        {
            Button btn = (Button) findViewById(btns[i]);
            if(btn != null)
            {
                btn.setBackgroundColor(i==curMode?0xff555555:0xffffffff);
                btn.setTextColor(i==curMode?0xffffffff:0xff555555);
            }
        }
        //초기화 글자색 알림
        if(drawLine != null) drawLine.setLineColor(colors[curMode]);
    }

    public void btnClick(View view)     //버튼 클릭
    {
        if(view == null) return;

        for(int i=0;i<btns.length;i++)
        {
            if(btns[i] == view.getId())
            {
                resetCurrentMode(i);
                break;
            }
        }
    }

    //-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    //picture method
    public static final int PICK_CARMERA=0;
    public static final int PICK_ALBUM=1;
    public static final int CROP_IMAGE=2;

    public Uri ImageCaptureUri;


    //포토파일 생성기(경로만들고 jpg생성)
    public File createPhotoFile() throws IOException {
        File storage = new File(Environment.getExternalStorageDirectory() + "/Arte");
        String fileName = "tmp_"+ String.valueOf(System.currentTimeMillis())+".jpg";

        if(!storage.exists()) storage.mkdirs();

        File img = File.createTempFile(fileName, ".jpg", storage);

        return img;

    }

    public void doTakephotoAction() //카메라 찍기
    {

        File photoFile = null;

        try {
            photoFile = createPhotoFile();
        }
        catch (IOException e) {
            Toast.makeText(getApplicationContext(), "이미지처리오류" + e, Toast.LENGTH_LONG).show();
        }

        if(photoFile!=null) {
            ImageCaptureUri = getUriForFile(getApplicationContext(), "com.koi.arte.fileprovider", photoFile);

            //String url="tmp_"+ String.valueOf(System.currentTimeMillis())+".jpg";

           // ImageCaptureUri=Uri.fromFile(new File(Environment.getExternalStorageDirectory(),url));

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, ImageCaptureUri);
            startActivityForResult(intent,PICK_CARMERA);

        }

    }

    public void doTakeAlbumAction() //앨범에서 불러오기
    {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
        intent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        startActivityForResult(intent, PICK_ALBUM);
    }

    @Override
    public void onActivityResult(int requestCode,int resultCode,Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (resultCode != RESULT_OK)
            return;

        switch (requestCode) {
            //앨범
            case PICK_ALBUM: {
                ImageCaptureUri = data.getData();
                Log.d("TestImage", ImageCaptureUri.getPath().toString());


            }

            case PICK_CARMERA: {

                Intent intent = new Intent("com.android.camera.action.CROP");
                intent.setDataAndType(ImageCaptureUri, "image/*");

                Log.v("imageCaptureUri", "" + ImageCaptureUri);

                intent.putExtra("scale", true);
                intent.putExtra("return-data", true);

                //안드로이드버전 6.0이상이면 권한묻기
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                    //권한관련
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                }


                startActivityForResult(intent, CROP_IMAGE);
                break;
            }

            case CROP_IMAGE: { //이미지 크롭

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                    //권한 설정
                    this.grantUriPermission("com.android.camera", ImageCaptureUri,
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);


                }

                if (resultCode != RESULT_OK) {
                    return;
                }

                final Bundle extras = data.getExtras();

                if (extras != null) {

                    Log.v("MainActivity", "CropImage Success");

                    Bitmap photo = extras.getParcelable("data");


                    //비트맵에 사이즈 설정하기

                    photo = Bitmap.createScaledBitmap(photo, height, width, true);





                    Log.e("Canny", "Canny start");

                    //네이티브함수에 필요한 Mat변수등록
                    Mat tmp = new Mat(height, width, CvType.CV_8UC4);
                    Mat edge = new Mat(height, width, CvType.CV_8UC4);
                    Mat gray = new Mat(height, width, CvType.CV_8UC4);
                    Mat binary = new Mat(height, width, CvType.CV_8UC4);

                    //비트맵 -> 매트
                    Utils.bitmapToMat(photo, tmp);

                    //Canny 호출 : getNativeObjAddr = 메모리주소가져오기(포인터로 접근)
                    Canny(tmp.getNativeObjAddr(), edge.getNativeObjAddr(), binary.getNativeObjAddr(), gray.getNativeObjAddr());


                    //매트 -> 비트맵
                    Utils.matToBitmap(binary, photo);


                    //canvas에 bitmap뿌리기
                    drawLine.canvas.drawBitmap(photo, 0, -10, drawLine.paint);
                    Log.e("Canny", "Canny success");
                    break;
                }

            }
        }
    }



   /* //콜백함수(지금은안쓰임)
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i("MainActivity", "OpenCV loaded successfully");

                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };*/



    /** 권한 관련 **/
    private boolean checkPermissions() {
        int result;
        List<String> permissionList = new ArrayList<>();
        for (String pm : permissions) {

            result = ContextCompat.checkSelfPermission(this, pm);
            if (result != PackageManager.PERMISSION_GRANTED) { //사용자가 해당 권한을 가지고 있지 않을 경우 리스트에 해당 권한명 추가
                permissionList.add(pm);
            }

        }
        if (!permissionList.isEmpty()) { //권한이 추가되었으면 해당 리스트가 empty가 아니므로 request 즉 권한을 요청합니다.
            ActivityCompat.requestPermissions(this, permissionList.toArray(new String[permissionList.size()]), MULTIPLE_PERMISSIONS);
            return false;
        }
        else return true;

    }

    //아래는 권한 요청 Callback 함수입니다. PERMISSION_GRANTED로 권한을 획득했는지 확인할 수 있습니다. 아래에서는 !=를 사용했기에
    //권한 사용에 동의를 안했을 경우를 if문으로 코딩되었습니다.
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MULTIPLE_PERMISSIONS: {
                if (grantResults.length > 0) {
                    for (int i = 0; i < permissions.length; i++) {
                        if (permissions[i].equals(this.permissions[0])) {
                            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                                showNoPermissionToastAndFinish();
                            }
                        } else if (permissions[i].equals(this.permissions[1])) {
                            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                                showNoPermissionToastAndFinish();

                            }
                        } else if (permissions[i].equals(this.permissions[2])) {
                            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                                showNoPermissionToastAndFinish();

                            }
                        }
                    }
                } else {
                    showNoPermissionToastAndFinish();
                }
                return;
            }
        }
    }

    //권한 획득에 동의를 하지 않았을 경우 아래 Toast 메세지를 띄우며 해당 Activity를 종료시킵니다.
    private void showNoPermissionToastAndFinish() {
        Toast.makeText(this, "권한 요청에 동의 해주셔야 이용 가능합니다. 설정에서 권한 허용 하시기 바랍니다.", Toast.LENGTH_SHORT).show();
        finish();
    }
}

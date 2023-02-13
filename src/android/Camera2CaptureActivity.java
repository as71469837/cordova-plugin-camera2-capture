package huayu.cordova.plugin.camera2capture;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.Nullable;

import android.view.TextureView;
import android.view.WindowManager;
import android.widget.ImageView;

public class Camera2CaptureActivity extends Activity {
  public static Camera2VideoCaptureHelper camera2VideoCaptureHelper;
  private TextureView textureView;
  private ImageView image;
  private int Height;
  private  int Width;
  private  int Duration=15;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if(savedInstanceState!=null)
    {
//    configOption=  savedInstanceState.getSerializable("Camera2ConfigOption");
      int height= savedInstanceState.getInt("Camera2ConfigOption.Height");
      int Width=  savedInstanceState.getInt("Camera2ConfigOption.Width");
      int duration= savedInstanceState.getInt("Camera2ConfigOption.Duration");
      if(duration>0)
      {
        Duration=duration;
      }
    }
    getWindow().setFlags(
      WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
      WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);//硬件加速
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);//保持常亮
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);//全屏，包含系统状态栏

    int viewId=getResources().getIdentifier("camera2_capture_activity","layout",getPackageName());
    setContentView(viewId);
    // setContentView(R.layout.camera2_capture_activity);

    int viewContentId=getResources().getIdentifier("camera2_capture_container","id",getPackageName());
    textureView = findViewById(viewContentId);
    // textureView = findViewById(R.id.camera2_capture_container);
    camera2VideoCaptureHelper = new Camera2VideoCaptureHelper(this, textureView,getResources().getDisplayMetrics());

    initBrightness();
  }

  /**
   * 初始化屏幕亮度，不到200自动调整到200
   */
  private void initBrightness() {
    int brightness = BrightnessTools.getScreenBrightness(this);
    if (brightness < 200) {
      BrightnessTools.setBrightness(this, 200);
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();


    camera2VideoCaptureHelper.releaseThread();
  }
}

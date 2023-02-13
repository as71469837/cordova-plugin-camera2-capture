package huayu.cordova.plugin.camera2capture;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.RequiresApi;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Camera2CaptureFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Camera2CaptureFragment extends Fragment {

  private static final String Key_Height = "Camera2ConfigOption.Height";
  private static final String Key_Width = "Camera2ConfigOption.Width";
  private static final String Key_Duration = "Camera2ConfigOption.Duration";
  public  static int Durantion=15;
  public  static Context mAppContext;

  private String mHeightKey;
  private String mWidthKey;
  private String mDurationKey;

  private View mPageView;
  private String appResourcesPackage;

  public static Camera2VideoCaptureHelper camera2VideoCaptureHelper;
  private TextureView mTextureView;
  private ImageView image;
  private int mHeight;
  private int mWidth;
  private int mDuration = 0;
  private Activity mActivity;

  public Camera2CaptureFragment() {
    // Required empty public constructor
  }

  /**
   * Use this factory method to create a new instance of
   * this fragment using the provided parameters.
   *
   * @param height Parameter 1.
   * @param width Parameter 2.
   * @param duration Parameter 2.
   * @return A new instance of fragment Camera2CaptureFragment.
   */
  public static Camera2CaptureFragment newInstance(int height, int width ,int duration) {
    Camera2CaptureFragment fragment = new Camera2CaptureFragment();
    Bundle args = new Bundle();
    args.putInt(Key_Height, height);
    args.putInt(Key_Width, width);
    args.putInt(Key_Duration, duration);
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getArguments() != null) {
      mHeight = getArguments().getInt(Key_Height);
      mWidth = getArguments().getInt(Key_Width);
      int duration = getArguments().getInt(Key_Duration);
      if (duration > 0) {
        mDuration = duration;
      }
    }

  }

  @RequiresApi(api = Build.VERSION_CODES.M)
  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    appResourcesPackage = getActivity().getPackageName();
    // Inflate the layout for this fragment
    int pageViewId=getResources().getIdentifier("camera2_capture_activity","layout",appResourcesPackage);
    mPageView = inflater.inflate(pageViewId, container, false);
    // mPageView = inflater.inflate(R.layout.camera2_capture_fragment, container, false);
    mActivity = getActivity();
    if (mActivity != null) {
      mActivity.getWindow().setFlags(
        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);//硬件加速
      mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);//保持常亮
      mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);//全屏，包含系统状态栏
    }

    int textureViewId=getResources().getIdentifier("camera2_capture_container","id",appResourcesPackage);
    mTextureView = mPageView.findViewById(textureViewId);
    // mTextureView = mPageView.findViewById(R.id.camera2_capture_container);
    camera2VideoCaptureHelper = new Camera2VideoCaptureHelper(getActivity(), mTextureView, getResources().getDisplayMetrics(),mDuration);
    initBrightness();
    return mPageView;
  }


  /**
   * 初始化屏幕亮度，不到200自动调整到200
   */
  private void initBrightness() {
    if (mActivity == null) {
      mActivity = getActivity();
    }
    int brightness = BrightnessTools.getScreenBrightness(mActivity);
    if (brightness < 200) {
      BrightnessTools.setBrightness(mActivity, 200);
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    camera2VideoCaptureHelper.releaseThread();
  }
}

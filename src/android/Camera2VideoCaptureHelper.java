package huayu.cordova.plugin.camera2capture;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.Surface;
import android.view.TextureView;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class Camera2VideoCaptureHelper {

  private static final String TAG = Camera2VideoCaptureHelper.class.getSimpleName();
  private CameraManager mCameraManager;
  private CameraDevice mCameraDevice;
  private CameraCaptureSession mCameraCaptureSession;

  private CameraCharacteristics mCameraCharacteristics;
  private int mCameraSensorOrientation = 0; // 摄像头方向
  private int mCameraFacing = CameraCharacteristics.LENS_FACING_BACK; // 默认使用后置摄像头;
  private int mDisplayRotation; // 手机方向

  private boolean isRecordingVideo = false; // 是否正在录像
  private boolean canExchangeCamera = false; // 是否可以切换摄像头

  private Handler mCameraHandler;
  private HandlerThread handlerThread = new HandlerThread("Camera2Thread");

  private Size mPreviewSize = new Size(PREVIEW_WIDTH, PREVIEW_HEIGHT); // 预览大小
  private Size mSavePicSize = new Size(SAVE_WIDTH, SAVE_HEIGHT); // 保存图片大小

  private static final int PREVIEW_WIDTH = 720; // 预览的宽度
  private static final int PREVIEW_HEIGHT = 1280; // 预览的高度
  private static final int SAVE_WIDTH = 720; // 保存的宽度
  private static final int SAVE_HEIGHT = 1280; // 保存的高度

  private Activity mActivity;
  private TextureView mTextureView;

  private int screenWidth;

  private CameraDevice.StateCallback mCameraDeviceStateCallback;
  private CameraCaptureSession.StateCallback mSessionStateCallback;
  private CameraCaptureSession.CaptureCallback mSessionCaptureCallback;
  private CaptureRequest.Builder mRecorderCaptureRequest;
  private MediaRecorder mMediaRecorder;
  private String mCurrentCameraId;
  private Handler mChildHandler;
  private DisplayMetrics displayMetrics;
  private int mDuration=0; // 设置的持续时长（单位：秒）
  private Timer mTimer; // 定时器
  private int mCurrentDuration; // 定时器当前的持续时长
  private boolean timerOnRunning = false; // 定时器是否正在执行
  private File mCurrentFile;//当前录像保存到的文件信息

  public Camera2VideoCaptureHelper(Activity activity, TextureView textureView, DisplayMetrics displayMetrics) {
    this.mActivity = activity;
    this.mTextureView = textureView;
    Display display = mActivity.getWindowManager().getDefaultDisplay();
    mDisplayRotation = display.getRotation();
    Point outSize = new Point();
    display.getSize(outSize);
    screenWidth = outSize.x;// 得到屏幕的宽度
    this.displayMetrics = displayMetrics;
    init();
  }


  public Camera2VideoCaptureHelper(Activity activity, TextureView textureView, DisplayMetrics displayMetrics,int maxDuration) {
    this.mActivity = activity;
    this.mTextureView = textureView;
    Display display = mActivity.getWindowManager().getDefaultDisplay();
    mDisplayRotation = display.getRotation();
    Point outSize = new Point();
    display.getSize(outSize);
    screenWidth = outSize.x;// 得到屏幕的宽度
    this.displayMetrics = displayMetrics;
    init();
    if(maxDuration>0){
      mDuration=maxDuration;
    }
  }

  private void init() {
    initChildHandler();
    initTextureViewStateListener();
    initCameraDeviceStateCallback();

    initSessionStateCallback();
    initSessionCaptureCallback();
  }

  /**
   * 初始化TextureView的纹理生成监听，只有纹理生成准备好了。我们才能去进行摄像头的初始化工作让TextureView接收摄像头预览画面
   */
  private void initTextureViewStateListener() {
    mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
      @Override
      public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        // 可以使用纹理
        initCameraManager();
        selectCamera();
        openCamera();

      }

      @Override
      public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // 纹理尺寸变化

      }

      @Override
      public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        // //纹理被销毁
        // return false;
        releaseCamera();
        return true;
      }

      @Override
      public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // 纹理更新

      }
    });
  }

  /**
   * 初始化子线程Handler，操作Camera2需要一个子线程的Handler
   */
  private void initChildHandler() {
    handlerThread.start();
    mChildHandler = new Handler(handlerThread.getLooper());
  }

  /**
   * 初始化预览
   */

  private void initMediaRecorder() {
    if (mMediaRecorder != null) {
      mMediaRecorder.release();
      mMediaRecorder = null;
    }
    mMediaRecorder = new MediaRecorder();

    configMediaRecorder();

    try {
      Size cameraSize = getMatchingSize2();
      SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
      surfaceTexture.setDefaultBufferSize(cameraSize.getWidth(), cameraSize.getHeight());
      Surface previewSurface = new Surface(surfaceTexture);
      Surface recorderSurface = mMediaRecorder.getSurface();// 从获取录制视频需要的Surface

      mRecorderCaptureRequest = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_VIDEO_SNAPSHOT);

      mRecorderCaptureRequest.set(CaptureRequest.CONTROL_AF_MODE,
        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE); // 自动对焦
      mRecorderCaptureRequest.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH); // 闪光灯
      mRecorderCaptureRequest.addTarget(recorderSurface);
      mRecorderCaptureRequest.addTarget(previewSurface);

      // 创建CaptureSession会话。
      // 第一个参数 outputs 是一个 List 数组，相机会把捕捉到的图片数据传递给该参数中的 Surface 。
      // 第二个参数 StateCallback 是创建会话的状态回调。
      // 第三个参数描述了 StateCallback 被调用时所在的线程
      // 请注意这里设置了Arrays.asList(previewSurface,recorderSurface)
      // 2个Surface，很好理解录制视频也需要有画面预览，第一个是预览的Surface，第二个是录制视频使用的Surface
      mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, recorderSurface),mSessionStateCallback, mChildHandler);

    } catch (CameraAccessException e) {
      e.printStackTrace();
      Log.e(TAG,e.getMessage());
    }
  }

  /**
   * 初始化MediaRecorder
   */
  private void initCameraPreview() {
    try {
      Size cameraSize = getMatchingSize2();
      SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
      surfaceTexture.setDefaultBufferSize(cameraSize.getWidth(), cameraSize.getHeight());
      Surface previewSurface = new Surface(surfaceTexture);

      mRecorderCaptureRequest = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

      mRecorderCaptureRequest.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE); // 自动对焦
      mRecorderCaptureRequest.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH); // 闪光灯
      mRecorderCaptureRequest.addTarget(previewSurface);

      // 创建CaptureSession会话。
      // 第一个参数 outputs 是一个 List 数组，相机会把捕捉到的图片数据传递给该参数中的 Surface 。
      // 第二个参数 StateCallback 是创建会话的状态回调。
      // 第三个参数描述了 StateCallback 被调用时所在的线程
      // 请注意这里设置了Arrays.asList(previewSurface,recorderSurface)
      // 2个Surface，很好理解录制视频也需要有画面预览，第一个是预览的Surface，第二个是录制视频使用的Surface
      mCameraDevice.createCaptureSession(Arrays.asList(previewSurface), new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
          mCameraCaptureSession = session;
          try {
            // 执行重复获取数据请求，等于一直获取数据呈现预览画面，mSessionCaptureCallback会返回此次操作的信息回调
            mCameraCaptureSession.setRepeatingRequest(mRecorderCaptureRequest.build(), mSessionCaptureCallback,
              mChildHandler);
          } catch (CameraAccessException e) {
            e.printStackTrace();
          }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {

        }
      }, mChildHandler);

    } catch (CameraAccessException e) {
      e.printStackTrace();
      Log.e(TAG,e.getMessage());
    }
  }

  /**
   * 配置录制视频相关数据
   */
  private void configMediaRecorder() {

    mCurrentFile= Camera2Capture.Configuration.CreateFile(mActivity.getBaseContext(), ".mp4");

    mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);// 设置音频来源
    mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);// 设置视频来源
    mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);// 设置输出格式
    mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);// 设置音频编码格式，请注意这里使用默认，实际app项目需要考虑兼容问题，应该选择AAC
    mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);// 设置视频编码格式，请注意这里使用默认，实际app项目需要考虑兼容问题，应该选择H264
    mMediaRecorder.setVideoEncodingBitRate(8 * 1024 * 1920);// 设置比特率 一般是 1*分辨率 到 10*分辨率 之间波动。比特率越大视频越清晰但是视频文件也越大。
    mMediaRecorder.setVideoFrameRate(30);// 设置帧数 选择 30即可， 过大帧数也会让视频文件更大当然也会更流畅，但是没有多少实际提升。人眼极限也就30帧了。
    Size size = getMatchingSize2();
    mMediaRecorder.setVideoSize(size.getWidth(), size.getHeight());
    mMediaRecorder.setOrientationHint(90);
    Surface surface = new Surface(mTextureView.getSurfaceTexture());
    mMediaRecorder.setPreviewDisplay(surface);
    mMediaRecorder.setOutputFile(mCurrentFile.getAbsolutePath());
    try {
      mMediaRecorder.prepare();
    } catch (IOException e) {
      e.printStackTrace();
      Log.e(TAG,e.getMessage());
    }

  }


  /**
   * 开始录制视频
   */
  public void startRecorder() {
    if(!isRecordingVideo){
      isRecordingVideo=true;
      mCurrentFile=null;
      try{
        initMediaRecorder();
        if(mDuration>0){
          mCurrentDuration=0;
          timerOnRunning=true;
          TimerTask timerTask= new TimerTask() {
            @Override
            public void run() {
              if(mCurrentDuration<mDuration){
                mCurrentDuration++;
                Camera2Capture.CallJS(new Camera2CaptureChannelMessage("duration", true, mCurrentDuration));
              }else
              {
                Camera2CaptureChannelMessage stopMessage= stopRecorder();
                cancel();
                Camera2Capture.CallJS(stopMessage);
              }
            }
          };
          mTimer= new Timer("mScheduler");
          mTimer.schedule(timerTask,1000,1000);
        }
        mMediaRecorder.start();
        Camera2Capture.CallJS(new Camera2CaptureChannelMessage("start", true, "success"));
      }
      catch(Exception e){
        e.printStackTrace();
        Log.e(TAG,"开始录制视频时发现异常");
        Camera2Capture.CallJS(new Camera2CaptureChannelMessage("start", false, "开始录制视频时发现异常"));
      }
      return;
    }else{
      Camera2Capture.CallJS(new Camera2CaptureChannelMessage("start", false, "已经开始录制视频，无需重复操作"));
    }
  }

  /**
   * 暂停录制视频（暂停后视频文件会自动保存）
   */
  public Camera2CaptureChannelMessage stopRecorder() {
    if(mTimer!=null && timerOnRunning){
      mTimer.cancel();
      mTimer=null;
      mCurrentDuration=0;
      timerOnRunning=false;
    }
    isRecordingVideo=false;
    File videoFile=mCurrentFile;
    mCurrentFile=null;
    try {
      mMediaRecorder.stop();
    } catch (Exception e) {
      Log.e(TAG, "停止视频时发生异常", e);
      mMediaRecorder.reset();
      initCameraPreview();
      return new Camera2CaptureChannelMessage("stop", false, "停止时发生异常");
    }
    mMediaRecorder.reset();
    initCameraPreview();
    JSONObject mediaFile= Camera2Capture.GetMediaFileInfo(videoFile);
    return new Camera2CaptureChannelMessage("stop", true, mediaFile);
  }

  /**
   * 初始化Camera2的相机管理，CameraManager用于获取摄像头分辨率，摄像头方向，摄像头id与打开摄像头的工作
   */
  private void initCameraManager() {
    mCameraManager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
  }

  /**
   * 选择一颗我们需要使用的摄像头，主要是选择使用前摄还是后摄或者是外接摄像头
   */
  private void selectCamera() {
    if (mCameraManager == null) {
      Log.e(TAG, "selectCamera: CameraManager is null");
    }
    try {
      String[] cameraIdList = mCameraManager.getCameraIdList(); // 获取当前设备的全部摄像头id集合
      if (cameraIdList.length == 0) {
        Log.e(TAG, "selectCamera: cameraIdList length is 0");
      }
      for (String cameraId : cameraIdList) { // 遍历所有摄像头
        CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);// 得到当前id的摄像头描述特征
        Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING); // 获取摄像头的方向特征信息
        if (facing == mCameraFacing) { // 这里选择后摄像头
          mCurrentCameraId = cameraId;
          mCameraCharacteristics = characteristics;
          break;
        }
      }

    } catch (CameraAccessException e) {
      e.printStackTrace();
    }
  }

  /**
   * 切换摄像头
   */
  public void exchangeCamera() {
    if (mCameraDevice == null || !mTextureView.isAvailable()) {
      Log.e(TAG, "不能切换摄像头");
      return;
    }

    if (mCameraFacing == CameraCharacteristics.LENS_FACING_FRONT) {
      mCameraFacing = CameraCharacteristics.LENS_FACING_BACK;
    } else {
      mCameraFacing = CameraCharacteristics.LENS_FACING_FRONT;
    }
    mPreviewSize = new Size(PREVIEW_WIDTH, PREVIEW_HEIGHT); // 重置预览大小
    // mDisplayRotation =
    // mActivity.getWindowManager().getDefaultDisplay().getRotation();
    releaseCamera();

    initCameraDeviceStateCallback();
    initCameraManager();
    selectCamera();
    openCamera();
  }

  public void releaseCamera() {
    if (mCameraCaptureSession != null) {
      mCameraCaptureSession.close();
      mCameraCaptureSession = null;
    }

    if (mCameraDevice != null) {
      mCameraDevice.close();
      mCameraDevice = null;
    }

    canExchangeCamera = false;
  }

  private void initCameraDeviceStateCallback() {
    mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
      @Override
      // 摄像头被打开
      public void onOpened(@NonNull CameraDevice camera) {
        mCameraDevice = camera;
        initCameraPreview();
      }

      @Override
      public void onDisconnected(@NonNull CameraDevice camera) {
        // 摄像头断开
        Log.e(TAG, "摄像头断开");
      }

      @Override
      public void onError(@NonNull CameraDevice camera, int error) {
        // 异常
        String message = "设备异常";
        switch (error) {
          case ERROR_CAMERA_DEVICE:
            message = "Fatal (device)";
            break;
          case ERROR_CAMERA_DISABLED:
            message = "Device policy";
            break;
          case ERROR_CAMERA_IN_USE:
            message = "Camera in use";
            break;
          case ERROR_CAMERA_SERVICE:
            message = "Fatal (service)";
            break;
          case ERROR_MAX_CAMERAS_IN_USE:
            message = "Maximum cameras in use";
            break;
          default:
            break;
        }
        Log.e(TAG, message);
        Camera2Capture.CallJS(new Camera2CaptureChannelMessage("CameraDeviceState", false, message));
      }
    };
  }

  private void initSessionStateCallback() {
    mSessionStateCallback = new CameraCaptureSession.StateCallback() {
      @Override
      public void onConfigured(@NonNull CameraCaptureSession session) {
        mCameraCaptureSession = session;
        try {
          // 执行重复获取数据请求，等于一直获取数据呈现预览画面，mSessionCaptureCallback会返回此次操作的信息回调
          mCameraCaptureSession.setRepeatingRequest(mRecorderCaptureRequest.build(), mSessionCaptureCallback,
            mChildHandler);
        } catch (CameraAccessException e) {
          e.printStackTrace();
        }
      }

      @Override
      public void onConfigureFailed(@NonNull CameraCaptureSession session) {

      }
    };
  }

  private void initSessionCaptureCallback() {
    mSessionCaptureCallback = new CameraCaptureSession.CaptureCallback() {
      @Override
      public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                                   long timestamp, long frameNumber) {
        super.onCaptureStarted(session, request, timestamp, frameNumber);
      }

      @Override
      public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                                      @NonNull CaptureResult partialResult) {
        super.onCaptureProgressed(session, request, partialResult);
      }

      @Override
      public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                                     @NonNull TotalCaptureResult result) {
        super.onCaptureCompleted(session, request, result);
        canExchangeCamera = true;
      }

      @Override
      public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                                  @NonNull CaptureFailure failure) {
        super.onCaptureFailed(session, request, failure);
      }
    };
  }

  /**
   * 打开摄像头，这里打开摄像头后，我们需要等待mCameraDeviceStateCallback的回调
   */
  @SuppressLint("MissingPermission")
  private void openCamera() {
    try {
      mCameraManager.openCamera(mCurrentCameraId, mCameraDeviceStateCallback, mChildHandler);
    } catch (CameraAccessException e) {
      e.printStackTrace();
      Log.e(TAG,e.getMessage());
    }
  }

  /**
   * 计算需要的使用的摄像头分辨率
   *
   * @return
   */
  private Size getMatchingSize2() {
    Size selectSize = null;
    try {
      CameraCharacteristics cameraCharacteristics = mCameraManager.getCameraCharacteristics(mCurrentCameraId);
      StreamConfigurationMap streamConfigurationMap = cameraCharacteristics
        .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
      Size[] sizes = streamConfigurationMap.getOutputSizes(ImageFormat.JPEG);
      // 因为我这里是将预览铺满屏幕,所以直接获取屏幕分辨率
      int deviceWidth = displayMetrics.widthPixels; // 屏幕分辨率宽
      int deviceHeigh = displayMetrics.heightPixels; // 屏幕分辨率高
      Log.i(TAG, "getMatchingSize2: 屏幕密度宽度=" + deviceWidth);
      Log.i(TAG, "getMatchingSize2: 屏幕密度高度=" + deviceHeigh);
      /**
       * 循环40次,让宽度范围从最小逐步增加,找到最符合屏幕宽度的分辨率,
       * 你要是不放心那就增加循环,肯定会找到一个分辨率,不会出现此方法返回一个null的Size的情况
       * ,但是循环越大后获取的分辨率就越不匹配
       */
      for (int j = 1; j < 41; j++) {
        for (int i = 0; i < sizes.length; i++) { // 遍历所有Size
          Size itemSize = sizes[i];
          Log.i(TAG, "当前itemSize 宽=" + itemSize.getWidth() + "高=" + itemSize.getHeight());
          // 判断当前Size高度小于屏幕宽度+j*5 && 判断当前Size高度大于屏幕宽度-j*5 && 判断当前Size宽度小于当前屏幕高度
          if (itemSize.getHeight() < (deviceWidth + j * 5) && itemSize.getHeight() > (deviceWidth - j * 5)) {
            if (selectSize != null) { // 如果之前已经找到一个匹配的宽度
              if (Math.abs(deviceHeigh - itemSize.getWidth()) < Math.abs(deviceHeigh - selectSize.getWidth())) { // 求绝对值算出最接近设备高度的尺寸
                selectSize = itemSize;
                continue;
              }
            } else {
              selectSize = itemSize;
            }

          }
        }
        if (selectSize != null) { // 如果不等于null 说明已经找到了 跳出循环
          break;
        }
      }
    } catch (CameraAccessException e) {
      e.printStackTrace();
      Log.e(TAG,e.getMessage());
    }
    Log.i(TAG, "getMatchingSize2: 选择的分辨率宽度=" + selectSize.getWidth());
    Log.i(TAG, "getMatchingSize2: 选择的分辨率高度=" + selectSize.getHeight());
    return selectSize;
  }

  public void releaseThread() {
    handlerThread.quitSafely();
  }

}

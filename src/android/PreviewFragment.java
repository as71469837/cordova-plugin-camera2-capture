package huayu.cordova.plugin.camera2capture;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.ionic.starter.R;
// 用来实现相机预览
public class PreviewFragment extends Fragment{
    private final String TAG = "PreviewFragment";
    private Context mContext;
    private String appResourcesPackage;
    private View pageView;
  
     public  PreviewFragment(Context context){
       mContext=context;
     }
  
  
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      appResourcesPackage = getActivity().getPackageName();
      pageView = inflater.inflate(R.layout.preview_fragment, container, false); 
      //inflater.inflate(getResources().getIdentifier("activity_push", "layout", appResourcesPackage), container, false);
  
  
      return  pageView;
    }
  }
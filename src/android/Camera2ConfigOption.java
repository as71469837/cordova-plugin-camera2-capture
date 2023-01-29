package huayu.cordova.plugin.camera2capture;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;

import android.os.Bundle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Camera2ConfigOption {

    public int Height;

    public int Width;

    public int Duration=15;


    public CallbackContext callbackContext;

    public Camera2ConfigOption(JSONObject options, CallbackContext callbackContext)throws JSONException {
        this.callbackContext= callbackContext;
        if (options != null) {
            Height = options.optInt("height", 1);
            Width = options.optInt("with", 1);
            Duration = options.optInt("duration", 0);
        }
    }


    public  Camera2ConfigOption(int height,int width,int duration, CallbackContext callbackContext){
      this.Height = height;
      this.Width = width;
      this.Duration = duration;
      this.callbackContext= callbackContext;
    }
}


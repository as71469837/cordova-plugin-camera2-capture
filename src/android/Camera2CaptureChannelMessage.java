package huayu.cordova.plugin.camera2capture;

import org.json.JSONException;
import org.json.JSONObject;

public final class Camera2CaptureChannelMessage<T> {
  public String Action;

  public boolean IsSuccess=false;

  public T Message= null ;

  public Camera2CaptureChannelMessage(String action, boolean isSuccess) {
    Action = action;
    IsSuccess = isSuccess;
  }

  public Camera2CaptureChannelMessage(String action, boolean isSuccess, T message) {
    Action = action;
    IsSuccess = isSuccess;
    Message = message;
  }


  public JSONObject ConvertToJson() throws JSONException {
    JSONObject jsonObject=new JSONObject();
    jsonObject.put("action",this.Action);
    jsonObject.put("isSuccess",this.IsSuccess);
    jsonObject.put("message",this.Message);
    return  jsonObject;
  }
}


# cordova-plugin-camera2-capture
cordova-plugin-camera2-capture
基于Camera2创建的视频拍摄插件，暂时只支持*** Android ***。

第一次使用时，需camera2capture.init(height,width,duration,function(),function())进行初始化。
duration表示录制视频的最大时长，如果为0，则表示没有限制。
```
    camera2capture.init(400, 400, 10, (s: any) => {
        ////以下代码可实现html界面显示在java界面上；
        //const bodyEl: any = window.document.querySelector('body');
        //bodyEl.style.visibility = 'hidden';
        //bodyEl.style.background = 'transparent';
      console.log("Video2Capture init Success");
    }, (f: any) => {
      console.error("Video2Capture init Fail");
    })
```

# recordVideo方法，开始录制视频；

```
camera2capture.recordVideo((s: any) => {
        console.log(s);
        console.log(typeof s);
        if (s.action == "start") {
          //自己的业务逻辑
        } else if (s.action == "duration") {
            //自己的业务逻辑
        } else if (s.action == "stop") {          
            //自己的业务逻辑
        }
      }, (f: any) => {
        console.log("RecordVideo Fail:" + f.message);
        alert("RecordVideo Fail");
      });
```
# endVideoCapture方法，手动停止录制视频；

```
camera2capture.endVideoCapture((s: any) => {
        console.log(s);
        console.log(typeof s);
         //自己的业务逻辑
      console.log("StopCaptureVideo Success");
    }, (f: any) => {
      console.log("StopCaptureVideo Fail:"+f.message);
    });
```

package dd.test.videocallapp

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas
import io.agora.rtc.video.VideoEncoderConfiguration
import kotlinx.android.synthetic.main.activity_main.*

class VideoCallActivity : AppCompatActivity() {

    private val TAG: String = VideoCallActivity::class.java.simpleName

    private var mRtcEngine: RtcEngine? = null
    private var mMuted = false
    private var mRemoteUserAlreadyConnected=false

    private var mLocalView: SurfaceView?=null
    private var mRemoteView: SurfaceView?=null

    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsedTime: Int) {
//            runOnUiThread {
//                showLongToast("Join channel success, uid: $uid")}
        }

        /**
         * Occurs when the first remote video frame is received and decoded.
         * This callback is triggered in either of the following scenarios:
         *
         * The remote user joins the channel and sends the video stream.
         * The remote user stops sending the video stream and re-sends it after 15 seconds. Possible reasons include:
         * The remote user leaves channel.
         * The remote user drops offline.
         * The remote user calls the muteLocalVideoStream method.
         * The remote user calls the disableVideo method.
         *
         * @param uid User ID of the remote user sending the video streams.
         * @param width Width (pixels) of the video stream.
         * @param height Height (pixels) of the video stream.
         * @param elapsed Time elapsed (ms) from the local user calling the joinChannel method until this callback is triggered.
         */
        override fun onFirstRemoteVideoDecoded(
            uid: Int,
            width: Int,
            height: Int,
            elapsedTime: Int
        ) {
            //272964
            runOnUiThread {
                if(elapsedTime/1000<=15 || mRemoteUserAlreadyConnected)
                    setupRemoteVideo(uid)
                else
                {
                    endCall()
                    showLongToast("Please try again after some time")
                    finish()
                }

            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread {
//                mLogView.logI("User offline, uid: " + (uid and 0xFFFFFFFFL))
                showLongToast("Offline reason code -> $reason")
                onRemoteUserLeft()
            }
        }
    }

    private fun setupRemoteVideo(uid: Int) {
        mRemoteUserAlreadyConnected=true
        // Only one remote video view is available for this
        // tutorial. Here we check if there exists a surface
        // view tagged as this uid.

        remote_video_view_container.visibility=View.VISIBLE

        val count = remote_video_view_container!!.childCount
        var view: View? = null
        for (i in 0 until count) {
            val v = remote_video_view_container.getChildAt(i)
            if (v.tag is Int && v.tag as Int == uid) {
                view = v
            }
        }
        if (view != null) {
            return
        }

        /*
          Creates the video renderer view.
          CreateRendererView returns the SurfaceView type. The operation and layout of the view
          are managed by the app, and the Agora SDK renders the view provided by the app.
          The video display view must be created using this method instead of directly
          calling SurfaceView.
         */mRemoteView = RtcEngine.CreateRendererView(baseContext)
        remote_video_view_container.addView(mRemoteView)
        // Initializes the video view of a remote user.
        mRtcEngine!!.setupRemoteVideo(VideoCanvas(mRemoteView, VideoCanvas.RENDER_MODE_HIDDEN, uid))
        mRemoteView?.tag = uid
    }

    private fun onRemoteUserLeft() {
        removeRemoteVideo()
    }

    private fun removeRemoteVideo() {
        remote_video_view_container.visibility=View.GONE
        if (mRemoteView != null) {
            remote_video_view_container!!.removeView(mRemoteView)
        }
        // Destroys remote view
        mRemoteView = null

        showLongToast("Another user has left the channel")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        initEngineAndJoinChannel()
    }

    override fun onResume() {
        super.onResume()
        mRtcEngine?.enableAudio()
        mRtcEngine?.enableVideo()
    }

    override fun onPause() {
        super.onPause()
        mRtcEngine?.disableAudio()
        mRtcEngine?.disableVideo()
    }

    private fun showLongToast(msg: String) {
        runOnUiThread { Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show() }
    }

    override fun onBackPressed() {
//        super.onBackPressed()
    }

    private fun initEngineAndJoinChannel() {
        // This is our usual steps for joining
        // a channel and starting a call.
        initializeEngine()
        setupVideoConfig()
        setupLocalVideo()
        joinChannel()
    }

    private fun initializeEngine() {
        mRtcEngine = try {
            RtcEngine.create(
                baseContext,
                getString(R.string.agora_app_id),
                mRtcEventHandler
            )
        } catch (e: Exception) {
            Log.e(
                TAG,Log.getStackTraceString(e)
            )
            throw RuntimeException(
                "NEED TO check rtc sdk init fatal error\n" + Log.getStackTraceString(
                    e
                )
            )
        }
    }

    private fun setupVideoConfig() {
        // In simple use cases, we only need to enable video capturing
        // and rendering once at the initialization step.
        // Note: audio recording and playing is enabled by default.

        mRtcEngine!!.setVideoEncoderConfiguration(
            VideoEncoderConfiguration(
                VideoEncoderConfiguration.VD_640x360,
                VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                VideoEncoderConfiguration.STANDARD_BITRATE,
                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT
            )
        )
    }

    private fun setupLocalVideo() {
        // This is used to set a local preview.
        // The steps setting local and remote view are very similar.
        // But note that if the local user do not have a uid or do
        // not care what the uid is, he can set his uid as ZERO.
        // Our server will assign one and return the uid via the event
        // handler callback function (onJoinChannelSuccess) after
        // joining the channel successfully.
        mLocalView = RtcEngine.CreateRendererView(baseContext)
        mLocalView?.setZOrderMediaOverlay(true)
        local_video_view_container!!.addView(mLocalView)
        // Initializes the local video view.
        // RENDER_MODE_HIDDEN: Uniformly scale the video until it fills the visible boundaries. One dimension of the video may have clipped contents.
        mRtcEngine!!.setupLocalVideo(VideoCanvas(mLocalView, VideoCanvas.RENDER_MODE_HIDDEN, 0))
    }

    private fun joinChannel() {
        // 1. Users can only see each other after they join the
        // same channel successfully using the same app id.
        // 2. One token is only valid for the channel name that
        // you use to generate this token.
        var token: String? = getString(R.string.agora_access_token)
        if (TextUtils.isEmpty(token) || TextUtils.equals(token, "#YOUR ACCESS TOKEN#")) {
            token = null // default, no token
        }
        mRtcEngine!!.joinChannel(token, "ertyuioDF6789", "Extra Optional Data", 0)
    }

    override fun onDestroy() {
        super.onDestroy()
        leaveChannel()
        /*
          Destroys the RtcEngine instance and releases all resources used by the Agora SDK.

          This method is useful for apps that occasionally make voice or video calls,
          to free up resources for other operations when not making calls.
         */RtcEngine.destroy()
    }

    private fun leaveChannel() {
        mRtcEngine!!.leaveChannel()
    }

    fun onLocalAudioMuteClicked(view: View?) {
        mMuted = !mMuted
        // Stops/Resumes sending the local audio stream.
        mRtcEngine!!.muteLocalAudioStream(mMuted)
        val res = if (mMuted) R.drawable.btn_mute else R.drawable.btn_unmute
        btn_mute!!.setImageResource(res)
    }

    fun onSwitchCameraClicked(view: View?) {
        // Switches between front and rear cameras.
        mRtcEngine!!.switchCamera()
    }

    fun onEndCallClicked(view: View?) {
        endCall()
        finish()
    }

    private fun endCall() {
        removeLocalVideo()
        removeRemoteVideo()
        leaveChannel()
    }

    private fun removeLocalVideo() {
        if (mLocalView != null) {
            local_video_view_container!!.removeView(mLocalView)
        }
        mLocalView = null
    }
}
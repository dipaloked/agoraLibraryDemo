package dd.test.videocallapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.activity_home.adView

class HomeActivity : AppCompatActivity() {

    private val PERMISSION_REQ_ID = 22

    // Permission WRITE_EXTERNAL_STORAGE is not mandatory
    // for Agora RTC SDK, just in case if you wanna save
    // logs to external sdcard.
    private val REQUESTED_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        //init AdMob
        MobileAds.initialize(
            this){

        }

        val adRequest =
            AdRequest.Builder().build()
        adView.loadAd(adRequest)

        btn_start.setOnClickListener {
            // Ask for permissions at runtime.
            // This is just an example set of permissions. Other permissions
            // may be needed, and please refer to our online documents.
            if (checkSelfPermission(
                    REQUESTED_PERMISSIONS[0], PERMISSION_REQ_ID
                ) &&
                checkSelfPermission(
                    REQUESTED_PERMISSIONS[1], PERMISSION_REQ_ID
                ) &&
                checkSelfPermission(
                    REQUESTED_PERMISSIONS[2], PERMISSION_REQ_ID
                )
            ) {
                startActivity(Intent(this,VideoCallActivity::class.java))
            }
        }
    }

    // Called when leaving the activity
    public override fun onPause() {
        adView.pause()
        super.onPause()
    }

    // Called when returning to the activity
    public override fun onResume() {
        super.onResume()
        adView.resume()
    }

    override fun onDestroy() {
        adView.destroy()
        super.onDestroy()
    }

    private fun checkSelfPermission(
        permission: String,
        requestCode: Int
    ): Boolean {
        if (ContextCompat.checkSelfPermission(this, permission) !==
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, REQUESTED_PERMISSIONS,
                requestCode
            )
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_REQ_ID) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED || grantResults[1] != PackageManager.PERMISSION_GRANTED || grantResults[2] != PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(applicationContext, "Need permissions " + Manifest.permission.RECORD_AUDIO +
                        "/" + Manifest.permission.CAMERA + "/" + Manifest.permission.WRITE_EXTERNAL_STORAGE, Toast.LENGTH_LONG).show()
                return
            }

            startActivity(Intent(this,VideoCallActivity::class.java))
        }
        else
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
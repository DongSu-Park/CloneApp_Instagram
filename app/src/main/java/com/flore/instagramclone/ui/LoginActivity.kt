package com.flore.instagramclone.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.facebook.CallbackManager
import com.flore.instagramclone.viewModel.LoginViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import androidx.lifecycle.Observer
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.flore.instagramclone.R
import com.flore.instagramclone.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private val viewModelLogin: LoginViewModel by viewModels()

    var auth: FirebaseAuth? = null // 파이어베이스 Auth
    private var callbackManager: CallbackManager? = null // 페이스북 계정 Auth

    companion object {
        const val TAG = "LoginActivity"
        const val GOOGLE_LOGIN_CODE = 9001
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart()")
        intentMainPage(auth?.currentUser)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding =
            DataBindingUtil.setContentView<ActivityLoginBinding>(this, R.layout.activity_login)
        binding.lifecycleOwner = this
        binding.loginViewModel = viewModelLogin
        viewModelLogin.loginActivity = this

        callbackManager = CallbackManager.Factory.create() // 페이스북 로그인 초기화
        auth = FirebaseAuth.getInstance() // 파이어베이스 로그인 기능 초기화
        Log.d(TAG, "onCreate()")

        // LoginViewModel Observer
        viewModelLogin.run {
            // 로그인 성공 후 메인엑티비티로 이동
            liveFirebaseUser.observe(this@LoginActivity, Observer {
                // user 인자가 존재할 경우
                if (it != null) {
                    Log.d(TAG, "Firebase Auth Login is Complete")
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            })

            // 구글 로그인 전송 (구글 로그인 화면 인텐트 이동)
            liveGoogleSignInIntent.observe(this@LoginActivity, Observer {
                if (it != null) {
                    Log.d(TAG, "StartActivity Call for Google Auth Login")
                    startActivityForResult(it, GOOGLE_LOGIN_CODE)
                }
            })

            // 페이스북 로그인 시작
            showFacebookAuth.observe(this@LoginActivity, Observer {
                if (it == true) {
                    viewModelLogin.showFacebookAuth.value = false

                    LoginManager.getInstance()
                        .logInWithReadPermissions(
                            this@LoginActivity,
                            listOf("public_profile", "email")
                        )

                    LoginManager.getInstance()
                        .registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
                            override fun onSuccess(result: LoginResult?) { // 페이스북 로그인 성공 시
                                Log.d(TAG, "facebook:onSuccess:$result")
                                viewModelLogin.handleFacebookAccessToken(result?.accessToken) // 페이스북 토큰 값 넘겨주기
                            }

                            override fun onCancel() {
                                Log.d(TAG, "facebook:onCancel")
                            }

                            override fun onError(error: FacebookException?) {
                                Log.d(TAG, "facebook:onError", error)
                            }
                        })
                }
            })

            // 로그인 실패 메세지
            showToastError.observe(this@LoginActivity, Observer {
                if (it == true) {
                    Log.d(TAG, "Login Error : ${viewModelLogin.showErrorMessage}")
                    Toast.makeText(
                        this@LoginActivity,
                        "로그인에 실패하였습니다 : ${viewModelLogin.showErrorMessage}",
                        Toast.LENGTH_LONG
                    ).show()

                    viewModelLogin.showToastError.value = false
                }
            })

            // 빈 문자열 오류 토스트 메세지
            showToastBlankAuth.observe(this@LoginActivity, Observer {
                if (it == true) {
                    Log.d(TAG, "Login Error : Email or Password is Empty")
                    Toast.makeText(
                        this@LoginActivity,
                        "Email or Password is Empty. \nPlease check the Email or Password",
                        Toast.LENGTH_LONG
                    ).show()
                    showToastBlankAuth.value = false
                }
            })
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager?.onActivityResult(requestCode, resultCode, data)

        if (requestCode == GOOGLE_LOGIN_CODE) {
            Log.d(TAG, "ActivityResult for Google Auth Login")
            viewModelLogin.googleLoginResult(data!!)
        }
    }

    private fun intentMainPage(user: FirebaseUser?) {
        if (user != null) { // user 인자가 존재할 경우
            Log.d(TAG, "기존 유저 로그인")
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }


}
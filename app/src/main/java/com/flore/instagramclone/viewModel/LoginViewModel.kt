package com.flore.instagramclone.viewModel

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.facebook.AccessToken
import com.flore.instagramclone.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    var auth: FirebaseAuth? = null // 파이어베이스 Auth
    var googleSignInClient: GoogleSignInClient? = null // 구글 계정 Auth
    var loginActivity : Activity? = null

    val liveFirebaseUser = MutableLiveData<FirebaseUser>()
    val liveGoogleSignInIntent = MutableLiveData<Intent>()

    var showFacebookAuth = MutableLiveData(false)
    var showToastError = MutableLiveData(false)
    var showToastBlankAuth = MutableLiveData(false)
    var showErrorMessage: String? = null

    var email : String? = ""
    var password : String? = ""

    private var viewContext = application
    private val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(viewContext.getString(R.string.default_web_client_id))
        .requestEmail()
        .build()

    init {
        auth = FirebaseAuth.getInstance() // 파이어베이스 로그인 기능 초기화
        googleSignInClient = GoogleSignIn.getClient(viewContext, gso); // 구글 로그인 기능 초기화
    }

    companion object {
        const val TAG = "LoginViewModel"
    }

    // 이메일 로그인 기능 실행 (데이터 바인딩으로 호출 변경)
    fun signinAndSignup(email: String, password: String) { // 첫 사용자인 경우 회원가입을 하고 로그인을 함
        if (email == "" || password == "") {
            // 빈 문자열 오류 토스트 메세지
            showToastBlankAuth.value = true
            return
        } else {
            auth?.createUserWithEmailAndPassword(email, password)
                ?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // 계정이 생성 되었을 경우
                        liveFirebaseUser.value = task.result?.user
                    } else if (task.exception?.message.isNullOrEmpty()) {
                        // 로그인 에러가 나왔을 경우 에러 메세지 표시
                        showToastError.value = true
                        showErrorMessage = task.exception?.message
                    } else {
                        signinEmail(email, password)
                    }
                }
        }
    }

    // 기존 이메일 로그인
    fun signinEmail(email : String, password: String) { // 기존 사용자인 경우 로그인을 함
        auth?.signInWithEmailAndPassword(email, password)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // 계정이 있으면 로그인 함
                    liveFirebaseUser.value = task.result?.user
                } else {
                    // 계정이 없을 경우 (이메일, 페스워드 틀릴 경우) 에러메세지 표시
                    showToastError.value = true
                    showErrorMessage = task.exception?.message
                }
            }
    }

    // 구글 로그인 기능 실행 (데이터 바인딩으로 호출 변경)
    fun googleLogin() { // 구글 로그인 기능 실행
        var signInIntent = googleSignInClient?.signInIntent
        liveGoogleSignInIntent.value = signInIntent
    }

    // 구글 로그인 결과
    fun googleLoginResult(data: Intent) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            var account = task.getResult(ApiException::class.java)!!
            Log.d(TAG, "firebaseAuthWithGoogle:" + account.id) // account.id 에는 구글 로그인 결과값
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            Log.w(TAG, "Google sign in failed", e)
        }
    }
    // 파이어베이스 Auth 로 google 로그인 실행
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null) // auth 값 가져옴
        auth?.signInWithCredential(credential) // 파이어베이스 auth 설정
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // 계정이 있으면 로그인 함
                    liveFirebaseUser.value = task.result?.user
                } else {
                    // 계정이 없을 경우 (이메일, 페스워드 틀릴 경우) 에러메세지 표시
                    showToastError.value = true
                    showErrorMessage = task.exception?.message
                }
            }
    }

    // 페이스북 로그인 기능 실행 (데이터 바인딩으로 호출 변경)
    fun facebookLogin() {
        showFacebookAuth.value = true
    }

    fun handleFacebookAccessToken(accessToken: AccessToken?) { // 페이스북 로그인 토큰 값이 오면 실행
        val credential = FacebookAuthProvider.getCredential(accessToken?.token!!)
        auth?.signInWithCredential(credential)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // 계정이 있으면 로그인 함
                    liveFirebaseUser.value = task.result?.user

                } else {
                    // 계정이 없을 경우 (이메일, 페스워드 틀릴 경우) 에러메세지 표시
                    showToastError.value = true
                    showErrorMessage = task.exception?.message
                }
            }
    }

}
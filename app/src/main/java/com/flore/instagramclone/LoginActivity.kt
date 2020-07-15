package com.flore.instagramclone

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {
    var auth: FirebaseAuth? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        auth = FirebaseAuth.getInstance()

        btn_login_email.setOnClickListener {
            signinAndSignup()
        }
    }

    fun signinAndSignup() { // 첫 사용자인 경우 회원가입을 하고 로그인을 함
        auth?.createUserWithEmailAndPassword(et_email.text.toString(), et_password.text.toString())
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // 계정이 생성 되었을 경우
                    intentMainpage(task.result?.user)
                } else if (!task.exception?.message.isNullOrEmpty()) {
                    // 로그인 에러가 나왔을 경우 에러 메세지 표시
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
                } else {
                    signinEmail()
                }
            }
    }

    fun signinEmail() { // 기존 사용자인 경우 로그인을 함
        auth?.signInWithEmailAndPassword(et_email.text.toString(), et_password.text.toString())
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // 계정이 있으면 로그인 함

                } else {
                    // 계정이 없을 경우 (이메일, 페스워드 틀릴 경우) 에러메세지 표시
                }
            }
    }

    fun intentMainpage(user: FirebaseUser?) {
        if (user != null) { // user 인자가 존재할 경우
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
}
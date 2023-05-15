package com.example.myapplication

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.util.concurrent.TimeUnit

class AuthActivity : AppCompatActivity() {


    private lateinit var signInBtn : AppCompatButton
    private lateinit var phoneInput : EditText
    private lateinit var goToRegister : TextView
    private var verificationId = ""//надо было подлючить SSH ключ
    lateinit var mAuth: FirebaseAuth //отвечает за firebase аунтификацию
    lateinit var builder: AlertDialog


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

//проверяю есть ли авторизованный пользователь, если он не пустой(!=null), то запускаем сразу MainActivity
        var firebaseAuth = FirebaseAuth.getInstance()
        if (firebaseAuth.currentUser != null){
            startActivity(Intent(this, MainActivity::class.java))
            finish() //закрывает переход из Auth.act в Main.act при нажатии на кнопку назад
        }

        //Если он пустой, выполняется код ниже
        init() //Вызываем функцию

        signInBtn.setOnClickListener { //Оформляем переход
            if (!Patterns.PHONE.matcher(phoneInput.text.toString()).matches()){ //Проевряем что бы,было только цифры
                phoneInput.setError("Enter phone number")
            }else{
                authUser(phoneInput.text.toString())
            }
        }

        goToRegister.setOnClickListener { //Переход из одного окна в другое
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
            finish()
        }

    }

    private fun authUser(phone : String) {
        FirebaseDatabase.getInstance().getReference("users")
            .child(phone)
            .addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()){
                        showOTPDialog()
                    }else{
                        Toast.makeText(this@AuthActivity, "Пользователя с таким номером не существует", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })
    }

    private fun showOTPDialog() { //Создает диалоговое окно


        val view = layoutInflater.inflate(R.layout.otp_dialog, null)
        val otpInput: EditText = view.findViewById(R.id.otp_input)
        val closeBtn: AppCompatButton = view.findViewById(R.id.close_dialog)
        val enterCode: AppCompatButton = view.findViewById(R.id.enter_code)
        builder.setView(view)


        val mCallBack = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() { //отвечает за отправку смс
            // при вызове нашего диалога(до его создание) отпрвляем сообщение
            override fun onVerificationCompleted(phone: PhoneAuthCredential) { //если код появился
                var code: String? = phone.smsCode
                if (code != null) {
                    otpInput.setText(code) //передаем код(текстовое поле, сюда приходит код)
                }
            }

            override fun onVerificationFailed(p0: FirebaseException) { //если код не появился
                Toast.makeText(this@AuthActivity, p0.message, Toast.LENGTH_SHORT).show()
                Log.e("err", p0.message.toString())
            }

            override fun onCodeSent(s: String, p1: PhoneAuthProvider.ForceResendingToken) { //отвечает за отправку смс
                super.onCodeSent(s, p1)
                verificationId = s
            }

        }

        val options = PhoneAuthOptions.newBuilder(mAuth)
            .setPhoneNumber(phoneInput.text.toString())
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(mCallBack) //отвечает за отправку кода, который написан выше
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)



        closeBtn.setOnClickListener {//пищем, что будет просиходить после нажатия
            builder.dismiss() //закрытие диалогового окна
        }
        enterCode.setOnClickListener {
            val code = otpInput.text.toString()
            if (TextUtils.isEmpty(code)) {
                otpInput.setError("Enter code")
            } else {
                verifyCode(code)
            }

        }

        builder.setCanceledOnTouchOutside(false) //если мимо окна нажали, то оно не закроется
        builder.show()
    }

    private fun verifyCode(code: String) { //создаем ф-ию для вертификации пользователя(сравниваем код, если все нормальното выполниться verificationId, code
        var credential: PhoneAuthCredential = PhoneAuthProvider.getCredential(verificationId, code) //credential-токен(нужен для firebase)
        signInWithCreditional(credential)
    }

    private fun signInWithCreditional(credential: PhoneAuthCredential) {
        var firebaseAuth = FirebaseAuth.getInstance()
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener { //метод, если все хорошо пошло, появляется сообщение успешно
                if (it.isSuccessful) {
                    Toast.makeText(this, "Успешно", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                }
            }
            .addOnCanceledListener {
                builder.dismiss()
            }
            .addOnFailureListener {
                Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            }
        }


    private fun init() { //инцидизируем переменные
        signInBtn = findViewById(R.id.sign_in_button)
        phoneInput = findViewById(R.id.phone_input)
        goToRegister = findViewById(R.id.go_to_register)
        mAuth = FirebaseAuth.getInstance()
        builder = AlertDialog.Builder(this).create()
    }

}
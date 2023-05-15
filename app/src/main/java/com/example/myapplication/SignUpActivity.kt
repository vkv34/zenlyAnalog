package com.example.myapplication

import android.app.AlertDialog
import android.app.Instrumentation.ActivityResult
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import android.widget.DatePicker
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView
import java.util.concurrent.TimeUnit

class SignUpActivity : AppCompatActivity() {

//все перменные, которые отображаются на пользовательском интерфейсе
    lateinit var backBtn: ImageButton
    lateinit var imageUser: CircleImageView
    lateinit var dobInput: DatePicker
    lateinit var nicknameInput: EditText
    lateinit var phoneInput: EditText
    lateinit var signUpBtn: LinearLayout
    var dobOfUser = ""
    private var verificationId = ""//надо было подлючить SSH ключ

    lateinit var mAuth: FirebaseAuth //отвечает за firebase аунтификацию

    private var uriImg: Uri? = null //путь к картинке

    private val selectImage = registerForActivityResult(ActivityResultContracts.GetContent()) {
        //отвечает за выбор картинки из папки выбор картинки

        uriImg = it
        imageUser.setImageURI(uriImg) //устанавливаем в юзера нашу картинку(передаем)
    }

    lateinit var builder: AlertDialog //Вызываем диалогове окно

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        init()

        imageUser.setOnClickListener { //При нажатии человек может выбрать картинку
            selectImage.launch("image/*")
        }

        signUpBtn.setOnClickListener {
            validData()
        }

        backBtn.setOnClickListener { //Переход из одного окна в другое
            val intent = Intent(this, AuthActivity::class.java)
            startActivity(intent)
            finish()
        }
    }


    private fun validData() { //проверяем корректность данных(проверка полей)
        if (TextUtils.isEmpty(nicknameInput.text.toString())) {
            nicknameInput.setError("Enter nickname")
        }else if (!Patterns.PHONE.matcher(phoneInput.text.toString()).matches()) {
            phoneInput.setError("Enter phone number")
        }else if(uriImg == null) {
            Toast.makeText(this, "Choose profile image", Toast.LENGTH_SHORT).show()
        }else {
            showOTPDialog()//отвечает за показ диалогового окна
        }
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
                Toast.makeText(this@SignUpActivity, p0.message, Toast.LENGTH_SHORT).show()
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



        closeBtn.setOnClickListener {//пишем, что будет просиходить после нажатия
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
                    val storageRef = FirebaseStorage.getInstance().getReference("profile") //папка
                        .child(FirebaseAuth.getInstance().currentUser!!.uid) //ребенок(подпапка)
                        .child("profile.jpg")

                    storageRef.putFile(uriImg!!) //profile.jpg закидываем нашу картинку сюда
                        .addOnSuccessListener {
                            storageRef.downloadUrl
                                .addOnSuccessListener {
                                    storeData(it)
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "${it.message}", Toast.LENGTH_SHORT).show()
                                    Log.e("err", it.message.toString())
                                }
                        }
                        .addOnFailureListener {
                            builder.dismiss()
                            Toast.makeText(this, "${it.message}", Toast.LENGTH_SHORT).show()
                            Log.e("err1", it.message.toString())
                        }
                }
            }
            .addOnCanceledListener {
                builder.dismiss()
                //Toast.makeText(this, "Лохъ", Toast.LENGTH_SHORT).show()
            }
    }

    private fun storeData(uri: Uri?) {//экземпляр класса пользователя
        val newUser = User( //новый тип класса пользователя
            FirebaseAuth.getInstance().currentUser!!.uid,
            FirebaseAuth.getInstance().currentUser!!.phoneNumber!!,
            nicknameInput.text.toString(),
            dobOfUser,
            uri.toString()
        )

        FirebaseDatabase.getInstance().getReference("users") //табличка, где лежат все пользователи
            .child(FirebaseAuth.getInstance().currentUser!!.phoneNumber!!)
            .setValue(newUser).addOnCompleteListener { //добавление нового пользователя в БД
                builder.dismiss()
                if (it.isSuccessful) {
                    Toast.makeText(this, "Успешная регистрация", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainActivity::class.java) //запускаем новыую активити
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "${it.exception!!.message}", Toast.LENGTH_SHORT).show()
                    Log.e("err2", it.exception!!.message.toString())
                }
            }.addOnFailureListener {
                Toast.makeText(this, "${it.message}", Toast.LENGTH_SHORT).show()
                Log.e("err3", it.message.toString())
            }

    }



    private fun init() {
        backBtn = findViewById(R.id.back_button)
        imageUser = findViewById(R.id.image_user)
        dobInput = findViewById(R.id.dob_input)
        nicknameInput = findViewById(R.id.nickname_input)
        phoneInput = findViewById(R.id.phone_input)
        signUpBtn = findViewById(R.id.sign_up_btn)
        mAuth = FirebaseAuth.getInstance()
        builder = AlertDialog.Builder(this).create()


        dobInput.init(2000, 0,1, object : DatePicker.OnDateChangedListener{
            override fun onDateChanged(
                view: DatePicker?,
                year: Int,
                monthOfYear: Int,
                dayOfMonth: Int
            ) {
                dobOfUser = "${view?.dayOfMonth}.${view!!.month + 1}.${view.year}"
            }

        })

    }

}
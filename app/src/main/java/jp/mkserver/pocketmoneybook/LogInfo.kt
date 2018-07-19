package jp.mkserver.pocketmoneybook

import android.app.Activity
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.CompoundButton
import android.widget.Toast
import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_log_info.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.yesButton
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import android.speech.RecognizerIntent
import android.content.Intent
import android.content.ActivityNotFoundException
import android.util.Log
import android.view.View
import java.nio.file.Files.size






class LogInfo : AppCompatActivity() {

    private lateinit var realm: Realm
    private var switch: Boolean = false
    private val REQUEST_CODE = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_info)
        Realm.init(applicationContext)
        realm = Realm.getDefaultInstance()

        val dataid = intent?.getLongExtra("data_id", -1L)
        if (dataid != -1L) {
            val data = realm.where<Data>().equalTo("id", dataid).findFirst()
            dateedit.setText(DateFormat.format("yyyy/MM/dd", data?.date))
            memoEdit.setText(data?.title)
            var bals: Int = data?.money!!
            if (bals < 0) {
                bals = bals.minus(bals).minus(bals)
                minusSwitch.isChecked = true
            }
            howmuchEdit.setText(bals.toString())
        }

        talkButton.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                try {
                    // インテント作成
                    val intent = Intent(
                            RecognizerIntent.ACTION_RECOGNIZE_SPEECH) // ACTION_WEB_SEARCH
                    intent.putExtra(
                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    intent.putExtra(
                            RecognizerIntent.EXTRA_PROMPT,
                            "『(理由)の理由で○○円をつかう/もらう』と言ってください") // お好きな文字に変更できます

                    // インテント発行
                    startActivityForResult(intent, REQUEST_CODE)
                } catch (e: ActivityNotFoundException) {
                    // このインテントに応答できるアクティビティがインストールされていない場合
                    Toast.makeText(this@LogInfo, "音声認識が利用できません(;・∀・)", Toast.LENGTH_SHORT).show()
                }

            }
        })

        saveButton.setOnClickListener {
            when (dataid) {
                -1L -> {
                    realm.executeTransaction {
                        val maxid = realm.where<Data>().max("id")
                        val nextId = (maxid?.toLong() ?: 0L) + 1
                        val data = realm.createObject<Data>(nextId)
                        try {
                            data.money = howmuchEdit.text.toString().replace(" ", "").toInt()
                            if (switch) {
                                data.money = -data.money
                            }
                            dateedit.text.toString().toDate("yyyy/MM/dd")?.let {
                                data.date = it
                            }
                            data.title = memoEdit.text.toString()
                            alert("ログを追加しました") {
                                yesButton { finish() }
                            }.show()
                        } catch (e: NumberFormatException) {
                            Toast.makeText(this, "どれぐらい？はすうじで入力してね！", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                else -> {
                    realm.executeTransaction {
                        val data = realm.where<Data>().equalTo("id", dataid).findFirst()
                        try {
                            data?.money = howmuchEdit.text.toString().replace(" ", "").toInt()
                            if (switch) {
                                data?.money = -data?.money!!
                            }
                            dateedit.text.toString().toDate("yyyy/MM/dd")?.let {
                                data?.date = it
                            }
                            data?.title = memoEdit.text.toString()
                            alert("ログを編集しました!") {
                                yesButton { finish() }
                            }.show()
                        } catch (e: NumberFormatException) {
                            Toast.makeText(this, "どれぐらい？はすうじで入力してね！", Toast.LENGTH_SHORT).show()
                        }
                    }

                }
            }
        }
        delete.setOnClickListener {
            realm.executeTransaction {
                realm.where<Data>().equalTo("id", dataid)?.findFirst()?.deleteFromRealm()
            }
            alert("削除しました。") {
                yesButton { finish() }
            }.show()
        }
        minusSwitch.setOnCheckedChangeListener { compoundButton: CompoundButton, b: Boolean ->
            var displayChar = ""
            // オンなら
            if (b) {
                displayChar = "おかねをつかったようになったよ！"
                switch = true
            } else {
                displayChar = "おかねをもらったようになったよ！"
                switch = false
            }// オフなら
            val toast = Toast.makeText(this, displayChar, Toast.LENGTH_SHORT)
            toast.show()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        realm.close()
    }

    fun onClick(v: View) {
        //音声入力がサポートされているか？
        if (packageManager.queryIntentActivities(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0).size == 0) {
            Log.d("tag", "Voice Input is not supported.")
            return   //サポートされてないとき無視
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        startActivityForResult(intent, 0)  //0:requestCode
    }


    // アクティビティ終了時に呼び出される
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == 0 && resultCode == Activity.RESULT_OK) {
            if (data.hasExtra(RecognizerIntent.EXTRA_RESULTS)) {
                //ここで認識された文字列を取得
                val results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                if (results.size > 0) {
                    var resultsString = "成功しました！"
                    val finalresult = results[0].split("の理由で")
                    if (finalresult.size < 2) {
                        resultsString = "言うものがたりないよ！"
                    } else {

                        memoEdit.setText(finalresult[0])
                        val truefinalresult = finalresult[1].split("円を")
                        if (truefinalresult.size < 2) {
                            resultsString = "使うかもらうかをいってほしいな！"
                        } else {
                            try {
                                when {
                                    truefinalresult[1] == "もらう" -> {
                                        howmuchEdit.setText(repJPBal(truefinalresult[0]).toString())
                                        minusSwitch.isChecked = false
                                    }
                                    truefinalresult[1] == "使う" -> {
                                        howmuchEdit.setText(repJPBal(truefinalresult[0]).toString())
                                        minusSwitch.isChecked = true
                                    }
                                    else -> {
                                        howmuchEdit.setText(finalresult[1])
                                        resultsString = "使うかもらうかがよくわからなかったよ！"
                                    }
                                }
                            } catch (e: NumberFormatException) {
                                resultsString = "おかねがよくわからなかったよ！"
                            }
                        }
                    }

                    // トーストを使って結果を表示
                    Toast.makeText(this, resultsString, Toast.LENGTH_LONG).show()
                }
            }
        }
    }


    fun String.toDate(pattern: String = "yyyy/MM/dd HH:mm"): Date? {
        val sdFormat = try {
            SimpleDateFormat(pattern)
        } catch (e: IllegalArgumentException) {
            null
        }
        val date = sdFormat?.let {
            try {
                it.parse(this)
            } catch (e: ParseException) {
                null
            }
        }
        return date
    }

    fun repJPBal(old: String): Int {
        try {
            return old.toInt()
        } catch (e: NumberFormatException) {
            var bal = 0
            try {
                if(old.contains("億")||old.contains("万")){
                    if(old.contains("億")){
                        val spl = old.split("億")
                        bal = (spl[0].toInt()*100000000)
                        if(spl.size >= 2) {
                            if (spl[1].contains("万")) {
                                val spls = spl[1].split("万")
                                bal += (spls[0].toInt() * 10000) + spls[1].toInt()
                            }
                        }
                    }else if(old.contains("万")){
                        val spls = old.split("万")
                        bal = (spls[0].toInt()*10000)
                        if(spls.size >= 2){
                            bal += spls[1].toInt()
                        }
                    }
                }
                return bal
            } catch (e: NumberFormatException) {
                return bal
            }

        }
    }


}

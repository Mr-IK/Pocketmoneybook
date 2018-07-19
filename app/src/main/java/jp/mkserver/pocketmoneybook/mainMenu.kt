package jp.mkserver.pocketmoneybook

import android.content.DialogInterface
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.KeyEvent
import android.view.View
import io.realm.Realm
import io.realm.RealmResults
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_main_menu.*
import org.jetbrains.anko.startActivity



class mainMenu : AppCompatActivity() {
    lateinit var realm: Realm
    var bals: Int = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)
        Realm.init(applicationContext)
        realm = Realm.getDefaultInstance()
        Realm.init(this)
        Balupdate()
        bal.text = bals.toString()
        if(bals<0){
            AlertDialog.Builder(this)
                    .setTitle("おかねがマイナスだよ！なおしてね！")
                    .setPositiveButton("はい") { dialog, which ->
                        unviewArere()
                    }
                    .show()
            viewArere()
        }
        val data = realm.where<Data>().findAll()
        listView.adapter = DataAdapter(data)
        listView.setOnItemClickListener { parent, view, position, id ->
            val data = parent.getItemAtPosition(position) as Data
            startActivity<LogInfo>("data_id" to data.id)
        }
        addlog.setOnClickListener { view ->
            startActivity<LogInfo>()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        realm.close()
    }

    override fun onResume(){
        super.onResume()
        Balupdate()
        bal.text = bals.toString()
    }

    fun Balupdate (){
        bals = read(realm).sum("money").toInt()
    }

    fun read(realm: Realm) : RealmResults<Data> {
        return realm.where(Data::class.java).findAll()
    }

    fun viewArere(){
        arere.visibility = View.VISIBLE
    }
    fun unviewArere(){
        arere.visibility = View.INVISIBLE
    }

}

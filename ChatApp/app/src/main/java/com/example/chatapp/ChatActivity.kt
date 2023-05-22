package com.example.chatapp


import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chatapp.adapter.MessageAdapter
import com.example.chatapp.data.Message
import com.example.chatapp.data.NotificationData
import com.example.chatapp.data.PushNotification
import com.example.chatapp.retrofit.RetrofitInstance
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class ChatActivity : AppCompatActivity() {
    val TAG = "ChatActivity"

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageBox: EditText
    private lateinit var sendButton: ImageView
    private lateinit var sendImageButton: ImageView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var messageList: ArrayList<Message>
    private lateinit var mDbRef: DatabaseReference


    var receiverRoom: String? = null
    var senderRoom: String? = null
    var usrName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val name = intent.getStringExtra("name")
        if (FirebaseAuth.getInstance().currentUser?.email.toString().contains("gmail.com")) {
            usrName = FirebaseAuth.getInstance().currentUser?.displayName.toString()
        } else {
            usrName = FirebaseAuth.getInstance().currentUser?.email.toString().substringBefore("@")
                .capitalize()
        }

        val receiverUid = intent.getStringExtra("uid")
        val receiverToken = intent.getStringExtra("deviceToken")

        val senderUid = FirebaseAuth.getInstance().currentUser?.uid
        mDbRef =
            FirebaseDatabase.getInstance("https://chatapp-7ecf9-default-rtdb.europe-west1.firebasedatabase.app/")
                .getReference()

        senderRoom = receiverUid + senderUid
        receiverRoom = senderUid + receiverUid

        supportActionBar?.title = name

        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        messageBox = findViewById(R.id.messageBox)
        sendButton = findViewById(R.id.sendButton)
        sendImageButton = findViewById(R.id.sendImageButton)
        messageList = ArrayList()
        messageAdapter = MessageAdapter(this, messageList)

        chatRecyclerView.layoutManager = LinearLayoutManager(this)
        chatRecyclerView.adapter = messageAdapter

        mDbRef.child("chats").child(senderRoom!!).child("messages")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    messageList.clear()

                    for (postSnapshot in snapshot.children) {
                        val message = postSnapshot.getValue(Message::class.java)
                        messageList.add(message!!)
                    }
                    messageAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })

        sendButton.setOnClickListener {
            val message = usrName.toString() + "-" + messageBox.text.toString()
            var messagenot = message
            if (messageBox.text.toString().startsWith("https://firebasestorage")) {
                messagenot = usrName.toString() + "-" + "Image Sent"
            }
            val messageObject =
                Message(message.toString().substringAfter("-"), senderUid, receiverUid)
            mDbRef.child("chats").child(senderRoom!!).child("messages").push()
                .setValue(messageObject).addOnSuccessListener {
                    mDbRef.child("chats").child(receiverRoom!!).child("messages").push()
                        .setValue(messageObject)
                }
            messageBox.setText("")
            if (message.isNotEmpty()) {
                var userDN: String? = null
                if (FirebaseAuth.getInstance().currentUser?.displayName.toString()
                        .isNullOrEmpty()
                ) {
                    userDN = FirebaseAuth.getInstance().currentUser?.displayName
                    Toast.makeText(
                        this@ChatActivity, "DISPLAYNAME" +
                                userDN, Toast.LENGTH_SHORT
                    ).show()
                }
                PushNotification(
                    NotificationData(userDN, messagenot),
                    receiverToken
                ).also {
                    sendNotification(it)
                }
            }
        }

        sendImageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, 4)
        }
    }

    @SuppressLint("SuspiciousIndentation")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null) {

            var selectedImage: Uri = data?.data!!
            val selectedImagePath = data.data!!

            val selectedImageBmp =
                MediaStore.Images.Media.getBitmap(contentResolver, selectedImagePath)

            val outputStream = ByteArrayOutputStream()

            selectedImageBmp.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            val selectedImageBytes = outputStream.toByteArray()


            val progressDialog = ProgressDialog(this)
            progressDialog.setMessage("Uploading file ...")
            progressDialog.setCancelable(false)
            progressDialog.show()

            val formatter = SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.getDefault())
            val now = Date()
            val fileName = formatter.format(now)
            val storageReference = FirebaseStorage.getInstance().getReference("images/$fileName")

            storageReference.putFile(selectedImage).addOnSuccessListener {
                val result = it.metadata!!.reference!!.downloadUrl;
                result.addOnSuccessListener {

                    var imageLink = it.toString()
                    messageBox.setText(imageLink)

                }
                selectedImage == null
                Toast.makeText(this@ChatActivity, "Image upload succeeded", Toast.LENGTH_SHORT)
                    .show()
                if (progressDialog.isShowing) progressDialog.dismiss()
            }.addOnFailureListener {
                if (progressDialog.isShowing) progressDialog.dismiss()
                Toast.makeText(this@ChatActivity, "Image upload failed", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun sendNotification(notification: PushNotification) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitInstance.api.postNotification(notification)
                if (response.isSuccessful) {
                    Log.d(TAG, "Response: ${Gson().toJson(response)}")
                } else {
                    Log.e(TAG, response.errorBody().toString())
                    Log.e(TAG, response.body()?.charStream().toString())
                }
            } catch (e: Exception) {
                Log.e(TAG, e.toString())
            }
        }
    }


}


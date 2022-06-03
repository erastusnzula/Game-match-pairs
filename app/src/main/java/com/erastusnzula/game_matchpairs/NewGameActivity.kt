package com.erastusnzula.game_matchpairs

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.Intent.ACTION_PICK
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.erastusnzula.game_matchpairs.models.BoardSize
import com.erastusnzula.game_matchpairs.utils.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.ByteArrayOutputStream

class NewGameActivity : AppCompatActivity() {
    companion object {
        private const val PICK_IMAGE_CODE = 3
        private const val READ_EXTERNAL_CODE = 4
        private const val READ_EXTERNAL_PERMISSION =
            android.Manifest.permission.READ_EXTERNAL_STORAGE
    }

    private lateinit var recyclerViewNewGame: RecyclerView
    private lateinit var save: Button
    private lateinit var gameName: TextView
    private lateinit var progressBar: ProgressBar

    private lateinit var boardSize: BoardSize
    private var requiredImages = -1
    private var chosenImagesListUri = mutableListOf<Uri>()
    private lateinit var adapter: NewGameAdapter
    private var storage = Firebase.storage
    private var database = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_game)
        recyclerViewNewGame = findViewById(R.id.recycler)
        save = findViewById(R.id.saveButton)
        gameName = findViewById(R.id.gameName)
        progressBar = findViewById(R.id.progressbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        boardSize = intent.getSerializableExtra(EXTRA_BOARD_SIZE) as BoardSize
        requiredImages = boardSize.getPairs()
        supportActionBar?.title = "Choose $requiredImages images"

        save.setOnClickListener {
            saveImagesToFireBase()
        }
        gameName.filters = arrayOf(InputFilter.LengthFilter(10))
        gameName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                save.isEnabled = enableSaveButton()
            }

        })

        adapter = NewGameAdapter(
            this,
            chosenImagesListUri,
            boardSize,
            object : NewGameAdapter.ImageViewClicked {
                override fun onImageViewClick() {
                    if (isPermissionGranted(this@NewGameActivity, READ_EXTERNAL_PERMISSION)) {
                        launchIntentForImages()
                    } else {
                        requestForPermissions(
                            this@NewGameActivity,
                            READ_EXTERNAL_PERMISSION,
                            READ_EXTERNAL_CODE
                        )
                    }

                }

            })
        recyclerViewNewGame.adapter = adapter
        recyclerViewNewGame.setHasFixedSize(true)
        recyclerViewNewGame.layoutManager = GridLayoutManager(this, boardSize.getColumns())
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == READ_EXTERNAL_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            launchIntentForImages()
        } else {
            Toast.makeText(this, "External permission needed", Toast.LENGTH_LONG).show()
            allPermissionDialog()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

//    val getImages = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
//        val bitmap = it?.data?.extras?.get("data")
//        chosenImagesListUri.add(bitmap as Uri)
//    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != PICK_IMAGE_CODE || resultCode != Activity.RESULT_OK || data == null) {
            return
        } else {
            val singleImagePicked = data.data
            val multipleImagesPicked = data.clipData
            if (multipleImagesPicked != null) {
                for (i in 0 until multipleImagesPicked.itemCount) {
                    val image = multipleImagesPicked.getItemAt(i)
                    if (chosenImagesListUri.size < requiredImages) {
                        chosenImagesListUri.add(image.uri)
                        adapter.notifyDataSetChanged()
                    }
                }
            } else {
                if (singleImagePicked != null) {
                    chosenImagesListUri.add(singleImagePicked)
                    adapter.notifyDataSetChanged()
                }
            }
            supportActionBar?.title =
                "Uploaded : ${chosenImagesListUri.size}/$requiredImages images"
            save.isEnabled = enableSaveButton()
        }
    }

    private fun enableSaveButton(): Boolean {
        return if (chosenImagesListUri.size != requiredImages) {
            false
        } else !(gameName.text.isBlank() || gameName.text.length < 3)
    }

    private fun launchIntentForImages() {
        val intent = Intent(ACTION_PICK)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(Intent.createChooser(intent, "Choose images"), PICK_IMAGE_CODE)
        //getImages.launch(intent)

    }

    private fun allPermissionDialog() {
        AlertDialog.Builder(this)
            .setMessage("Permissions turned off, click Go to settings to allow permissions")
            .setPositiveButton("Go to settings") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)

                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    private fun saveImagesToFireBase() {
        save.isEnabled = false
        val name = gameName.text.toString()
        database.collection("Game-match pairs").document(name).get()
            .addOnSuccessListener { document ->
                if (document != null && document.data != null) {
                    AlertDialog.Builder(this)
                        .setTitle("Game Name")
                        .setMessage("The name: $name is already taken, please choose another.")
                        .setPositiveButton("Okay", null)
                        .show()
                    save.isEnabled = true
                } else {
                    handleUploadingImages(name)
                }
            }.addOnFailureListener {
                Toast.makeText(
                    this,
                    "Error while saving game, please make sure you have an active network connection.",
                    Toast.LENGTH_LONG
                ).show()
                save.isEnabled = true
            }

    }

    private fun handleUploadingImages(name: String) {
        progressBar.visibility = View.VISIBLE
        var isError = false
        val uploadedImagesUrl = mutableListOf<String>()
        for ((index, imageUri) in chosenImagesListUri.withIndex()) {
            val imageByteArray = getImageByteArray(imageUri)
            val filePath = "images/$name/${System.currentTimeMillis()}-${index}.jpg"
            val imageReference = storage.reference.child(filePath)
            imageReference.putBytes(imageByteArray)
                .continueWithTask {
                    imageReference.downloadUrl
                }.addOnCompleteListener { downLoadTask ->
                    if (!downLoadTask.isSuccessful) {
                        Toast.makeText(
                            this,
                            "Failed to upload images, please try again and make sure you have an active internet connection.",
                            Toast.LENGTH_LONG
                        ).show()
                        isError = true
                        return@addOnCompleteListener
                    } else if (isError) {
                        progressBar.visibility = View.GONE
                        return@addOnCompleteListener
                    } else {
                        val downloadUrl = downLoadTask.result.toString()
                        uploadedImagesUrl.add(downloadUrl)
                        progressBar.progress =
                            uploadedImagesUrl.size * 100 / chosenImagesListUri.size
                        if (uploadedImagesUrl.size == chosenImagesListUri.size) {
                            handleUploadedImages(name, uploadedImagesUrl)
                        }
                    }
                }
        }

    }

    private fun handleUploadedImages(gameName: String, imageUrl: MutableList<String>) {
        database.collection("Game-match pairs").document(gameName)
            .set(mapOf("images" to imageUrl))
            .addOnCompleteListener { gameCreation ->
                progressBar.visibility = View.GONE
                if (!gameCreation.isSuccessful) {
                    Toast.makeText(this, "Game creation failed", Toast.LENGTH_LONG).show()
                    return@addOnCompleteListener
                } else {
                    AlertDialog.Builder(this)
                        .setTitle("The game: $gameName, uploaded successfully. Play game?")
                        .setPositiveButton("Yes") { _, _ ->
                            val output = Intent()
                            output.putExtra(EXTRA_GAME_NAME, gameName)
                            setResult(Activity.RESULT_OK, output)
                            finish()
                        }.show()
                }
            }

    }

    private fun getImageByteArray(imageUri: Uri): ByteArray {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(contentResolver, imageUri)
            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
        }
        val bitMapScaler = BitMapScaler.scaleToFitHeight(bitmap, 250)
        val outputStream = ByteArrayOutputStream()
        bitMapScaler.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
        return outputStream.toByteArray()

    }
}
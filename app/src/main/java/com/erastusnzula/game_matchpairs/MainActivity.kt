package com.erastusnzula.game_matchpairs

import android.animation.ArgbEvaluator
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.erastusnzula.game_matchpairs.models.BoardSize
import com.erastusnzula.game_matchpairs.models.CustomGameImageList
import com.erastusnzula.game_matchpairs.models.GameLogic
import com.erastusnzula.game_matchpairs.utils.EXTRA_BOARD_SIZE
import com.erastusnzula.game_matchpairs.utils.EXTRA_GAME_NAME
import com.github.jinatonic.confetti.CommonConfetti
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso

class MainActivity : AppCompatActivity() {
    companion object {
        private const val CREATE_NEW_ACTIVITY_CODE = 2
    }

    private lateinit var recylerview: RecyclerView
    private var database = Firebase.firestore
    private var gameName: String? = null
    private var customGameImages: List<String>? = null
    private lateinit var constrainedLayout: CoordinatorLayout
    private lateinit var moves: TextView
    private lateinit var pairs: TextView
    private lateinit var gameLogic: GameLogic
    private lateinit var adapter: GameAdapter
    private var boardSize: BoardSize = BoardSize.HARD
    private lateinit var gameOver: MediaPlayer
    private lateinit var invalidMove: MediaPlayer
    private var lose = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        constrainedLayout = findViewById(R.id.constrainedLayout)
        moves = findViewById(R.id.moves)
        pairs = findViewById(R.id.pairs)
        gameSetup()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.refresh -> {
                if (gameLogic.getNumberMoves() > 0 && gameLogic.pairsFound > 0 && !gameLogic.gameOver()) {
                    showAlertDialog(getString(R.string.quit), null) {
                        gameSetup()
                    }
                } else {
                    gameSetup()
                }
                return true

            }
            R.id.chooseGameSize -> {
                showGameSizes()
                return true
            }
            R.id.new_game -> {
                createNewGameDialog()
                return true
            }
            R.id.downloadGame -> {
                showCustomGameDownloadDialog()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CREATE_NEW_ACTIVITY_CODE && resultCode == Activity.RESULT_OK) {
            val newGameName = data?.getStringExtra(EXTRA_GAME_NAME)
            if (newGameName == null) {
                return
            } else {
                downloadNewGame(newGameName)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun showCustomGameDownloadDialog() {
        val boardDownloadView = LayoutInflater.from(this).inflate(R.layout.download_game, null)
        showAlertDialog("Download Game", boardDownloadView, View.OnClickListener {
            val gameToDownload = boardDownloadView.findViewById<EditText>(R.id.entergametodownload)
            val download = gameToDownload.text.toString().trim()
            downloadNewGame(download)

        })
    }

    private fun downloadNewGame(name: String) {
        database.collection("Game-match pairs").document(name).get()
            .addOnSuccessListener { document ->
                val customGameImageList = document.toObject(CustomGameImageList::class.java)
                if (customGameImageList?.images == null) {
                    Snackbar.make(
                        constrainedLayout,
                        "Sorry no game under that name",
                        Snackbar.LENGTH_LONG
                    ).show()
                    return@addOnSuccessListener
                } else {
                    val doubleImages = customGameImageList.images.size * 2
                    boardSize = BoardSize.getByValue(doubleImages)
                    customGameImages = customGameImageList.images
                    for (image in customGameImageList.images) {
                        Picasso.get().load(image).fetch()
                    }
                    Snackbar.make(
                        constrainedLayout,
                        "You are now playing $name game.",
                        Snackbar.LENGTH_LONG
                    ).show()
                    gameName = name
                    gameSetup()
                }

            }.addOnFailureListener {
                Toast.makeText(this, "Sorry your game failed to download", Toast.LENGTH_LONG).show()
            }

    }

    private fun createNewGameDialog() {
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board, null)
        val radioGroup = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)
        showAlertDialog(getString(R.string.create_new_game), boardSizeView) {
            val newBoardSize = when (radioGroup.checkedRadioButtonId) {
                R.id.easy -> BoardSize.EASY
                R.id.medium -> BoardSize.MEDIUM
                R.id.hard -> BoardSize.HARD
                else -> BoardSize.ADVANCED
            }

            val intent = Intent(this, NewGameActivity::class.java)
            intent.putExtra(EXTRA_BOARD_SIZE, newBoardSize)
            startActivityForResult(intent, CREATE_NEW_ACTIVITY_CODE)
        }
    }

    private fun showGameSizes() {
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board, null)
        val radioGroup = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)
        when (boardSize) {
            BoardSize.EASY -> radioGroup.check(R.id.easy)
            BoardSize.MEDIUM -> radioGroup.check(R.id.medium)
            BoardSize.HARD -> radioGroup.check(R.id.hard)
            BoardSize.ADVANCED -> radioGroup.check(R.id.advanced)
        }
        showAlertDialog(getString(R.string.choose_size), boardSizeView) {
            boardSize = when (radioGroup.checkedRadioButtonId) {
                R.id.easy -> BoardSize.EASY
                R.id.medium -> BoardSize.MEDIUM
                R.id.hard -> BoardSize.HARD
                else -> BoardSize.ADVANCED
            }
            gameName = null
            customGameImages = null
            gameSetup()
        }
    }

    private fun showAlertDialog(title: String, view: View?, positiveClick: View.OnClickListener) {
        AlertDialog.Builder(this)
            .setIcon(R.drawable.ic_baseline_view_module_24)
            .setTitle(title)
            .setView(view)
            .setCancelable(false)
            .setNegativeButton("No", null)
            .setPositiveButton("Yes") { _, _ ->
                positiveClick.onClick(null)
            }.show()
    }

    private fun gameSetup() {
        supportActionBar?.title = gameName ?: getString(R.string.app_name)
        when (boardSize) {
            BoardSize.EASY -> {
                pairs.text = getString(R.string.easy_pairs)
                moves.text = getString(R.string.easy_moves)
            }
            BoardSize.MEDIUM -> {
                pairs.text = getString(R.string.medium_pairs)
                moves.text = getString(R.string.medium_moves)
            }
            BoardSize.HARD -> {
                pairs.text = getString(R.string.hard_pairs)
                moves.text = getString(R.string.hard_moves)
            }
            BoardSize.ADVANCED -> {
                pairs.text = getString(R.string.advanced_pairs)
                moves.text = getString(R.string.advanced_moves)
            }
        }
        pairs.setTextColor(ContextCompat.getColor(this, R.color.purple_200))
        recylerview = findViewById(R.id.recyclerView)
        gameLogic = GameLogic(boardSize, customGameImages)
        adapter = GameAdapter(
            this,
            boardSize,
            gameLogic.imageListDoubled,
            object : GameAdapter.ImageButtonClickListener {
                override fun onImageButtonClicked(position: Int) {
                    updateGame(position)
                }
            })
        recylerview.adapter = adapter
        recylerview.setHasFixedSize(true)
        recylerview.layoutManager = GridLayoutManager(this, boardSize.getColumns())
    }

    private fun updateGame(position: Int) {
        when {
            gameLogic.gameOver() -> {
                val snack = Snackbar.make(
                    constrainedLayout,
                    getString(R.string.game_over),
                    Snackbar.LENGTH_LONG
                )
                snack.setActionTextColor(Color.GREEN)
                snack.setAction("Restart Game") {
                    gameSetup()
                }
                snack.show()
                gameOverSelectAction()
                return
            }
            gameLogic.cardIsFaceUp(position) -> {
                val snack = Snackbar.make(
                    constrainedLayout,
                    getString(R.string.invalid_move),
                    Snackbar.LENGTH_LONG
                )
                snack.setActionTextColor(Color.GREEN)
                snack.setAction("Restart Game") {
                    gameSetup()
                }
                snack.show()
                invalidMove = MediaPlayer.create(this, R.raw.failure)
                invalidMove.setVolume(0f, .2f)
                invalidMove.start()
                return
            }
            else -> {
                if (gameLogic.flipCard(position)) {
                    val color = ArgbEvaluator().evaluate(
                        gameLogic.pairsFound.toFloat() / boardSize.getPairs(),
                        ContextCompat.getColor(this, R.color.purple_200),
                        ContextCompat.getColor(this, R.color.complete)
                    ) as Int
                    pairs.setTextColor(color)
                    val pairText =
                        getString(R.string.pairs, gameLogic.pairsFound, boardSize.getPairs())
                    pairs.text = pairText
                    val playSound = MediaPlayer.create(this, R.raw.funny)
                    playSound.setVolume(0f, .2f)
                    playSound?.start()
                    if (gameLogic.gameOver()) {
                        CommonConfetti.rainingConfetti(
                            constrainedLayout, intArrayOf(
                                Color.YELLOW,
                                Color.GREEN,
                                Color.MAGENTA,
                                Color.CYAN,
                                Color.RED,
                                Color.LTGRAY,
                                Color.BLACK,
                                Color.DKGRAY,
                                Color.TRANSPARENT,
                                Color.WHITE,
                                Color.rgb(128, 128, 128),
                                Color.rgb(255, 128, 0),
                                Color.rgb(51, 153, 255),
                                Color.rgb(153, 255, 51),
                                Color.rgb(178, 102, 255),
                                Color.rgb(229, 204, 255)
                            )
                        ).oneShot()
                        val snack = Snackbar.make(
                            constrainedLayout,
                            getString(R.string.win),
                            Snackbar.LENGTH_LONG
                        )
                        snack.setActionTextColor(Color.GREEN)
                        snack.setAction("Play again") {
                            gameSetup()
                        }
                        snack.show()
                        gameOverSelectAction()
                        gameOver = MediaPlayer.create(this, R.raw.ethereal)
                        gameOver.setVolume(0f, .2f)
                        gameOver.start()


                    }
                }
                val moveText = getString(R.string.moves, gameLogic.getNumberMoves())
                moves.text = moveText
                checkGameMoves()
                adapter.notifyDataSetChanged()
            }
        }

    }



    private fun checkGameMoves(): Boolean {

        when (boardSize) {
            BoardSize.EASY -> {
                if (gameLogic.getNumberMoves() > 10 && !gameLogic.gameOver()) {
                    lose = true
                    showEndGameDialog()
                }
            }
            BoardSize.MEDIUM -> {
                if (gameLogic.getNumberMoves() > 30 && !gameLogic.gameOver()) {
                    lose = true
                    showEndGameDialog()
                }
            }
            BoardSize.HARD -> {
                if (gameLogic.getNumberMoves() > 50 && !gameLogic.gameOver()) {
                    lose = true
                    showEndGameDialog()
                }
            }
            BoardSize.ADVANCED -> {
                if (gameLogic.getNumberMoves() > 75 && !gameLogic.gameOver()) {
                    lose = true
                    showEndGameDialog()
                }
            }
        }
        return lose
    }
    private fun showEndGameDialog() {
        val alert = AlertDialog.Builder(this)
        alert.setTitle("Out of moves")
        val options = arrayOf(
            "1. Play again",
            "2. Change game level",
            "3. Create custom game",
            "4. Download custom game"
        )
        alert.setCancelable(false)
        alert.setItems(options){_,which->
            when (which){
                0->gameSetup()
                1->showGameSizes()
                2->createNewGameDialog()
                3->showCustomGameDownloadDialog()
            }
        }
        alert.show()
    }

    private fun gameOverSelectAction() {
        val gameOver = AlertDialog.Builder(this)
        gameOver.setTitle("Congratulations")
        gameOver.setIcon(R.drawable.ic_win)
        val gameOptions = arrayOf(
            "1. Play Again",
            "2. Change game level",
            "3. Download custom game",
            "4. Create custom game"
        )
        gameOver.setItems(gameOptions) { _, which ->
            when (which) {
                0 -> gameSetup()
                1 -> showGameSizes()
                2 -> showCustomGameDownloadDialog()
                3 -> createNewGameDialog()
            }
        }
        gameOver.setCancelable(false)
        gameOver.show()
    }
}
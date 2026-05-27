package com.jnetai.sporeheist

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.*
import java.util.*

enum class Organ { BRAIN, LUNGS, BLOODSTREAM }

class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG = "SporeHeist"
        const val CURRENT_VERSION = "1.0.0"
        const val GITHUB_REPO = "jnetai-clawbot/SporeHeist"
    }

    private lateinit var gameView: GameView
    private lateinit var aboutButton: Button
    private lateinit var scoreText: TextView
    private lateinit var organText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = 0xFF0A0A1A.toInt()
        window.navigationBarColor = 0xFF0A0A1A.toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0A0A1A.toInt())
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val hudRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(32, 32, 32, 8)
        }

        scoreText = TextView(this).apply {
            text = "Score: 0"
            setTextColor(0xFF33FF66.toInt())
            textSize = 16f
            typeface = Typeface.MONOSPACE
        }

        organText = TextView(this).apply {
            text = "Organ: Brain"
            setTextColor(0xFF33FF66.toInt())
            textSize = 16f
            typeface = Typeface.MONOSPACE
            setPadding(32, 0, 0, 0)
        }

        hudRow.addView(scoreText)
        hudRow.addView(organText)

        gameView = GameView(this, ::updateScore, ::updateOrgan)

        val buttonBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            setPadding(16, 8, 16, 48)
        }

        val restartBtn = Button(this).apply {
            text = "Restart"
            setBackgroundColor(0xFF1A2A3A.toInt())
            setTextColor(0xFFCCCCCC.toInt())
            textSize = 14f
            minHeight = 0
            minimumHeight = 80
            setPadding(24, 12, 24, 12)
            setOnClickListener { gameView.restart() }
        }

        aboutButton = Button(this).apply {
            text = "About"
            setBackgroundColor(0xFF1A2A3A.toInt())
            setTextColor(0xFF33FF66.toInt())
            textSize = 14f
            minHeight = 0
            minimumHeight = 80
            setPadding(24, 12, 24, 12)
            setOnClickListener { showAbout() }
        }

        buttonBar.addView(restartBtn)
        val spacer = View(this).apply { layoutParams = LinearLayout.LayoutParams(32, 0) }
        buttonBar.addView(spacer)
        buttonBar.addView(aboutButton)

        root.addView(hudRow)
        root.addView(gameView, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        root.addView(buttonBar)
        setContentView(root)
    }

    private fun updateScore(score: Int) {
        runOnUiThread { scoreText.text = "Score: $score" }
    }

    private fun updateOrgan(organ: String) {
        runOnUiThread { organText.text = "Organ: $organ" }
    }

    private fun showAbout() {
        val builder = AlertDialog.Builder(this, R.style.AboutDialogTheme)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 32)
            setBackgroundColor(0xFF151528.toInt())
        }

        layout.addView(TextView(this).apply {
            text = "Spore Heist"
            setTextColor(0xFF00FF88.toInt())
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 8)
        })

        layout.addView(TextView(this).apply {
            text = "Made by jnetai.com"
            setTextColor(0xFF888899.toInt())
            textSize = 14f
            setPadding(0, 0, 0, 16)
        })

        layout.addView(TextView(this).apply {
            text = "Version $CURRENT_VERSION"
            setTextColor(0xFFCCCCCC.toInt())
            textSize = 16f
            setPadding(0, 0, 0, 24)
        })

        val checkBtn = Button(this).apply {
            text = "Check for Update"
            setBackgroundColor(0xFF006644.toInt())
            setTextColor(0xFF00FF88.toInt())
            textSize = 15f
            minimumHeight = 96
            setPadding(32, 16, 32, 16)
            val btn = this
            setOnClickListener {
                btn.isEnabled = false
                btn.text = "Checking..."
                checkForUpdate { result ->
                    runOnUiThread {
                        btn.text = result
                        btn.isEnabled = true
                    }
                }
            }
        }
        layout.addView(checkBtn)

        layout.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 24)
        })

        val shareBtn = Button(this).apply {
            text = "Share App"
            setBackgroundColor(0xFF234A6A.toInt())
            setTextColor(0xFF00CCFF.toInt())
            textSize = 15f
            minimumHeight = 96
            setPadding(32, 16, 32, 16)
            setOnClickListener {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Spore Heist")
                    putExtra(Intent.EXTRA_TEXT, getString(R.string.share_message))
                }
                startActivity(Intent.createChooser(intent, "Share via"))
            }
        }
        layout.addView(shareBtn)

        val scrollView = ScrollView(this).apply {
            addView(layout)
        }

        builder.setView(scrollView)
            .setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun checkForUpdate(callback: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://api.github.com/repos/$GITHUB_REPO/releases/latest")
                val conn = url.openConnection() as HttpURLConnection
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                conn.connectTimeout = 8000
                conn.readTimeout = 8000

                val response = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val latestTag = json.getString("tag_name").removePrefix("v")

                if (latestTag != CURRENT_VERSION) {
                    callback("New version $latestTag available!")
                } else {
                    callback("You're up to date!")
                }
            } catch (e: Exception) {
                callback("Could not check updates: ${e.message}")
            }
        }
    }
}

class GameView(
    context: Context,
    private val scoreCallback: (Int) -> Unit,
    private val organCallback: (String) -> Unit
) : View(context) {
    companion object {
        const val WORLD_W = 2400f
        const val WORLD_H = 2400f
        const val PLAYER_RADIUS = 18f
        const val CELL_RADIUS = 14f
        const val PLAYER_SPEED = 8f
        const val IMMUNE_CHASE_RADIUS = 200f
        const val IMMUNE_SPEED = 3.5f
        const val MITO_COUNT = 20
        const val IMMUNE_COUNT = 8
        const val NEURAL_COUNT = 5
        const val COLLECT_TARGET = 10
        const val NEURAL_TAP_RADIUS = 100f
        const val STUN_RADIUS_MAX = 220f
        const val STUN_DURATION_MS = 4000
        const val WIND_STRENGTH = 2.5f
        const val CURRENT_STRENGTH = 4f
        const val CELL_DRIFT_SPEED = 1.2f
        const val TAG = "GameView"
    }

    private val random = Random()

    private var playerX = WORLD_W / 2f
    private var playerY = WORLD_H / 2f
    private var targetX = playerX
    private var targetY = playerY
    private var touchActive = false

    private var currentOrgan = Organ.BRAIN
    private var score = 0
    private var collectedThisLevel = 0
    private var gameOver = false
    private var showingOrganBanner = false
    private var organBannerTimer = 0
    private var frameCount = 0

    private val mitochondria = mutableListOf<Mitochondria>()
    private val immuneCells = mutableListOf<ImmuneCell>()
    private val neuralCells = mutableListOf<NeuralCell>()
    private val stunPulses = mutableListOf<StunPulse>()
    private val walls = mutableListOf<WallRect>()

    private var windAngle = 0f
    private var windTimer = 0

    private val bgPaint = Paint().apply { color = 0xFF0A0A1A.toInt(); style = Paint.Style.FILL }
    private val wallPaint = Paint().apply { color = 0xFF2A1A3A.toInt(); style = Paint.Style.FILL }
    private val wallEdgePaint = Paint().apply { color = 0xFF3A2A4A.toInt(); style = Paint.Style.STROKE; strokeWidth = 2f }
    private val mitoPaint = Paint().apply { color = 0xFF33FF66.toInt(); style = Paint.Style.FILL }
    private val mitoGlowPaint = Paint().apply { color = 0x6633FF66.toInt(); style = Paint.Style.FILL }
    private val immunePaint = Paint().apply { color = 0xFFFFFFFF.toInt(); style = Paint.Style.FILL }
    private val immuneAlertPaint = Paint().apply { color = 0xFFFF6644.toInt(); style = Paint.Style.FILL }
    private val immuneStunPaint = Paint().apply { color = 0xFF888899.toInt(); style = Paint.Style.FILL }
    private val immuneGlowPaint = Paint().apply { color = 0x55FFFFFF.toInt(); style = Paint.Style.FILL }
    private val neuralPaint = Paint().apply { color = 0xFF4488FF.toInt(); style = Paint.Style.FILL }
    private val neuralGlowPaint = Paint().apply { color = 0x664488FF.toInt(); style = Paint.Style.FILL }
    private val playerPaint = Paint().apply { color = 0xFF33FF66.toInt(); style = Paint.Style.FILL }
    private val playerGlowPaint = Paint().apply { color = 0x6633FF66.toInt(); style = Paint.Style.FILL }
    private val playerOutlinePaint = Paint().apply { color = 0xFF66FF99.toInt(); style = Paint.Style.STROKE; strokeWidth = 2f }
    private val stunPaint = Paint().apply { color = 0x664488FF.toInt(); style = Paint.Style.FILL }
    private val stunRingPaint = Paint().apply { color = 0xAA4488FF.toInt(); style = Paint.Style.STROKE; strokeWidth = 3f }
    private val targetPaint = Paint().apply { color = 0x8833FF66.toInt(); style = Paint.Style.STROKE; strokeWidth = 2f }
    private val targetFillPaint = Paint().apply { color = 0x2233FF66.toInt(); style = Paint.Style.FILL }
    private val gridPaint = Paint().apply { color = 0x1100FF88.toInt(); strokeWidth = 1f }
    private val progressPaint = Paint().apply { color = 0xFF33FF66.toInt(); style = Paint.Style.STROKE; strokeWidth = 4f }
    private val progressBgPaint = Paint().apply { color = 0x3333FF66.toInt(); style = Paint.Style.STROKE; strokeWidth = 4f }
    private val hudTextPaint = Paint().apply {
        color = 0xFF33FF66.toInt()
        textSize = 14f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.MONOSPACE
    }
    private val bannerOverlayPaint = Paint().apply { color = 0xCC000000.toInt(); style = Paint.Style.FILL }
    private val bannerTextPaint = Paint().apply {
        color = 0xFF33FF66.toInt()
        textSize = 36f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val gameOverOverlayPaint = Paint().apply { color = 0xBB000000.toInt(); style = Paint.Style.FILL }
    private val goTextPaint = Paint().apply {
        color = 0xFFFF3344.toInt()
        textSize = 48f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val goSubTextPaint = Paint().apply {
        color = 0xFFCCCCCC.toInt()
        textSize = 28f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val organTagPaint = Paint().apply {
        textSize = 12f
        textAlign = Paint.Align.LEFT
        typeface = Typeface.MONOSPACE
        color = 0x889999AA.toInt()
    }
    private val collectTextPaint = Paint().apply {
        color = 0xFF66FF99.toInt()
        textSize = 18f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    init {
        generateLevel()
        post(gameLoop)
    }

    private fun generateLevel() {
        mitochondria.clear()
        immuneCells.clear()
        neuralCells.clear()
        stunPulses.clear()
        walls.clear()

        playerX = WORLD_W / 2f
        playerY = WORLD_H / 2f
        targetX = playerX
        targetY = playerY
        collectedThisLevel = 0
        gameOver = false
        touchActive = false
        frameCount = 0

        when (currentOrgan) {
            Organ.BRAIN -> generateBrainWalls()
            Organ.LUNGS -> {
                windAngle = random.nextFloat() * PI.toFloat() * 2f
                windTimer = 0
            }
            Organ.BLOODSTREAM -> {}
        }

        var placed = 0
        while (placed < MITO_COUNT) {
            val mx = random.nextFloat() * (WORLD_W - 120) + 60
            val my = random.nextFloat() * (WORLD_H - 120) + 60
            if (!isBlocked(mx, my, CELL_RADIUS + 10)) {
                mitochondria.add(Mitochondria(mx, my))
                placed++
            }
        }

        placed = 0
        while (placed < IMMUNE_COUNT) {
            val ix = random.nextFloat() * (WORLD_W - 120) + 60
            val iy = random.nextFloat() * (WORLD_H - 120) + 60
            val dx = ix - playerX
            val dy = iy - playerY
            if (sqrt(dx * dx + dy * dy) > 300 && !isBlocked(ix, iy, CELL_RADIUS + 10)) {
                immuneCells.add(ImmuneCell(ix, iy))
                placed++
            }
        }

        placed = 0
        while (placed < NEURAL_COUNT) {
            val nx = random.nextFloat() * (WORLD_W - 120) + 60
            val ny = random.nextFloat() * (WORLD_H - 120) + 60
            if (!isBlocked(nx, ny, CELL_RADIUS + 10)) {
                neuralCells.add(NeuralCell(nx, ny))
                placed++
            }
        }

        showingOrganBanner = true
        organBannerTimer = 0
        updateOrganCallback()
        scoreCallback(score)
    }

    private fun generateBrainWalls() {
        walls.clear()
        val margin = 120f
        val cols = 7
        val rows = 7
        val cellW = (WORLD_W - margin * 2) / cols
        val cellH = (WORLD_H - margin * 2) / rows

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                if (random.nextFloat() < 0.55f) continue
                val isHorizontal = random.nextBoolean()
                val cx = margin + col * cellW + cellW / 2
                val cy = margin + row * cellH + cellH / 2
                if (isHorizontal) {
                    val w = cellW * 0.65f
                    val h = 22f
                    walls.add(WallRect(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2))
                } else {
                    val w = 22f
                    val h = cellH * 0.65f
                    walls.add(WallRect(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2))
                }
            }
        }

        walls.removeAll { wall ->
            val cx = WORLD_W / 2f
            val cy = WORLD_H / 2f
            val closestX = maxOf(wall.left, minOf(cx, wall.right))
            val closestY = maxOf(wall.top, minOf(cy, wall.bottom))
            val dx = cx - closestX
            val dy = cy - closestY
            sqrt(dx * dx + dy * dy) < 180
        }
    }

    private fun isBlocked(x: Float, y: Float, r: Float): Boolean {
        if (x - r < 0 || x + r > WORLD_W || y - r < 0 || y + r > WORLD_H) return true
        if (currentOrgan == Organ.BRAIN) {
            for (wall in walls) {
                if (x + r > wall.left && x - r < wall.right &&
                    y + r > wall.top && y - r < wall.bottom) return true
            }
        }
        return false
    }

    private fun advanceOrgan() {
        currentOrgan = when (currentOrgan) {
            Organ.BRAIN -> Organ.LUNGS
            Organ.LUNGS -> Organ.BLOODSTREAM
            Organ.BLOODSTREAM -> Organ.BRAIN
        }
        generateLevel()
    }

    private fun updateOrganCallback() {
        val name = when (currentOrgan) {
            Organ.BRAIN -> "Brain"
            Organ.LUNGS -> "Lungs"
            Organ.BLOODSTREAM -> "Bloodstream"
        }
        organCallback(name)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (gameOver) {
            if (event.action == MotionEvent.ACTION_DOWN) restart()
            return true
        }

        if (showingOrganBanner) return true

        val screenCenterX = width / 2f
        val screenCenterY = height / 2f
        val worldX = event.x - screenCenterX + playerX
        val worldY = event.y - screenCenterY + playerY

        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                var tappedNeural = false
                for (nc in neuralCells) {
                    val ndx = worldX - nc.x
                    val ndy = worldY - nc.y
                    val ndist = sqrt(ndx * ndx + ndy * ndy)
                    if (ndist < NEURAL_TAP_RADIUS) {
                        stunPulses.add(StunPulse(nc.x, nc.y, STUN_RADIUS_MAX, STUN_DURATION_MS))
                        tappedNeural = true
                        break
                    }
                }
                if (!tappedNeural) {
                    targetX = worldX.coerceIn(PLAYER_RADIUS + 10, WORLD_W - PLAYER_RADIUS - 10)
                    targetY = worldY.coerceIn(PLAYER_RADIUS + 10, WORLD_H - PLAYER_RADIUS - 10)
                    touchActive = true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                touchActive = false
            }
        }
        return true
    }

    private val gameLoop = object : Runnable {
        override fun run() {
            if (!gameOver) update()
            invalidate()
            postDelayed(this, 33)
        }
    }

    private fun update() {
        frameCount++

        if (showingOrganBanner) {
            organBannerTimer += 33
            if (organBannerTimer > 2000) showingOrganBanner = false
            return
        }

        if (collectedThisLevel >= COLLECT_TARGET) {
            advanceOrgan()
            return
        }

        movePlayer()
        applyOrganForces()
        updateMitochondria()
        updateImmuneCells()
        updateNeuralCells()
        updateStunPulses()
        checkCollisions()
    }

    private fun movePlayer() {
        if (!touchActive) return
        val dx = targetX - playerX
        val dy = targetY - playerY
        val dist = sqrt(dx * dx + dy * dy)
        if (dist < 3f) return

        val speed = minOf(dist * 0.15f, PLAYER_SPEED)
        val moveX = (dx / dist * speed)
        val moveY = (dy / dist * speed)

        var newX = (playerX + moveX).coerceIn(PLAYER_RADIUS, WORLD_W - PLAYER_RADIUS)
        var newY = (playerY + moveY).coerceIn(PLAYER_RADIUS, WORLD_H - PLAYER_RADIUS)

        when {
            !isBlocked(newX, newY, PLAYER_RADIUS) -> {
                playerX = newX
                playerY = newY
            }
            !isBlocked(newX, playerY, PLAYER_RADIUS) -> playerX = newX
            !isBlocked(playerX, newY, PLAYER_RADIUS) -> playerY = newY
        }
    }

    private fun applyOrganForces() {
        when (currentOrgan) {
            Organ.LUNGS -> {
                windTimer++
                if (windTimer > 90) {
                    windAngle += (random.nextFloat() - 0.5f) * 0.8f
                    windTimer = 0
                }
                val wx = cos(windAngle) * WIND_STRENGTH
                val wy = sin(windAngle) * WIND_STRENGTH

                playerX = (playerX + wx).coerceIn(PLAYER_RADIUS, WORLD_W - PLAYER_RADIUS)
                playerY = (playerY + wy).coerceIn(PLAYER_RADIUS, WORLD_H - PLAYER_RADIUS)
                targetX = (targetX + wx).coerceIn(PLAYER_RADIUS, WORLD_W - PLAYER_RADIUS)
                targetY = (targetY + wy).coerceIn(PLAYER_RADIUS, WORLD_H - PLAYER_RADIUS)

                for (m in mitochondria) {
                    if (m.collected) continue
                    m.x = (m.x + wx * 0.8f).coerceIn(CELL_RADIUS, WORLD_W - CELL_RADIUS)
                    m.y = (m.y + wy * 0.8f).coerceIn(CELL_RADIUS, WORLD_H - CELL_RADIUS)
                }
                for (ic in immuneCells) {
                    ic.x = (ic.x + wx * 0.6f).coerceIn(CELL_RADIUS, WORLD_W - CELL_RADIUS)
                    ic.y = (ic.y + wy * 0.6f).coerceIn(CELL_RADIUS, WORLD_H - CELL_RADIUS)
                }
                for (nc in neuralCells) {
                    nc.x = (nc.x + wx * 0.5f).coerceIn(CELL_RADIUS, WORLD_W - CELL_RADIUS)
                    nc.y = (nc.y + wy * 0.5f).coerceIn(CELL_RADIUS, WORLD_H - CELL_RADIUS)
                }
            }
            Organ.BLOODSTREAM -> {
                val currentAngle = (frameCount * 0.005f) % (PI.toFloat() * 2).let { it }
                val cx = cos(0.785f) * CURRENT_STRENGTH
                val cy = sin(0.785f + sin(frameCount * 0.01f) * 0.3f) * CURRENT_STRENGTH

                playerX = (playerX + cx).coerceIn(PLAYER_RADIUS, WORLD_W - PLAYER_RADIUS)
                playerY = (playerY + cy).coerceIn(PLAYER_RADIUS, WORLD_H - PLAYER_RADIUS)
                targetX = (targetX + cx).coerceIn(PLAYER_RADIUS, WORLD_W - PLAYER_RADIUS)
                targetY = (targetY + cy).coerceIn(PLAYER_RADIUS, WORLD_H - PLAYER_RADIUS)

                for (m in mitochondria) {
                    if (m.collected) continue
                    m.x = (m.x + cx * 1.2f).coerceIn(CELL_RADIUS, WORLD_W - CELL_RADIUS)
                    m.y = (m.y + cy * 1.2f).coerceIn(CELL_RADIUS, WORLD_H - CELL_RADIUS)
                }
                for (ic in immuneCells) {
                    ic.x = (ic.x + cx * 0.8f).coerceIn(CELL_RADIUS, WORLD_W - CELL_RADIUS)
                    ic.y = (ic.y + cy * 0.8f).coerceIn(CELL_RADIUS, WORLD_H - CELL_RADIUS)
                }
                for (nc in neuralCells) {
                    nc.x = (nc.x + cx * 0.7f).coerceIn(CELL_RADIUS, WORLD_W - CELL_RADIUS)
                    nc.y = (nc.y + cy * 0.7f).coerceIn(CELL_RADIUS, WORLD_H - CELL_RADIUS)
                }
            }
            Organ.BRAIN -> {}
        }
    }

    private fun updateMitochondria() {
        for (m in mitochondria) {
            if (m.collected) continue
            m.driftTimer++
            if (m.driftTimer >= m.driftInterval) {
                m.driftAngle = random.nextFloat() * PI.toFloat() * 2f
                m.driftTimer = 0
                m.driftInterval = 40 + random.nextInt(80)
            }
            m.x = (m.x + cos(m.driftAngle) * CELL_DRIFT_SPEED).coerceIn(CELL_RADIUS, WORLD_W - CELL_RADIUS)
            m.y = (m.y + sin(m.driftAngle) * CELL_DRIFT_SPEED).coerceIn(CELL_RADIUS, WORLD_H - CELL_RADIUS)
        }
    }

    private fun updateImmuneCells() {
        for (ic in immuneCells) {
            ic.stunnedUntil -= 33
            ic.idleTimer++

            val dx = playerX - ic.x
            val dy = playerY - ic.y
            val distToPlayer = sqrt(dx * dx + dy * dy)

            if (ic.stunnedUntil > 0) {
                ic.chasing = false
                continue
            }

            if (distToPlayer < IMMUNE_CHASE_RADIUS && distToPlayer > 0.5f) {
                ic.chasing = true
                ic.x += (dx / distToPlayer * IMMUNE_SPEED * 1.6f)
                ic.y += (dy / distToPlayer * IMMUNE_SPEED * 1.6f)
            } else {
                ic.chasing = false
                if (ic.idleTimer >= ic.idleInterval) {
                    ic.idleAngle = random.nextFloat() * PI.toFloat() * 2f
                    ic.idleTimer = 0
                    ic.idleInterval = 50 + random.nextInt(100)
                }
                ic.x += cos(ic.idleAngle) * CELL_DRIFT_SPEED * 0.7f
                ic.y += sin(ic.idleAngle) * CELL_DRIFT_SPEED * 0.7f
            }

            ic.x = ic.x.coerceIn(CELL_RADIUS, WORLD_W - CELL_RADIUS)
            ic.y = ic.y.coerceIn(CELL_RADIUS, WORLD_H - CELL_RADIUS)
        }
    }

    private fun updateNeuralCells() {
        for (nc in neuralCells) {
            nc.pulseAnimTimer = (nc.pulseAnimTimer + 1) % 120
            nc.driftTimer++
            if (nc.driftTimer >= nc.driftInterval) {
                nc.driftAngle = random.nextFloat() * PI.toFloat() * 2f
                nc.driftTimer = 0
                nc.driftInterval = 60 + random.nextInt(120)
            }
            nc.x = (nc.x + cos(nc.driftAngle) * CELL_DRIFT_SPEED * 0.5f)
                .coerceIn(CELL_RADIUS, WORLD_W - CELL_RADIUS)
            nc.y = (nc.y + sin(nc.driftAngle) * CELL_DRIFT_SPEED * 0.5f)
                .coerceIn(CELL_RADIUS, WORLD_H - CELL_RADIUS)
        }
    }

    private fun updateStunPulses() {
        val iter = stunPulses.iterator()
        while (iter.hasNext()) {
            val sp = iter.next()
            sp.elapsed += 33
            if (sp.elapsed >= sp.duration) {
                iter.remove()
                continue
            }
            val progress = sp.elapsed.toFloat() / sp.duration
            val currentRadius = sp.maxRadius * sqrt(progress)
            for (ic in immuneCells) {
                if (ic.stunnedUntil > 0) continue
                val edx = ic.x - sp.x
                val edy = ic.y - sp.y
                val edist = sqrt(edx * edx + edy * edy)
                if (edist <= currentRadius) {
                    ic.stunnedUntil = STUN_DURATION_MS
                    ic.chasing = false
                }
            }
        }
    }

    private fun checkCollisions() {
        val mitoIter = mitochondria.iterator()
        while (mitoIter.hasNext()) {
            val m = mitoIter.next()
            if (m.collected) {
                mitoIter.remove()
                continue
            }
            val dx = playerX - m.x
            val dy = playerY - m.y
            val dist = sqrt(dx * dx + dy * dy)
            if (dist < PLAYER_RADIUS + CELL_RADIUS) {
                m.collected = true
                score++
                collectedThisLevel++
                scoreCallback(score)

                spawnCollectEffect(m.x, m.y)

                if (collectedThisLevel >= COLLECT_TARGET) {
                    advanceOrgan()
                    return
                }
            }
        }

        for (ic in immuneCells) {
            if (ic.stunnedUntil > 0) continue
            val dx = playerX - ic.x
            val dy = playerY - ic.y
            val dist = sqrt(dx * dx + dy * dy)
            if (dist < PLAYER_RADIUS + CELL_RADIUS) {
                gameOver = true
                return
            }
        }
    }

    private fun spawnCollectEffect(x: Float, y: Float) {
        for (i in 0 until 4) {
            val angle = (PI * 2 * i / 4).toFloat()
            val dist = 30f
            mitochondria.add(Mitochondria(
                x + cos(angle) * dist,
                y + sin(angle) * dist,
                collected = true
            ))
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val screenCenterX = width / 2f
        val screenCenterY = height / 2f
        val offsetX = screenCenterX - playerX
        val offsetY = screenCenterY - playerY

        drawGrid(canvas, offsetX, offsetY)

        if (currentOrgan == Organ.BRAIN) {
            for (wall in walls) {
                val wLeft = wall.left + offsetX
                val wTop = wall.top + offsetY
                val wRight = wall.right + offsetX
                val wBottom = wall.bottom + offsetY
                if (wRight > -100 && wLeft < width + 100 && wBottom > -100 && wTop < height + 100) {
                    canvas.drawRect(wLeft, wTop, wRight, wBottom, wallPaint)
                    canvas.drawRect(wLeft, wTop, wRight, wBottom, wallEdgePaint)
                }
            }
        }

        drawOrganIndicator(canvas, offsetX, offsetY)

        for (m in mitochondria) {
            if (m.collected) continue
            val sx = m.x + offsetX
            val sy = m.y + offsetY
            if (sx > -100 && sx < width + 100 && sy > -100 && sy < height + 100) {
                val scale = 1f + 0.06f * sin((frameCount + m.x.toInt() + m.y.toInt()) * 0.1f)
                canvas.drawCircle(sx, sy, (CELL_RADIUS + 4) * scale, mitoGlowPaint)
                canvas.drawCircle(sx, sy, CELL_RADIUS * scale, mitoPaint)
            }
        }

        for (ic in immuneCells) {
            val sx = ic.x + offsetX
            val sy = ic.y + offsetY
            if (sx > -100 && sx < width + 100 && sy > -100 && sy < height + 100) {
                val basePaint = when {
                    ic.stunnedUntil > 0 -> immuneStunPaint
                    ic.chasing -> immuneAlertPaint
                    else -> immunePaint
                }
                val radius = CELL_RADIUS * if (ic.chasing && ic.stunnedUntil <= 0) {
                    1f + 0.1f * sin(frameCount * 0.15f)
                } else 1f
                canvas.drawCircle(sx, sy, radius + 4, immuneGlowPaint)
                canvas.drawCircle(sx, sy, radius, basePaint)
                if (ic.stunnedUntil > 0) {
                    stunPaint.alpha = 100
                    canvas.drawCircle(sx, sy, radius + 3, stunPaint)
                }
            }
        }

        for (nc in neuralCells) {
            val sx = nc.x + offsetX
            val sy = nc.y + offsetY
            if (sx > -100 && sx < width + 100 && sy > -100 && sy < height + 100) {
                val pulseScale = 1f + 0.15f * sin(nc.pulseAnimTimer.toFloat() / 120f * PI.toFloat() * 2f)
                neuralGlowPaint.alpha = ((0.4f + 0.2f * pulseScale) * 255).toInt()
                canvas.drawCircle(sx, sy, (CELL_RADIUS + 6) * pulseScale, neuralGlowPaint)
                neuralGlowPaint.alpha = 102
                canvas.drawCircle(sx, sy, CELL_RADIUS * pulseScale, neuralPaint)
                canvas.drawCircle(sx, sy, NEURAL_TAP_RADIUS, Paint().apply {
                    color = 0x118888FF.toInt(); style = Paint.Style.FILL
                })
            }
        }

        for (sp in stunPulses) {
            val progress = (sp.elapsed.toFloat() / sp.duration).coerceIn(0f, 1f)
            val currentRadius = sp.maxRadius * sqrt(progress)
            val sx = sp.x + offsetX
            val sy = sp.y + offsetY
            if (sx + currentRadius > -100 && sx - currentRadius < width + 100 &&
                sy + currentRadius > -100 && sy - currentRadius < height + 100) {
                val alpha = ((1f - progress) * 0.35f).coerceIn(0f, 0.4f)
                stunPaint.alpha = (alpha * 255).toInt()
                canvas.drawCircle(sx, sy, currentRadius, stunPaint)
                if (currentRadius > 10) {
                    stunRingPaint.alpha = (alpha * 1.5f * 255).coerceIn(0, 255).toInt()
                    canvas.drawCircle(sx, sy, currentRadius, stunRingPaint)
                }
            }
        }

        val px = playerX + offsetX
        val py = playerY + offsetY
        canvas.drawCircle(px, py, PLAYER_RADIUS + 8, playerGlowPaint)
        canvas.drawCircle(px, py, PLAYER_RADIUS, playerPaint)
        canvas.drawCircle(px, py, PLAYER_RADIUS, playerOutlinePaint)

        val eyeOffset = PLAYER_RADIUS * 0.4f
        val eyeRadius = 3f
        val eyePaint = Paint().apply { color = 0xFF0A0A1A.toInt(); style = Paint.Style.FILL }
        canvas.drawCircle(px - eyeOffset * 0.7f, py - eyeOffset * 0.6f, eyeRadius, eyePaint)
        canvas.drawCircle(px + eyeOffset * 0.7f, py - eyeOffset * 0.6f, eyeRadius, eyePaint)

        if (touchActive) {
            val tx = targetX + offsetX
            val ty = targetY + offsetY
            if (abs(tx - px) > 5 || abs(ty - py) > 5) {
                targetFillPaint.alpha = 40
                canvas.drawCircle(tx, ty, 10f, targetFillPaint)
                canvas.drawCircle(tx, ty, 10f, targetPaint)
                val trailPaint = Paint(targetPaint).apply { alpha = 60 }
                canvas.drawLine(px, py, tx, ty, trailPaint)
            }
        }

        val hudY = 50f
        val barWidth = 220f
        val barX = width / 2f - barWidth / 2
        val barHeight = 14f
        val collectProgress = (collectedThisLevel.toFloat() / COLLECT_TARGET).coerceIn(0f, 1f)

        canvas.drawRoundRect(barX - 2, hudY - 2, barX + barWidth + 2, hudY + barHeight + 2, 8f, 8f,
            Paint().apply { color = 0x55000000.toInt(); style = Paint.Style.FILL })
        canvas.drawRoundRect(barX, hudY, barX + barWidth, hudY + barHeight, 7f, 7f, progressBgPaint)
        canvas.drawRoundRect(
            barX, hudY,
            barX + barWidth * collectProgress, hudY + barHeight,
            7f, 7f, progressPaint
        )

        canvas.drawText(
            "${collectedThisLevel}/$COLLECT_TARGET",
            width / 2f, hudY - 10,
            hudTextPaint
        )

        val organTag = when (currentOrgan) {
            Organ.BRAIN -> "[ Maze ]"
            Organ.LUNGS -> "[ Wind ]"
            Organ.BLOODSTREAM -> "[ Flow ]"
        }
        organTagPaint.color = when (currentOrgan) {
            Organ.BRAIN -> 0xFF8888AA.toInt()
            Organ.LUNGS -> 0xFF88AACC.toInt()
            Organ.BLOODSTREAM -> 0xFFAA8866.toInt()
        }
        canvas.drawText(organTag, 16f, height - 20f, organTagPaint)

        canvas.drawText(
            "Score: $score",
            width - 16f, height - 20f,
            organTagPaint.apply { textAlign = Paint.Align.RIGHT }
        )

        if (showingOrganBanner) {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bannerOverlayPaint)
            val name = when (currentOrgan) {
                Organ.BRAIN -> "BRAIN"
                Organ.LUNGS -> "LUNGS"
                Organ.BLOODSTREAM -> "BLOODSTREAM"
            }
            val subtitle = when (currentOrgan) {
                Organ.BRAIN -> "Navigate the neural maze"
                Organ.LUNGS -> "Ride the respiratory wind"
                Organ.BLOODSTREAM -> "Fight the blood current"
            }
            bannerTextPaint.textSize = 40f
            canvas.drawText("Entering $name", width / 2f, height / 2f - 12, bannerTextPaint)
            bannerTextPaint.textSize = 18f
            bannerTextPaint.color = 0xFF8899AA.toInt()
            canvas.drawText(subtitle, width / 2f, height / 2f + 36, bannerTextPaint)
            bannerTextPaint.color = 0xFF33FF66.toInt()
        }

        if (gameOver) {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), gameOverOverlayPaint)
            canvas.drawText("Infected!", width / 2f, height / 2f - 30, goTextPaint)
            goSubTextPaint.textSize = 24f
            canvas.drawText("Score: $score", width / 2f, height / 2f + 16, goSubTextPaint)
            goSubTextPaint.textSize = 22f
            goSubTextPaint.color = 0xFF888899.toInt()
            canvas.drawText("Tap to Restart", width / 2f, height / 2f + 52, goSubTextPaint)
            goSubTextPaint.color = 0xFFCCCCCC.toInt()
        }
    }

    private fun drawGrid(canvas: Canvas, offsetX: Float, offsetY: Float) {
        val gridSize = 80f
        val startX = (-offsetX % gridSize + gridSize) % gridSize
        val startY = (-offsetY % gridSize + gridSize) % gridSize
        var x = startX
        while (x < width) {
            canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint)
            x += gridSize
        }
        var y = startY
        while (y < height) {
            canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
            y += gridSize
        }
    }

    private fun drawOrganIndicator(canvas: Canvas, _offsetX: Float, _offsetY: Float) {
        val margin = 20f
        val size = 16f
        val indicatorBgPaint = Paint().apply {
            color = 0x44000000.toInt()
            style = Paint.Style.FILL
        }
        val indicatorFgPaint = Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        canvas.drawRoundRect(margin - 4, margin - 4, margin + size + 8, margin + size + 8, 6f, 6f, indicatorBgPaint)

        when (currentOrgan) {
            Organ.BRAIN -> {
                indicatorFgPaint.color = 0x772A1A3A.toInt()
                canvas.drawRect(margin + 2, margin + 2, margin + size + 6, margin + size + 6, indicatorFgPaint)
            }
            Organ.LUNGS -> {
                indicatorFgPaint.color = 0x77CCDDEE.toInt()
                canvas.drawCircle(margin + size / 2 + 4, margin + size / 2 + 4, size / 2 + 1, indicatorFgPaint)
            }
            Organ.BLOODSTREAM -> {
                indicatorFgPaint.color = 0x77FF6644.toInt()
                val path = Path()
                path.moveTo(margin + 3, margin + size / 2 + 3)
                path.lineTo(margin + size + 6, margin + 4)
                path.lineTo(margin + size + 6, margin + size + 4)
                path.lineTo(margin + 3, margin + size / 2 + 4)
                path.close()
                canvas.drawPath(path, indicatorFgPaint)
            }
        }
    }

    fun restart() {
        currentOrgan = Organ.BRAIN
        score = 0
        scoreCallback(0)
        generateLevel()
        updateOrganCallback()
        invalidate()
    }
}

data class Mitochondria(
    var x: Float, var y: Float,
    var collected: Boolean = false,
    var driftAngle: Float = (Math.random() * PI * 2).toFloat(),
    var driftTimer: Int = 0,
    var driftInterval: Int = (30..80).random()
)

data class ImmuneCell(
    var x: Float, var y: Float,
    var stunnedUntil: Int = 0,
    var chasing: Boolean = false,
    var idleAngle: Float = (Math.random() * PI * 2).toFloat(),
    var idleTimer: Int = 0,
    var idleInterval: Int = (40..100).random()
)

data class NeuralCell(
    var x: Float, var y: Float,
    var driftAngle: Float = (Math.random() * PI * 2).toFloat(),
    var driftTimer: Int = 0,
    var driftInterval: Int = (60..150).random(),
    var pulseAnimTimer: Int = (0..120).random()
)

data class StunPulse(
    val x: Float, val y: Float,
    val maxRadius: Float, val duration: Int,
    var elapsed: Int = 0
)

data class WallRect(
    val left: Float, val top: Float,
    val right: Float, val bottom: Float
)

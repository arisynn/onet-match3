package game.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import game.OnetMatch3Game
import game.assets.SoundCue
import game.assets.TileAssetResolver
import game.cosmetics.*
import game.gameplay.onet.*
import game.gameplay.shared.SeededRandom
import game.gameplay.triplematch.*
import game.missions.MissionStatus
import kotlin.math.min
import kotlin.math.sin
import java.io.IOException

/** All hit areas are the same world-space rectangles used to draw controls. */
abstract class CeramicScreen(game: OnetMatch3Game) : BaseScreen(game) {
    override val responsiveLayout = true
    val worldWidth: Float get() = viewport.worldWidth
    val worldHeight: Float get() = viewport.worldHeight
    val backBounds get() = Rectangle(16f, worldHeight - 90f, 64f, 64f)
    private var notice = ""
    private var noticeTime = 0f
    protected var elapsed = 0f
    init {
        backgroundColor = Color.valueOf("F2F4EA")
        addInputProcessor(object : InputAdapter() {
            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                if (pointer != 0) return false
                val p = inputPoint(screenX, screenY)
                return try { tapped(p.x, p.y) } catch (_: IOException) { toast("Unable to save. Please free some storage."); true }
            }
        })
    }
    private var inputStage: com.badlogic.gdx.scenes.scene2d.Stage? = null
    private var touchSurface: com.badlogic.gdx.scenes.scene2d.Actor? = null
    override fun show() {
        super.show()
        // Scene2D owns touch focus: a release belongs to the actor that received its press.
        // The shared renderer draws ceramic controls using the same world coordinates.
        if (Gdx.gl != null && inputStage == null) {
            val stage = com.badlogic.gdx.scenes.scene2d.Stage(viewport)
            val surface = com.badlogic.gdx.scenes.scene2d.Actor()
            surface.setBounds(0f, 0f, worldWidth, worldHeight)
            surface.addListener(object : com.badlogic.gdx.scenes.scene2d.InputListener() {
                override fun touchDown(event: com.badlogic.gdx.scenes.scene2d.InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean = pointer == 0
                override fun touchUp(event: com.badlogic.gdx.scenes.scene2d.InputEvent, x: Float, y: Float, pointer: Int, button: Int) {
                    if (pointer == 0 && x in 0f..worldWidth && y in 0f..worldHeight) tapped(x, y)
                }
            })
            stage.addActor(surface)
            inputStage = stage; touchSurface = surface
            inputMultiplexer.addProcessor(0, stage)
        }
    }
    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        touchSurface?.setBounds(0f, 0f, worldWidth, worldHeight)
    }
    override fun onDispose() { inputStage?.dispose(); inputStage = null; touchSurface = null }
    override fun actionBounds(id: String): Rectangle? = if (id == "back") backBounds else null
    protected open fun tapped(x: Float, y: Float): Boolean = false
    override fun update(delta: Float) { elapsed += delta; noticeTime = (noticeTime - delta).coerceAtLeast(0f) }
    protected fun persist(action: () -> Unit) {
        try { action() } catch (_: IOException) { toast("Your changes could not be saved. Check storage.") }
    }
    protected fun toast(value: String) { notice = value; noticeTime = 2.5f }
    protected fun attempt(success: String, action: () -> Unit) {
        try { action(); toast(success) } catch (_: IOException) { toast("Unable to save. Please check storage.") } catch (_: IllegalArgumentException) { toast("Not available yet") }
        catch (_: IllegalStateException) { toast("Complete the goal first") }
        catch (_: game.economy.InsufficientFundsException) { toast("Earn more Gems through your goals") }
    }
    protected fun go(type: ScreenType) { game.screenManager.setScreen(type, FadeTransition(if (game.settings.reducedMotion) 0f else 0.22f, .91f, .94f, .88f)) }
    protected fun ScreenUi.top(title: String, subtitle: String = "", back: Boolean = true) {
        if (back) iconButton(backBounds, "back")
        heading(title, 80f, height - 43f, 34f, width - 160f)
        if (subtitle.isNotEmpty()) text(subtitle, 30f, height - 84f, .86f, muted, width - 60f)
    }
    protected fun ScreenUi.notifications() {
        if (noticeTime > 0f) {
            panel(Rectangle(28f, 128f, width - 56f, 52f))
            text(notice, 40f, 161f, .88f, ink, width - 80f)
        }
    }
    protected fun mainButton(y: Float) = Rectangle(64f, y, worldWidth - 128f, 66f)
    protected fun ScreenUi.control(label: String, rect: Rectangle, enabled: Boolean = true, primary: Boolean = false) =
        button(label, rect.x, rect.y, rect.width, rect.height, enabled, primary)
    protected fun start(mode: ScreenType, level: Int = game.selectedLevel) {
        game.startNewRound(mode, level); go(mode)
    }
}

class BootScreen(game: OnetMatch3Game) : CeramicScreen(game) {
    override fun update(delta: Float) { super.update(delta); if (elapsed >= .45f) go(ScreenType.LOADING) }
    override fun draw(delta: Float) { ui {
        art("onet", width / 2f - 72f, height / 2f + 20f, 144f, 144f)
        heading("Onet & Triple", 20f, height / 2f - 20f, 48f, width - 40f)
        text("A little moment of calm", 20f, height / 2f - 66f, 1f, muted, width - 40f)
    } }
}
class LoadingScreen(game: OnetMatch3Game) : CeramicScreen(game) {
    private val progress: Float get() = runCatching { game.assetService.progress }.getOrDefault(1f)
    override fun update(delta: Float) {
        super.update(delta)
        if (elapsed >= .45f && runCatching { game.assetService.isFinished }.getOrDefault(true)) go(ScreenType.MAIN_MENU)
    }
    override fun draw(delta: Float) { ui {
        art("succulent", width / 2f - 90f, height / 2f + 10f, 180f, 150f)
        heading("Make room for joy", 20f, height / 2f - 22f, 36f, width - 40f)
        rounded(100f, height / 2f - 78f, width - 200f, 7f, 4f, Color.valueOf("DEE5D6"))
        rounded(100f, height / 2f - 78f, (width - 200f) * min(progress, elapsed / .45f), 7f, 4f, sage)
    } }
}
class MainMenuScreen(game: OnetMatch3Game) : CeramicScreen(game) {
    override fun actionBounds(id: String): Rectangle? = when(id) {
        "play" -> playBounds; "resume_game" -> resumeBounds; "missions" -> mission; "collection" -> collection; "profile" -> profile; "settings" -> settings; else -> super.actionBounds(id)
    }

    private fun hasSavedRound() = game.resumedOnet() != null || game.resumedTriple() != null
    val resumeBounds get() = Rectangle(64f, worldHeight * .43f + 82f, worldWidth - 128f, 54f)
    val playBounds get() = mainButton(worldHeight * .43f)
    private val mission get() = mainButton(worldHeight * .43f - 84f)
    private val collection get() = mainButton(worldHeight * .43f - 168f)
    private val profile get() = Rectangle(64f, 66f, (worldWidth - 144f) / 2f, 58f)
    private val settings get() = Rectangle(worldWidth / 2f + 8f, 66f, (worldWidth - 144f) / 2f, 58f)
    override fun tapped(x: Float, y: Float): Boolean {
        when {
            resumeBounds.contains(x,y) && hasSavedRound() -> go(game.activeGameType)
            playBounds.contains(x,y) -> go(ScreenType.MODE_SELECT)
            mission.contains(x,y) -> go(ScreenType.MISSION_CENTER)
            collection.contains(x,y) -> go(ScreenType.COSMETIC_COLLECTION)
            profile.contains(x,y) -> go(ScreenType.PLAYER_PROFILE)
            settings.contains(x,y) -> go(ScreenType.SETTINGS)
            else -> return false
        }; return true
    }
    override fun draw(delta: Float) { ui {
        text("YOUR DAILY LITTLE ESCAPE", 20f, height - 66f, .75f, muted, width - 40f)
        heading("Onet & Triple", 20f, height - 135f, 52f, width - 40f)
        text("Little tiles. Lovely moments.", 20f, height - 186f, 1f, muted, width - 40f)
        val center = (height - 220f + playBounds.y + playBounds.height) / 2f
        tile("candy_2", width / 2f - 108f, center - 45f, 100f, 106f)
        tile("ocean_0", width / 2f + 8f, center - 30f, 100f, 106f)
        if (hasSavedRound()) control("Continue your puzzle", resumeBounds)
        control("Let's play", playBounds, primary = true)
        control("Missions & achievements", mission)
        control("Tile collection", collection)
        control("My journey", profile); control("Settings", settings)
        text("${game.economyService.gemBalance} Gems  •  Level ${game.playerProfile.level}", 20f, 36f, .8f, muted, width - 40f)
    } }
}
private var selectedMode = ScreenType.ONET_GAME

class ModeSelectScreen(game: OnetMatch3Game) : CeramicScreen(game) {
    override fun actionBounds(id: String): Rectangle? = when(id) { "onet" -> onetBounds; "triple" -> tripleBounds; else -> super.actionBounds(id) }

    val onetBounds get() = Rectangle(34f, worldHeight / 2f + 30f, worldWidth - 68f, 230f)
    val tripleBounds get() = Rectangle(34f, worldHeight / 2f - 235f, worldWidth - 68f, 230f)
    override fun tapped(x: Float, y: Float): Boolean {
        if (backBounds.contains(x,y)) { go(ScreenType.MAIN_MENU); return true }
        val mode = when { onetBounds.contains(x,y) -> ScreenType.ONET_GAME; tripleBounds.contains(x,y) -> ScreenType.TRIPLE_MATCH_GAME; else -> return false }
        selectedMode = mode; go(ScreenType.LEVEL_SELECT); return true
    }
    override fun draw(delta: Float) { ui {
        top("Choose your calm", "Two puzzles, one lovely collection")
        for ((rect, title, subtitle, artId) in listOf(
            listOf(onetBounds,"Onet Connect","Find the path between matching pairs","onet"),
            listOf(tripleBounds,"Triple Match","Collect three. Keep your tray clear.","triple"))) {
            val r = rect as Rectangle
            panel(r); art(artId as String, r.x + 24f, r.y + 105f, 92f, 92f)
            heading(title as String, r.x + 132f, r.y + 175f, 31f)
            text(if (artId == "onet") "${game.unlockedLevel(ScreenType.ONET_GAME)} levels reached" else "${game.unlockedLevel(ScreenType.TRIPLE_MATCH_GAME)} levels reached", r.x + 132f, r.y + 130f, .88f, muted)
            text(subtitle as String, r.x + 24f, r.y + 68f, .94f, muted)
            text("Choose a level  →", r.x + 24f, r.y + 34f, .9f, sage)
        }
    } }
}
class LevelSelectScreen(game: OnetMatch3Game) : CeramicScreen(game) {
    override fun actionBounds(id: String): Rectangle? = if (id.startsWith("level_")) id.substringAfter('_').toIntOrNull()?.let { level -> val index = level - page * 20 - 1; if(index in 0..19) levelBounds(index) else null } else super.actionBounds(id)

    var page = 0
    val previousBounds get() = Rectangle(36f, 56f, 130f, 58f)
    val nextBounds get() = Rectangle(worldWidth - 166f, 56f, 130f, 58f)
    fun levelBounds(index: Int): Rectangle {
        val step = (worldWidth - 64f) / 4f
        val cell = min(94f, step - 12f)
        return Rectangle(32f + (index % 4) * step + (step - cell) / 2f, worldHeight - 265f - (index / 4) * 112f, cell, 88f)
    }
    override fun tapped(x: Float, y: Float): Boolean {
        if (backBounds.contains(x,y)) { go(ScreenType.MODE_SELECT); return true }
        if (previousBounds.contains(x,y) && page > 0) { page--; return true }
        if (nextBounds.contains(x,y) && (page + 1) * 20 < game.unlockedLevel(selectedMode) + 20) { page++; return true }
        for (i in 0 until 20) if (levelBounds(i).contains(x,y)) {
            val level = page * 20 + i + 1
            if (level <= game.unlockedLevel(selectedMode)) start(selectedMode, level) else toast("Finish the previous puzzle to unlock")
            return true
        }
        return false
    }
    override fun draw(delta: Float) { ui {
        top(if (selectedMode == ScreenType.ONET_GAME) "Onet journey" else "Triple journey", "A little progress, one puzzle at a time")
        for (i in 0 until 20) {
            val level = page * 20 + i + 1; val unlocked = level <= game.unlockedLevel(selectedMode)
            control(if (unlocked) "$level" else "·", levelBounds(i), unlocked, level == game.unlockedLevel(selectedMode))
            if (game.bestScore(selectedMode,level) > 0) text("completed", levelBounds(i).x, levelBounds(i).y - 10f, .57f, sage, levelBounds(i).width)
        }
        control("Previous", previousBounds, page > 0); control("Next", nextBounds)
        text("${page * 20 + 1} – ${(page + 1) * 20}", 170f, 92f, .9f, muted, width - 340f)
        notifications()
    } }
}

class OnetGameScreen(game: OnetMatch3Game) : CeramicScreen(game) {
    override fun actionBounds(id: String): Rectangle? = when(id) { "pause" -> puzzleLayout.pause; "hint" -> puzzleLayout.hint; "shuffle" -> puzzleLayout.shuffle; else -> super.actionBounds(id) }

    val puzzleLayout get() = PuzzleLayout(worldWidth, worldHeight)
    var roundState = game.resumedOnet() ?: createOnetLevelRound(game.selectedTheme, game.selectedLevel, SeededRandom("onet_${game.selectedLevel}"))
    private var activePath = roundState.pending?.path
    private var pathTimer = if (activePath == null) 0f else .35f
    private var accumulator = 0f
    private var hint: Hint? = null
    private var hintsUsed = if (game.resumedOnet() != null) game.snapshot().roundHints else 0
    private var shufflesUsed = if (game.resumedOnet() != null) game.snapshot().roundShuffles else 0
    private var finished = false
    private var settlementRetry = 0f
    private var invalidFlash = 0f
    // Terminal outcomes are committed only by completeOnet's atomic transaction.
    // Leaving the last playable snapshot lets an interrupted settlement replay safely.
    private fun checkpoint() { if (roundState.phase == "playing") persist { game.captureOnet(roundState,hintsUsed,shufflesUsed) } }
    override fun onScreenShown() { game.activeGameType = ScreenType.ONET_GAME }
    override fun pause() { roundState = pauseOnetRound(roundState,true); checkpoint() }
    override fun resume() { roundState = pauseOnetRound(roundState,false) }
    override fun tapped(x: Float, y: Float): Boolean {
        val layout = puzzleLayout
        if (layout.pause.contains(x,y) || layout.back.contains(x,y)) {
            pause(); game.screenManager.pushScreen(ScreenType.PAUSE, FadeTransition(.18f,.91f,.94f,.88f)); return true
        }
        if (roundState.pending != null || roundState.phase != "playing") return false
        when {
            layout.hint.contains(x,y) -> { hint=hintFor(roundState); if (hint != null) hintsUsed++; return true }
            layout.shuffle.contains(x,y) -> { roundState=shuffleRound(roundState,SeededRandom(System.nanoTime())); shufflesUsed++; hint=null; game.soundService.feedback(SoundCue.SHUFFLE); checkpoint(); return true }
        }
        val (r,c) = layout.onetHit(x,y) ?: return false
        val previous = roundState
        roundState=selectTile(roundState,r,c,System.currentTimeMillis()); hint=null
        if (roundState.pending != null && previous.pending == null) {
            activePath=roundState.pending!!.path; pathTimer=.36f
            game.soundService.feedback(if (roundState.combo > 1) SoundCue.COMBO else SoundCue.MATCH)
        } else if (roundState.event?.type == "blocked") { invalidFlash=.3f; game.soundService.feedback(SoundCue.INVALID) }
        else game.soundService.feedback(SoundCue.SELECT)
        checkpoint(); return true
    }
    override fun update(delta: Float) {
        super.update(delta); settlementRetry=(settlementRetry-delta).coerceAtLeast(0f); invalidFlash=(invalidFlash-delta).coerceAtLeast(0f)
        if (roundState.paused || finished) return
        if (roundState.pending != null) {
            pathTimer-=delta
            if (pathTimer<=0f) { roundState=resolveMatch(roundState); activePath=null; if (needsShuffle(roundState)) { roundState=shuffleRound(roundState); toast("A fresh arrangement for your next match") }; checkpoint() }
        }
        accumulator+=delta
        if (accumulator>=1f) { val seconds=accumulator.toInt(); accumulator-=seconds; roundState=tickOnetRound(roundState,seconds); checkpoint() }
        if (roundState.phase in listOf("won","lost") && settlementRetry <= 0f) {
            try {
                game.completeOnet(roundState,hintsUsed,shufflesUsed)
                finished=true
                game.soundService.feedback(if (roundState.phase=="won") SoundCue.VICTORY else SoundCue.DEFEAT)
                go(if (roundState.phase=="won") ScreenType.VICTORY else ScreenType.DEFEAT)
            } catch (_: IOException) { settlementRetry=2f; toast("Unable to save your result. Please check storage.") }
        }
    }
    override fun draw(delta: Float) { ui {
        val layout=puzzleLayout
        gameHeader("Onet Connect", "LEVEL ${game.selectedLevel}  •  ${game.selectedTheme.replaceFirstChar { it.uppercase() }}",roundState.score,roundState.time,roundState.duration,layout)
        for (r in 1..10) for (c in 1..6) {
            val cell=roundState.board[r][c] ?: continue
            if (cell.isSlave) continue
            val size=groupSize(cell)
            val selected=roundState.selected==Point(r,c) || hint?.let { it.p1==Point(r,c)||it.p2==Point(r,c) }==true
            val rect=layout.onetCell(r,c,size.rows,size.cols)
            val matching=roundState.pending?.let { it.a==Point(r,c)||it.b==Point(r,c) }==true
            if (!game.settings.reducedMotion && matching) {
                val scale=.82f+.18f*(pathTimer/.36f).coerceIn(0f,1f)
                val oldW=rect.width;val oldH=rect.height;rect.width*=scale;rect.height*=scale
                rect.x+=(oldW-rect.width)/2f;rect.y+=(oldH-rect.height)/2f
            } else if (!game.settings.reducedMotion && selected) rect.y+=sin(elapsed*7f)*1.5f
            tile(cell.id,rect,selected)
            cell.openSide?.let { side ->
                val arrow=when(side){"up"->"^";"down"->"v";"left"->"<";else->">"}
                text(arrow,rect.x+7f,rect.y+15f,.65f,sage)
            }
        }
        activePath?.zipWithNext()?.forEach { (a,b) -> val p=layout.onetPoint(a.r,a.c);val q=layout.onetPoint(b.r,b.c);line(p.x,p.y,q.x,q.y) }
        tool(layout.hint,"hint","Hint");tool(layout.shuffle,"shuffle","Shuffle")
        text(if(invalidFlash>0f) "That path is blocked. Try another pair." else if(game.selectedLevel==1) "Match identical tiles • paths can turn twice" else if(roundState.combo>1) "Lovely! ${roundState.combo}× combo" else "Find your next little connection",16f,29f,.75f,if(invalidFlash>0f)Color.valueOf("AF705A")else muted,width-32f)
        notifications()
    } }
}

class TripleMatchGameScreen(game: OnetMatch3Game) : CeramicScreen(game) {
    override fun actionBounds(id: String): Rectangle? = when(id) { "pause" -> puzzleLayout.pause; "undo" -> puzzleLayout.undo; else -> super.actionBounds(id) }

    val puzzleLayout get()=PuzzleLayout(worldWidth,worldHeight)
    var roundState=game.resumedTriple() ?: startTripleRound(createTripleLevelRound(TileAssetResolver.tileIdsFor(game.selectedTheme),game.selectedLevel,SeededRandom("triple_${game.selectedLevel}")))
    private var accumulator=0f
    private var finished=false
    private var settlementRetry=0f
    private var flightId: String?=null
    private var flightFrom=Rectangle()
    private var flightTo=Rectangle()
    private var flightTime=0f
    private fun checkpoint(){if(roundState.phase=="playing")persist { game.captureTriple(roundState) }}
    override fun onScreenShown(){game.activeGameType=ScreenType.TRIPLE_MATCH_GAME}
    override fun pause(){roundState=pauseTripleRound(roundState,true);checkpoint()}
    override fun resume(){roundState=pauseTripleRound(roundState,false)}
    override fun tapped(x:Float,y:Float):Boolean {
        val layout=puzzleLayout
        if(layout.pause.contains(x,y)||layout.back.contains(x,y)){pause();game.screenManager.pushScreen(ScreenType.PAUSE,FadeTransition(.18f,.91f,.94f,.88f));return true}
        if(layout.undo.contains(x,y)) {if(!roundState.undoUsed&&roundState.undo!=null){roundState=undoPick(roundState);flightId=null;game.soundService.feedback(SoundCue.UNDO);checkpoint()};return true}
        if(flightId!=null||roundState.phase!="playing")return false
        val index=layout.stackHit(x,y)?:return false
        val id=roundState.stacks[index].lastOrNull()?:return false
        flightFrom=layout.stack(index);flightTo=layout.traySlot(roundState.tray.size.coerceAtMost(6));flightTime=0f
        val previousCleared=roundState.cleared
        roundState=pickTile(roundState,index)
        flightId=if(game.settings.reducedMotion)null else id
        game.soundService.feedback(if(roundState.cleared>previousCleared)SoundCue.MATCH else SoundCue.SELECT)
        checkpoint();return true
    }
    override fun update(delta:Float){
        super.update(delta); settlementRetry=(settlementRetry-delta).coerceAtLeast(0f)
        if(roundState.paused||finished)return
        if(flightId!=null){flightTime+=delta;if(flightTime>=.19f)flightId=null}
        accumulator+=delta
        if(accumulator>=1f){val seconds=accumulator.toInt();accumulator-=seconds;roundState=tickTripleRound(roundState,seconds);checkpoint()}
        if(roundState.phase in listOf("won","lost")&&flightId==null&&settlementRetry<=0f){
            try {game.completeTriple(roundState);finished=true;game.soundService.feedback(if(roundState.phase=="won")SoundCue.VICTORY else SoundCue.DEFEAT);go(if(roundState.phase=="won")ScreenType.VICTORY else ScreenType.DEFEAT)}
            catch (_: IOException) {settlementRetry=2f;toast("Unable to save your result. Please check storage.")}
        }
    }
    override fun draw(delta:Float){ui{
        val layout=puzzleLayout
        gameHeader("Triple Match","LEVEL ${game.selectedLevel}  •  Collect a little joy",tripleScore(roundState),roundState.time,roundState.duration,layout)
        roundState.stacks.forEachIndexed { i,stack -> if(stack.isNotEmpty()){
            val r=layout.stack(i)
            if(stack.size>1)rounded(r.x+6f,r.y+7f,r.width,r.height,15f,Color.valueOf("CFD8C4"))
            tile(stack.last(),r)
            text("${stack.size} left",r.x,r.y-8f,.65f,muted,r.width)
        }}
        panel(layout.tray,Color.valueOf("E4ECDB"))
        text("YOUR TRAY",layout.tray.x+16f,layout.tray.y+87f,.62f,muted)
        text("${roundState.tray.size} / 7",layout.tray.x+layout.tray.width-70f,layout.tray.y+87f,.62f,muted)
        for(i in 0 until 7){val rect=layout.traySlot(i);slot(rect);roundState.tray.getOrNull(i)?.let{id->tile(id,rect)}}
        flightId?.let{id->val t=(flightTime/.19f).coerceIn(0f,1f);val eased=1f-(1f-t)*(1f-t);tile(id,Rectangle(flightFrom.x+(flightTo.x-flightFrom.x)*eased,flightFrom.y+(flightTo.y-flightFrom.y)*eased,flightFrom.width+(flightTo.width-flightFrom.width)*eased,flightFrom.height+(flightTo.height-flightFrom.height)*eased))}
        tool(layout.undo,"shuffle",if(roundState.undoUsed)"Undo used"else"Undo",!roundState.undoUsed&&roundState.undo!=null)
        text("Three alike make a match.",layout.shuffle.x,layout.shuffle.y+45f,.84f,muted,layout.shuffle.width)
        text("Keep a little space in your tray.",layout.shuffle.x,layout.shuffle.y+21f,.68f,muted,layout.shuffle.width)
        text("Tap the top tile of any stack",20f,29f,.78f,muted,width-40f)
    }}
}

class PauseScreen(game:OnetMatch3Game):CeramicScreen(game){
    override fun actionBounds(id:String):Rectangle?=when(id){"resume"->resumeBounds;"restart"->restartBounds;"settings"->settingsBounds;"quit"->quitBounds;else->super.actionBounds(id)}

    val resumeBounds get()=mainButton(worldHeight/2f-30f)
    val restartBounds get()=mainButton(worldHeight/2f-112f)
    val settingsBounds get()=mainButton(worldHeight/2f-194f)
    val quitBounds get()=mainButton(worldHeight/2f-276f)
    override fun tapped(x:Float,y:Float):Boolean {when{
        resumeBounds.contains(x,y)->game.screenManager.popScreen(FadeTransition(.18f,.91f,.94f,.88f))
        restartBounds.contains(x,y)->{game.screenManager.popScreen();start(game.activeGameType)}
        settingsBounds.contains(x,y)->game.screenManager.pushScreen(ScreenType.SETTINGS,FadeTransition(.18f,.91f,.94f,.88f))
        quitBounds.contains(x,y)->{game.screenManager.popScreen();go(ScreenType.MAIN_MENU)}
        else->return false
    };return true}
    override fun draw(delta:Float){ui{
        art("pause",width/2f-105f,height/2f+110f,210f,180f)
        heading("Take a breather",20f,height/2f+76f,40f,width-40f)
        text("Your puzzle will be right here.",20f,height/2f+32f,.94f,muted,width-40f)
        control("Resume",resumeBounds,primary=true);control("Fresh start",restartBounds);control("Settings",settingsBounds);control("Save & main menu",quitBounds)
    }}
}
abstract class ResultScreen(game:OnetMatch3Game,private val won:Boolean):CeramicScreen(game){
    override fun actionBounds(id:String):Rectangle?=when(id){"next"->if(won)nextBounds else null;"replay"->replayBounds;"menu"->menuBounds;else->super.actionBounds(id)}
    val nextBounds get()=mainButton(worldHeight/2f-120f)
    val replayBounds get()=mainButton(worldHeight/2f-202f)
    val menuBounds get()=mainButton(worldHeight/2f-284f)
    override fun tapped(x:Float,y:Float):Boolean {when{
        won&&nextBounds.contains(x,y)->start(game.activeGameType,game.selectedLevel+1)
        replayBounds.contains(x,y)->start(game.activeGameType)
        menuBounds.contains(x,y)->go(ScreenType.MAIN_MENU)
        else->return false
    };return true}
    override fun draw(delta:Float){ui{
        art(if(won)"victory"else"time-up",width/2f-120f,height/2f+160f,240f,220f)
        heading(if(won)"Beautifully matched!"else"A fresh start awaits",20f,height/2f+120f,37f,width-40f)
        text(game.lastResultReason,20f,height/2f+76f,.95f,muted,width-40f)
        panel(Rectangle(84f,height/2f-20f,width-168f,70f))
        text("SCORE  ${game.lastScore}",84f,height/2f+25f,1.3f,ink,width-168f)
        if(won){text("${game.lastStars} / 3 stars",20f,height/2f-52f,.9f,sage,width-40f);control("Next puzzle",nextBounds,primary=true)}
        control("Play again",replayBounds,primary=!won);control("Main menu",menuBounds)
    }}
}
class VictoryScreen(game:OnetMatch3Game):ResultScreen(game,true)
class DefeatScreen(game:OnetMatch3Game):ResultScreen(game,false)

class MissionCenterScreen(game:OnetMatch3Game):CeramicScreen(game){
    override fun actionBounds(id:String):Rectangle?=when(id){"achievements"->achievementsBounds;else->super.actionBounds(id)}

    var page=0
    val achievementsBounds get()=Rectangle(worldWidth-204f,worldHeight-150f,172f,48f)
    private val previous get()=Rectangle(32f,55f,132f,55f)
    private val next get()=Rectangle(worldWidth-164f,55f,132f,55f)
    private fun missions()=game.missionService.getActiveMissions().drop(page*3).take(3)
    fun claimBounds(index:Int)=Rectangle(worldWidth-166f,worldHeight-294f-index*166f,112f,48f)
    override fun onScreenShown(){game.refreshMissions()}
    override fun tapped(x:Float,y:Float):Boolean{
        when{backBounds.contains(x,y)->go(ScreenType.MAIN_MENU);achievementsBounds.contains(x,y)->go(ScreenType.ACHIEVEMENT_CENTER);previous.contains(x,y)&&page>0->page--;next.contains(x,y)&&(page+1)*3<game.missionService.getActiveMissions().size->page++;else->{missions().forEachIndexed{i,m->if(claimBounds(i).contains(x,y)){if(m.isClaimable)attempt("A little reward, well earned"){game.claimMission(m.id);game.soundService.feedback(SoundCue.MISSION_CLAIM)};return true}};return false}}
        return true
    }
    override fun draw(delta:Float){ui{
        top("Little goals","Play, improve, collect something lovely")
        text("${game.economyService.gemBalance} Gems",32f,height-130f,1.1f,sage)
        control("Achievements",achievementsBounds)
        missions().forEachIndexed{i,m->val y=height-320f-i*166f;panel(Rectangle(32f,y,width-64f,142f));text(m.title,50f,y+114f,1.05f);text(m.description.take(49),50f,y+83f,.75f,muted);rounded(50f,y+39f,width-245f,7f,4f,Color.valueOf("E1E6D9"));rounded(50f,y+39f,(width-245f)*m.progressRatio,7f,4f,sage);text("${m.currentCount.coerceAtMost(m.targetCount)} / ${m.targetCount}  •  ${m.gemReward} Gems",50f,y+22f,.7f,muted);control(if(m.status==MissionStatus.CLAIMED)"Claimed"else"Claim",claimBounds(i),m.isClaimable)}
        control("Previous",previous,page>0);control("Next",next,(page+1)*3<game.missionService.getActiveMissions().size);notifications()
    }}
}
class AchievementCenterScreen(game:OnetMatch3Game):CeramicScreen(game){
    var page=0
    private val previous get()=Rectangle(32f,55f,132f,55f)
    private val next get()=Rectangle(worldWidth-164f,55f,132f,55f)
    private fun entries()=game.achievementService.getAchievements().drop(page*3).take(3)
    fun claimBounds(index:Int)=Rectangle(worldWidth-166f,worldHeight-294f-index*166f,112f,48f)
    override fun tapped(x:Float,y:Float):Boolean{
        when{backBounds.contains(x,y)->go(ScreenType.MISSION_CENTER);previous.contains(x,y)&&page>0->page--;next.contains(x,y)&&(page+1)*3<game.achievementService.getAchievements().size->page++;else->{entries().forEachIndexed{i,a->if(claimBounds(i).contains(x,y)){a.tiers.firstOrNull{a.isTierClaimable(it.tierIndex)}?.let{tier->attempt("A new milestone to remember"){game.claimAchievement(a.id,tier.tierIndex);game.soundService.feedback(SoundCue.ACHIEVEMENT)}};return true}};return false}}
        return true
    }
    override fun draw(delta:Float){ui{
        top("Your milestones","Small steps become a lovely journey")
        text("${game.economyService.gemBalance} Gems",32f,height-130f,1.1f,sage)
        entries().forEachIndexed{i,a->val y=height-320f-i*166f;val tier=a.tiers.firstOrNull{!it.isClaimed}?:a.tiers.last();panel(Rectangle(32f,y,width-64f,142f));text(a.title,50f,y+113f,1.05f);text("${tier.tierName}  •  ${tier.gemReward} Gems",50f,y+82f,.82f,muted);val ratio=(a.currentValue.toFloat()/tier.requiredValue).coerceIn(0f,1f);rounded(50f,y+39f,width-245f,7f,4f,Color.valueOf("E1E6D9"));rounded(50f,y+39f,(width-245f)*ratio,7f,4f,sage);text("${a.currentValue} / ${tier.requiredValue}",50f,y+22f,.7f,muted);control(if(tier.isClaimed)"Claimed"else"Claim",claimBounds(i),a.hasAnyClaimableTier())}
        control("Previous",previous,page>0);control("Next",next,(page+1)*3<game.achievementService.getAchievements().size);notifications()
    }}
}
class PlayerProfileScreen(game:OnetMatch3Game):CeramicScreen(game){
    override fun actionBounds(id:String):Rectangle?=when(id){"collection"->collectionBounds;else->super.actionBounds(id)}

    val collectionBounds get()=mainButton(85f)
    override fun tapped(x:Float,y:Float):Boolean{when{backBounds.contains(x,y)->go(ScreenType.MAIN_MENU);collectionBounds.contains(x,y)->go(ScreenType.COSMETIC_COLLECTION);else->return false};return true}
    override fun draw(delta:Float){ui{
        top("Your little journey")
        art("new-level",width/2f-100f,height-360f,200f,190f)
        heading("Level ${game.playerProfile.level}",20f,height-410f,40f,width-40f)
        val total=game.playerProfile.currentXp;val floor=game.progressionEngine.xpRequiredForLevel(game.playerProfile.level);val target=game.progressionEngine.xpRequiredForLevel(game.playerProfile.level+1)
        val ratio=((total-floor).toFloat()/(target-floor)).coerceIn(0f,1f)
        rounded(66f,height-458f,width-132f,12f,6f,Color.valueOf("DDE5D5"));rounded(66f,height-458f,(width-132f)*ratio,12f,6f,sage)
        text("$total / $target EXP",20f,height-487f,.85f,muted,width-40f)
        panel(Rectangle(40f,230f,width-80f,170f))
        text("Onet journey",62f,365f,1f);text("Level ${game.unlockedLevel(ScreenType.ONET_GAME)}",width-200f,365f,1f,sage)
        text("Triple journey",62f,312f,1f);text("Level ${game.unlockedLevel(ScreenType.TRIPLE_MATCH_GAME)}",width-200f,312f,1f,sage)
        text("${game.cosmeticService.inventory.getUnlockedCount()} collected  •  ${game.economyService.gemBalance} Gems",62f,258f,.85f,muted)
        control("My collection",collectionBounds,primary=true)
    }}
}

private var previewItemId = CosmeticCatalog.THEME_CANDY.id
abstract class CatalogScreen(game:OnetMatch3Game,private val shop:Boolean):CeramicScreen(game){
    override fun actionBounds(id:String):Rectangle?=when(id){"shop","collection"->switchBounds;else->if(id.startsWith("card_")) id.substringAfter('_').toIntOrNull()?.takeIf { it in 0..3 }?.let(::cardBounds) else super.actionBounds(id)}

    var page=0
    val switchBounds get()=Rectangle(worldWidth-180f,worldHeight-150f,148f,48f)
    private val previous get()=Rectangle(32f,54f,132f,54f)
    private val next get()=Rectangle(worldWidth-164f,54f,132f,54f)
    private fun catalog()=CosmeticCatalog.ALL_ITEMS.filter{if(shop)it.acquisitionType==CosmeticAcquisitionType.EARNABLE else game.cosmeticService.inventory.isOwned(it.id)}
    fun cardBounds(index:Int):Rectangle {val step=(worldWidth-76f)/2f;return Rectangle(30f+(index%2)*(step+16f),worldHeight-408f-(index/2)*238f,step,216f)}
    override fun tapped(x:Float,y:Float):Boolean{
        when{backBounds.contains(x,y)->go(ScreenType.MAIN_MENU);switchBounds.contains(x,y)->go(if(shop)ScreenType.COSMETIC_COLLECTION else ScreenType.COSMETIC_SHOP);previous.contains(x,y)&&page>0->page--;next.contains(x,y)&&(page+1)*4<catalog().size->page++;else->{catalog().drop(page*4).take(4).forEachIndexed{i,item->if(cardBounds(i).contains(x,y)){previewItemId=item.id;game.soundService.feedback(SoundCue.COSMETIC_PREVIEW);game.screenManager.pushScreen(ScreenType.COSMETIC_PREVIEW,FadeTransition(.2f,.91f,.94f,.88f));return true}};return false}};return true
    }
    protected fun ScreenUi.preview(item:CosmeticItem,rect:Rectangle){
        when(item.type){
            CosmeticType.TILE_SET->{val theme=item.assetPath.substringAfterLast('/');tile(TileAssetResolver.tileIdsFor(theme).first(),rect.x+rect.width/2f-42f,rect.y+85f,84f,86f)}
            CosmeticType.TILE_BODY->bodyPreview(item.assetPath,rect.x+rect.width/2f-42f,rect.y+85f,84f,86f)
            CosmeticType.UI_PALETTE->rounded(rect.x+rect.width/2f-42f,rect.y+90f,84f,75f,18f,paletteColor(item.assetPath))
            CosmeticType.EFFECT->effect(item.assetPath.substringAfterLast('/').removeSuffix(".webp"),elapsed,rect.x+rect.width/2f-50f,rect.y+85f,100f,100f)
            else->art("succulent",rect.x+rect.width/2f-47f,rect.y+85f,94f,94f)
        }
    }
    override fun draw(delta:Float){ui{
        top(if(shop)"Something lovely"else"Your collection",if(shop)"Cosmetics earned through your little goals"else"A look that feels like you")
        text("${game.economyService.gemBalance} Gems",32f,height-129f,1f,sage);control(if(shop)"Collection"else"Gem shop",switchBounds)
        catalog().drop(page*4).take(4).forEachIndexed{i,item->val rect=cardBounds(i);panel(rect);preview(item,rect);text(item.name.take(23),rect.x+6f,rect.y+64f,.9f,ink,rect.width-12f);text(if(shop)"${item.gemPrice} Gems"else if(game.cosmeticService.inventory.getEquipped(item.type)==item.id)"Equipped"else"Preview & equip",rect.x+6f,rect.y+30f,.77f,sage,rect.width-12f)}
        control("Previous",previous,page>0);control("Next",next,(page+1)*4<catalog().size);notifications()
    }}
}
class CosmeticCollectionScreen(game:OnetMatch3Game):CatalogScreen(game,false)
class CosmeticShopScreen(game:OnetMatch3Game):CatalogScreen(game,true)
class CosmeticPreviewScreen(game:OnetMatch3Game):CeramicScreen(game){
    override fun actionBounds(id:String):Rectangle?=when(id){"equip","buy"->actionBounds;"back"->closeBounds;else->super.actionBounds(id)}

    val item get()=CosmeticCatalog.getItem(previewItemId)?:CosmeticCatalog.THEME_CANDY
    val actionBounds get()=mainButton(170f)
    val closeBounds get()=mainButton(85f)
    override fun tapped(x:Float,y:Float):Boolean{
        when{closeBounds.contains(x,y)||backBounds.contains(x,y)->game.screenManager.popScreen(FadeTransition(.18f,.91f,.94f,.88f));actionBounds.contains(x,y)->{
            if(game.cosmeticService.inventory.isOwned(item.id))attempt("Made for you"){game.equipCosmetic(item.id);game.soundService.feedback(SoundCue.COSMETIC_PREVIEW)}
            else if(item.acquisitionType==CosmeticAcquisitionType.EARNABLE)attempt("A lovely addition to your collection"){game.buyCosmetic(item.id);game.soundService.feedback(SoundCue.MISSION_CLAIM)}
            else toast("This collection is not available yet")
        };else->return false};return true
    }
    override fun draw(delta:Float){ui{
        top("A closer look",item.rarity.name.lowercase().replaceFirstChar{it.uppercase()})
        heading(item.name,24f,height-185f,34f,width-48f)
        panel(Rectangle(54f,height/2f-80f,width-108f,300f))
        when(item.type){
            CosmeticType.TILE_SET->{val theme=item.assetPath.substringAfterLast('/');val ids=TileAssetResolver.tileIdsFor(theme);for(i in 0..2)tile(ids[i],width/2f-144f+i*99f,height/2f+12f,90f,98f)}
            CosmeticType.TILE_BODY->{for(i in 0..2)bodyPreview(item.assetPath,width/2f-144f+i*99f,height/2f+12f,90f,98f)}
            CosmeticType.UI_PALETTE->rounded(width/2f-120f,height/2f-15f,240f,145f,24f,paletteColor(item.assetPath))
            CosmeticType.EFFECT->effect(item.assetPath.substringAfterLast('/').removeSuffix(".webp"),elapsed,width/2f-110f,height/2f-40f,220f,220f)
            else->art("succulent",width/2f-100f,height/2f-20f,200f,190f)
        }
        text("A fresh look. The same fair puzzle.",44f,height/2f-110f,.88f,muted,width-88f)
        val owned=game.cosmeticService.inventory.isOwned(item.id)
        control(if(owned)"Equip"else if(item.acquisitionType==CosmeticAcquisitionType.EARNABLE)"Collect for ${item.gemPrice} Gems"else"Coming soon",actionBounds,owned||item.acquisitionType==CosmeticAcquisitionType.EARNABLE,primary=true)
        control("Back to collection",closeBounds);notifications()
    }}
}
class SettingsScreen(game:OnetMatch3Game):CeramicScreen(game){
    override fun actionBounds(id:String):Rectangle? = if(id.startsWith("toggle_")) id.substringAfter('_').toIntOrNull()?.takeIf { it in 0..4 }?.let(::toggleBounds) else super.actionBounds(id)
    fun toggleBounds(index:Int)=Rectangle(worldWidth-151f,worldHeight-244f-index*112f,109f,53f)
    override fun tapped(x:Float,y:Float):Boolean{
        if(backBounds.contains(x,y)){game.saveNow();if(!game.screenManager.popScreen(FadeTransition(.18f,.91f,.94f,.88f)))go(ScreenType.MAIN_MENU);return true}
        for(i in 0..4)if(toggleBounds(i).contains(x,y)){
            val s=game.settings
            when(i){0->s.soundEnabled=!s.soundEnabled;1->s.musicEnabled=!s.musicEnabled;2->s.hapticsEnabled=!s.hapticsEnabled;3->s.reducedMotion=!s.reducedMotion;4->s.reducedEffects=!s.reducedEffects}
            game.saveNow();return true
        };return false
    }
    override fun draw(delta:Float){ui{
        top("Make yourself at home","A little comfort, your way")
        val entries=listOf(Triple("Sound effects","Soft feedback for each little match",game.settings.soundEnabled),Triple("Music","A calm background for your puzzle",game.settings.musicEnabled),Triple("Haptics","Gentle touch feedback",game.settings.hapticsEnabled),Triple("Reduced motion","Keep movement to a minimum",game.settings.reducedMotion),Triple("Reduced effects","A quieter, lighter experience",game.settings.reducedEffects))
        entries.forEachIndexed{i,(title,subtitle,on)->val rect=toggleBounds(i);panel(Rectangle(28f,rect.y-17f,width-56f,91f));text(title,47f,rect.y+42f,1.03f);text(subtitle,47f,rect.y+10f,.68f,muted);control(if(on)"On"else"Off",rect,primary=on)}
    }}
}

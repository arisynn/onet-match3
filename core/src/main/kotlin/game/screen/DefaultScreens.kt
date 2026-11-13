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
        catch (_: game.economy.InsufficientFundsException) { toast("Earn more Permen through your goals") }
    }
    protected fun go(type: ScreenType) { game.screenManager.setScreen(type, FadeTransition(if (game.settings.reducedMotion) 0f else 0.22f, .91f, .94f, .88f)) }
    protected fun ScreenUi.top(title: String, subtitle: String = "", back: Boolean = true) {
        if (back) iconButton(backBounds, "back")
        heading(title, 80f, height - 43f, 34f, width - 160f)
        if (subtitle.isNotEmpty()) text(subtitle, 30f, height - 84f, .86f, muted, width - 60f)
    }
    protected fun ScreenUi.notifications() {
        if (noticeTime > 0f) {
            val visibility = if (game.settings.reducedMotion) 1f else
                min(((2.5f-noticeTime)/.18f).coerceIn(0f,1f),(noticeTime/.25f).coerceIn(0f,1f))
            val y=128f+(1f-visibility)*12f
            rounded(28f,y,width-56f,52f,18f,Color(cream).also { it.a=visibility })
            text(notice,40f,y+33f,.88f,Color(ink).also { it.a=visibility },width-80f)
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
    private fun hasSavedRound() = game.resumedOnet()!=null || game.resumedTriple()!=null
    val homeLayout get() = HomeLayout(worldWidth,worldHeight,hasSavedRound())
    val playBounds get() = homeLayout.play
    val resumeBounds get() = homeLayout.continueGame
    override fun actionBounds(id:String):Rectangle? = when(id) {
        "play"->homeLayout.play; "resume_game"->homeLayout.continueGame; "onet"->homeLayout.onet; "triple"->homeLayout.triple
        "missions"->homeLayout.missions; "collection"->homeLayout.collection; "profile"->homeLayout.profile; "settings"->homeLayout.settings
        else->super.actionBounds(id)
    }
    override fun tapped(x:Float,y:Float):Boolean {
        val l=homeLayout
        when {
            hasSavedRound() && l.continueGame.contains(x,y)->go(game.activeGameType)
            l.play.contains(x,y)->go(ScreenType.MODE_SELECT)
            l.onet.contains(x,y)->{ selectedMode=ScreenType.ONET_GAME;go(ScreenType.LEVEL_SELECT) }
            l.triple.contains(x,y)->{ selectedMode=ScreenType.TRIPLE_MATCH_GAME;go(ScreenType.LEVEL_SELECT) }
            l.missions.contains(x,y)->go(ScreenType.MISSION_CENTER)
            l.collection.contains(x,y)->go(ScreenType.COSMETIC_COLLECTION)
            l.profile.contains(x,y)->go(ScreenType.PLAYER_PROFILE)
            l.settings.contains(x,y)->go(ScreenType.SETTINGS)
            else->return false
        };return true
    }
    override fun draw(delta:Float){ui{
        val l=homeLayout
        rounded(l.profile.x,l.profile.y,l.profile.width,l.profile.height,24f,Color.valueOf("DFEBDB"))
        text("${game.playerProfile.level}",l.profile.x,l.profile.y+43f,1.48f,ink,l.profile.width)
        text("LEVEL",l.profile.x,l.profile.y+19f,.56f,sage,l.profile.width)
        wallet(game.economyService.gemBalance,l.wallet)
        heading("Onet & Triple",28f,height-111f,32f)
        text("YOUR NEXT MATCH STARTS HERE",28f,height-151f,.70f,muted)
        panel(l.hero,Color.valueOf("E4EEDF"))
        val tileSize=min(l.hero.width*.28f,l.hero.height*.45f)
        val cy=l.hero.y+l.hero.height*.49f
        tile("candy_3",width/2f-tileSize*1.35f,cy-tileSize*.45f,tileSize,tileSize)
        tile("ocean_0",width/2f-tileSize*.48f,cy-tileSize*.22f,tileSize,tileSize)
        tile("candy_0",width/2f+tileSize*.39f,cy-tileSize*.45f,tileSize,tileSize)
        star(width*.18f,l.hero.y+l.hero.height*.80f,12f,true)
        star(width*.81f,l.hero.y+l.hero.height*.70f,9f,true)
        text("Two games. One more good match.",l.hero.x+12f,l.hero.y+26f,.9f,ink,l.hero.width-24f)
        for((r,title,icon,level) in listOf(
            ModeCard(l.onet,"Onet Connect","onet",game.unlockedLevel(ScreenType.ONET_GAME)),
            ModeCard(l.triple,"Triple Match","triple",game.unlockedLevel(ScreenType.TRIPLE_MATCH_GAME)))) {
            panel(r,cream)
            art(icon,r.x+r.width/2f-43f,r.y+r.height-101f,86f,86f)
            heading(title,r.x+12f,r.y+67f,24f,r.width-24f)
            text("Level $level",r.x+12f,r.y+34f,.85f,sage,r.width-24f)
        }
        control("Play",l.play,primary=true)
        if(hasSavedRound()) control("Continue saved game",l.continueGame)
        rounded(16f,16f,width-32f,100f,26f,cream)
        navButton(l.missions,"reward","Goals")
        navButton(l.collection,"new-level","Collection")
        navButton(l.settings,"settings","Settings")
    }}
    private data class ModeCard(val rect:Rectangle,val title:String,val icon:String,val level:Int)
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
        top("Choose a game", "Pick your mode")
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
        top(if (selectedMode == ScreenType.ONET_GAME) "Onet levels" else "Triple levels", "Choose your next level")
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
    private var clearEffects=emptyList<Rectangle>()
    private var clearEffectTime=0f
    private fun checkpoint(){if(roundState.phase=="playing")persist { game.captureTriple(roundState) }}
    override fun onScreenShown(){game.activeGameType=ScreenType.TRIPLE_MATCH_GAME}
    override fun pause(){roundState=pauseTripleRound(roundState,true);checkpoint()}
    override fun resume(){roundState=pauseTripleRound(roundState,false)}
    override fun tapped(x:Float,y:Float):Boolean {
        val layout=puzzleLayout
        if(layout.pause.contains(x,y)||layout.back.contains(x,y)){pause();game.screenManager.pushScreen(ScreenType.PAUSE,FadeTransition(.18f,.91f,.94f,.88f));return true}
        if(layout.undo.contains(x,y)) {if(!roundState.undoUsed&&roundState.undo!=null){roundState=undoPick(roundState);flightId=null;clearEffects=emptyList();game.soundService.feedback(SoundCue.UNDO);checkpoint()};return true}
        if(flightId!=null||roundState.phase!="playing")return false
        val index=layout.stackHit(x,y)?:return false
        val id=roundState.stacks[index].lastOrNull()?:return false
        flightFrom=layout.stack(index);flightTo=layout.traySlot(roundState.tray.size.coerceAtMost(6));flightTime=0f
        val previousCleared=roundState.cleared
        val matchedPositions=roundState.tray.mapIndexedNotNull { i,tileId -> if(tileId==id)layout.traySlot(i) else null }
        roundState=pickTile(roundState,index)
        if (roundState.cleared>previousCleared && !game.settings.reducedMotion && !game.settings.reducedEffects) {
            clearEffects=matchedPositions+Rectangle(flightTo)
            clearEffectTime=0f
        }
        flightId=if(game.settings.reducedMotion)null else id
        game.soundService.feedback(if(roundState.cleared>previousCleared)SoundCue.MATCH else SoundCue.SELECT)
        checkpoint();return true
    }
    override fun update(delta:Float){
        super.update(delta); settlementRetry=(settlementRetry-delta).coerceAtLeast(0f)
        if(roundState.paused||finished)return
        if(clearEffects.isNotEmpty()){clearEffectTime+=delta;if(clearEffectTime>=.64f)clearEffects=emptyList()}
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
        gameHeader("Triple Match","LEVEL ${game.selectedLevel}",tripleScore(roundState),roundState.time,roundState.duration,layout)
        panel(layout.playfield,Color.valueOf("FAFBF3"))
        roundState.stacks.forEachIndexed { i,stack -> if(stack.isNotEmpty()){
            val r=layout.stack(i)
            if(stack.size>1)rounded(r.x+6f,r.y+7f,r.width,r.height,15f,Color.valueOf("CFD8C4"))
            tile(stack.last(),r)
            if(stack.size>1){
                rounded(r.x+r.width-23f,r.y+r.height-19f,28f,28f,14f,sage)
                text("${stack.size}",r.x+r.width-23f,r.y+r.height+1f,.77f,cream,28f)
            }
        }}
        panel(layout.tray,Color.valueOf("E4ECDB"))
        text("MATCH 3",layout.tray.x+16f,layout.tray.y+87f,.62f,muted)
        text("${roundState.tray.size} / 7",layout.tray.x+layout.tray.width-70f,layout.tray.y+87f,.62f,muted)
        for(i in 0 until 7){val rect=layout.traySlot(i);slot(rect);roundState.tray.getOrNull(i)?.let{id->tile(id,rect)}}
        flightId?.let{id->val t=(flightTime/.19f).coerceIn(0f,1f);val eased=1f-(1f-t)*(1f-t);tile(id,Rectangle(flightFrom.x+(flightTo.x-flightFrom.x)*eased,flightFrom.y+(flightTo.y-flightFrom.y)*eased,flightFrom.width+(flightTo.width-flightFrom.width)*eased,flightFrom.height+(flightTo.height-flightFrom.height)*eased))}
        if(!game.settings.reducedMotion&&!game.settings.reducedEffects){
            val chosen=game.cosmeticService.inventory.getEquipped(CosmeticType.EFFECT)?.let(CosmeticCatalog::getItem)
                ?.assetPath?.substringAfterLast('/')?.removeSuffix(".webp") ?: "match-burst"
            clearEffects.forEach { rect->effect(chosen,clearEffectTime,rect.x-13f,rect.y-13f,rect.width+26f,rect.height+26f) }
        }
        tool(layout.undo,"shuffle",if(roundState.undoUsed)"Used"else"Undo",!roundState.undoUsed&&roundState.undo!=null)
        for(i in 0..2) imageTile("candy_3",layout.shuffle.x+24f+i*45f,layout.shuffle.y+23f,42f,42f)
        text("3 identical tiles",layout.shuffle.x,layout.shuffle.y+15f,.68f,muted,layout.shuffle.width)
        text("Tap a stack. Collect three.",20f,29f,.78f,muted,width-40f)
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
        heading("Paused",20f,height/2f+76f,40f,width-40f)
        text("Ready when you are.",20f,height/2f+32f,.94f,muted,width-40f)
        control("Resume",resumeBounds,primary=true);control("Restart",restartBounds);control("Settings",settingsBounds);control("Save & exit",quitBounds)
    }}
}
abstract class ResultScreen(game:OnetMatch3Game,private val won:Boolean):CeramicScreen(game){
    val resultLayout get()=ResultLayout(worldWidth,worldHeight,won)
    val nextBounds get()=resultLayout.primary
    val replayBounds get()=if(won)resultLayout.replay else resultLayout.primary
    val menuBounds get()=resultLayout.menu
    override fun actionBounds(id:String):Rectangle?=when(id){"next"->if(won)nextBounds else null;"replay"->replayBounds;"menu"->menuBounds;else->super.actionBounds(id)}
    override fun tapped(x:Float,y:Float):Boolean {when{
        won&&nextBounds.contains(x,y)->start(game.activeGameType,game.selectedLevel+1)
        replayBounds.contains(x,y)->start(game.activeGameType)
        menuBounds.contains(x,y)->go(ScreenType.MAIN_MENU)
        else->return false
    };return true}
    override fun draw(delta:Float){ui{
        val l=resultLayout
        panel(Rectangle(16f,l.menu.y-24f,width-32f,l.illustration.y+l.illustration.height-l.menu.y+40f))
        val image=if(won)"victory" else if(game.lastResultReason.contains("tray",true))"tray-full"else"time-up"
        art(image,l.illustration.x,l.illustration.y,l.illustration.width,l.illustration.height)
        heading(if(won)"Level complete!"else"Try again",30f,l.titleY,34f,width-60f)
        text(if(won)"${if(game.activeGameType==ScreenType.ONET_GAME)"Onet Connect"else"Triple Match"}  •  Level ${game.selectedLevel}"else game.lastResultReason,
            30f,l.subtitleY,.92f,muted,width-60f)
        if(won) for(i in 0..2) star(width/2f+(i-1)*59f,l.stars.y+23f,22f,i<game.lastStars)
        else text("Your best move is still ahead.",l.stars.x,l.stars.y+26f,.81f,muted,l.stars.width)
        rounded(l.stats.x,l.stats.y,l.stats.width,l.stats.height,20f,Color.valueOf("EDF3E8"))
        val progress=if(game.settings.reducedMotion)1f else (elapsed/.85f).coerceIn(0f,1f).let{it*it*(3f-2f*it)}
        text("SCORE",l.stats.x+20f,l.stats.y+l.stats.height-18f,.65f,muted)
        heading("${(game.lastScore*progress).toInt()}",l.stats.x+20f,l.stats.y+83f,36f)
        if(won){
            text("+${game.lastRoundXp} EXP",l.stats.x+l.stats.width-144f,l.stats.y+83f,1.1f,sage,124f)
            val oldXp=(game.playerProfile.currentXp-game.lastRoundXp).coerceAtLeast(0)
            val xp=oldXp+(game.lastRoundXp*progress).toLong()
            val level=game.progressionEngine.calculateLevelForXp(xp)
            val floor=game.progressionEngine.xpRequiredForLevel(level)
            val target=game.progressionEngine.xpRequiredForLevel(level+1)
            val ratio=((xp-floor).toFloat()/(target-floor)).coerceIn(0f,1f)
            text("Level $level",l.stats.x+20f,l.stats.y+49f,.74f,muted)
            text("$xp / $target EXP",l.stats.x+l.stats.width-176f,l.stats.y+49f,.74f,muted,156f)
            rounded(l.stats.x+20f,l.stats.y+20f,l.stats.width-40f,9f,4f,Color.valueOf("D5DFCF"))
            rounded(l.stats.x+20f,l.stats.y+20f,(l.stats.width-40f)*ratio,9f,4f,sage)
            control("Next level",l.primary,primary=true)
            control("Replay",l.replay);control("Home",l.menu)
        }else{
            text("A new attempt, a fresh board.",l.stats.x+20f,l.stats.y+36f,.84f,muted,l.stats.width-40f)
            control("Try again",l.primary,primary=true)
            control("Home",l.menu)
        }
    }}
}
class VictoryScreen(game:OnetMatch3Game):ResultScreen(game,true)
class DefeatScreen(game:OnetMatch3Game):ResultScreen(game,false)

class MissionCenterScreen(game:OnetMatch3Game):CeramicScreen(game){
    override fun actionBounds(id:String):Rectangle?=when(id){"achievements"->achievementsBounds;else->super.actionBounds(id)}

    val listLayout get()=ListLayout(worldWidth,worldHeight)
    var page=0
    val achievementsBounds get()=Rectangle(worldWidth-204f,worldHeight-150f,172f,48f)
    private val previous get()=Rectangle(32f,55f,132f,55f)
    private val next get()=Rectangle(worldWidth-164f,55f,132f,55f)
    private fun missions()=game.missionService.getActiveMissions().drop(page*listLayout.count).take(listLayout.count)
    fun claimBounds(index:Int)=listLayout.claim(index)
    override fun onScreenShown(){game.refreshMissions()}
    override fun tapped(x:Float,y:Float):Boolean{
        when{backBounds.contains(x,y)->go(ScreenType.MAIN_MENU);achievementsBounds.contains(x,y)->go(ScreenType.ACHIEVEMENT_CENTER);previous.contains(x,y)&&page>0->page--;next.contains(x,y)&&(page+1)*listLayout.count<game.missionService.getActiveMissions().size->page++;else->{missions().forEachIndexed{i,m->if(claimBounds(i).contains(x,y)){if(m.isClaimable)attempt("A little reward, well earned"){game.claimMission(m.id);game.soundService.feedback(SoundCue.MISSION_CLAIM)};return true}};return false}}
        return true
    }
    override fun draw(delta:Float){ui{
        top("Missions","Complete goals. Collect rewards.")
        wallet(game.economyService.gemBalance,Rectangle(28f,height-153f,170f,48f))
        control("Achievements",achievementsBounds)
        missions().forEachIndexed{i,m->val r=listLayout.card(i)
            panel(r);text(m.title,r.x+18f,r.y+r.height-23f,1.05f,ink,r.width-152f)
            wallet(m.gemReward,Rectangle(r.x+r.width-116f,r.y+r.height-47f,100f,34f))
            text(m.description.take(58),r.x+18f,r.y+r.height-60f,.76f,muted,r.width-36f)
            rounded(r.x+18f,r.y+47f,r.width-180f,7f,4f,Color.valueOf("E1E6D9"))
            rounded(r.x+18f,r.y+47f,(r.width-180f)*m.progressRatio,7f,4f,sage)
            text("${m.currentCount.coerceAtMost(m.targetCount)} / ${m.targetCount}",r.x+18f,r.y+30f,.72f,muted)
            control(if(m.status==MissionStatus.CLAIMED)"Claimed"else"Claim",claimBounds(i),m.isClaimable)
        }
        control("Previous",previous,page>0);control("Next",next,(page+1)*listLayout.count<game.missionService.getActiveMissions().size);notifications()
    }}
}
class AchievementCenterScreen(game:OnetMatch3Game):CeramicScreen(game){
    val listLayout get()=ListLayout(worldWidth,worldHeight)
    var page=0
    private val previous get()=Rectangle(32f,55f,132f,55f)
    private val next get()=Rectangle(worldWidth-164f,55f,132f,55f)
    private fun entries()=game.achievementService.getAchievements().drop(page*listLayout.count).take(listLayout.count)
    fun claimBounds(index:Int)=listLayout.claim(index)
    override fun tapped(x:Float,y:Float):Boolean{
        when{backBounds.contains(x,y)->go(ScreenType.MISSION_CENTER);previous.contains(x,y)&&page>0->page--;next.contains(x,y)&&(page+1)*listLayout.count<game.achievementService.getAchievements().size->page++;else->{entries().forEachIndexed{i,a->if(claimBounds(i).contains(x,y)){a.tiers.firstOrNull{a.isTierClaimable(it.tierIndex)}?.let{tier->attempt("A new milestone to remember"){game.claimAchievement(a.id,tier.tierIndex);game.soundService.feedback(SoundCue.ACHIEVEMENT)}};return true}};return false}}
        return true
    }
    override fun draw(delta:Float){ui{
        top("Achievements","Your progress, rewarded.")
        wallet(game.economyService.gemBalance,Rectangle(28f,height-153f,170f,48f))
        entries().forEachIndexed{i,a->val r=listLayout.card(i);val tier=a.tiers.firstOrNull{!it.isClaimed}?:a.tiers.last()
            panel(r);text(a.title,r.x+18f,r.y+r.height-23f,1.05f,ink,r.width-152f)
            wallet(tier.gemReward,Rectangle(r.x+r.width-116f,r.y+r.height-47f,100f,34f))
            text(tier.tierName,r.x+18f,r.y+r.height-60f,.82f,muted)
            val ratio=(a.currentValue.toFloat()/tier.requiredValue).coerceIn(0f,1f)
            rounded(r.x+18f,r.y+47f,r.width-180f,7f,4f,Color.valueOf("E1E6D9"))
            rounded(r.x+18f,r.y+47f,(r.width-180f)*ratio,7f,4f,sage)
            text("${a.currentValue} / ${tier.requiredValue}",r.x+18f,r.y+30f,.72f,muted)
            control(if(tier.isClaimed)"Claimed"else"Claim",claimBounds(i),a.hasAnyClaimableTier())
        }
        control("Previous",previous,page>0);control("Next",next,(page+1)*listLayout.count<game.achievementService.getAchievements().size);notifications()
    }}
}
class PlayerProfileScreen(game:OnetMatch3Game):CeramicScreen(game){
    override fun actionBounds(id:String):Rectangle?=when(id){"collection"->collectionBounds;else->super.actionBounds(id)}

    val collectionBounds get()=mainButton((worldHeight-940f)/2f+86f)
    override fun tapped(x:Float,y:Float):Boolean{when{backBounds.contains(x,y)->go(ScreenType.MAIN_MENU);collectionBounds.contains(x,y)->go(ScreenType.COSMETIC_COLLECTION);else->return false};return true}
    override fun draw(delta:Float){ui{
        top("Your profile")
        art("new-level",width/2f-80f,height-275f,160f,150f)
        heading("Level ${game.playerProfile.level}",20f,height-310f,36f,width-40f)
        val total=game.playerProfile.currentXp;val floor=game.progressionEngine.xpRequiredForLevel(game.playerProfile.level);val target=game.progressionEngine.xpRequiredForLevel(game.playerProfile.level+1)
        val reveal=if(game.settings.reducedMotion)1f else (elapsed/.7f).coerceIn(0f,1f).let { it*it*(3f-2f*it) }
        val ratio=((total-floor).toFloat()/(target-floor)).coerceIn(0f,1f)*reveal
        rounded(66f,height-357f,width-132f,12f,6f,Color.valueOf("DDE5D5"));rounded(66f,height-357f,(width-132f)*ratio,12f,6f,sage)
        text("$total / $target EXP",20f,height-386f,.85f,muted,width-40f)
        panel(Rectangle(28f,height-610f,width-56f,174f))
        text("Onet Connect",48f,height-472f,1f);text("Level ${game.unlockedLevel(ScreenType.ONET_GAME)}",width-180f,height-472f,1f,sage)
        text("Triple Match",48f,height-525f,1f);text("Level ${game.unlockedLevel(ScreenType.TRIPLE_MATCH_GAME)}",width-180f,height-525f,1f,sage)
        text("${game.cosmeticService.inventory.getUnlockedCount()} cosmetics collected",48f,height-579f,.85f,muted)
        wallet(game.economyService.gemBalance,Rectangle(width/2f-90f,height-696f,180f,48f))
        control("My collection",collectionBounds,primary=true)
    }}
}

private var previewItemId = CosmeticCatalog.THEME_CANDY.id
abstract class CatalogScreen(game:OnetMatch3Game,private val shop:Boolean):CeramicScreen(game){
    override fun actionBounds(id:String):Rectangle?=when(id){"shop","collection"->switchBounds;else->if(id.startsWith("card_")) id.substringAfter('_').toIntOrNull()?.takeIf { it in 0 until pageSize }?.let(::cardBounds) else super.actionBounds(id)}

    private val rowCount get()=if(worldHeight>=1120f)3 else 2
    private val pageSize get()=rowCount*2
    var page=0
    val switchBounds get()=Rectangle(worldWidth-180f,worldHeight-150f,148f,48f)
    private val previous get()=Rectangle(32f,54f,132f,54f)
    private val next get()=Rectangle(worldWidth-164f,54f,132f,54f)
    private fun catalog()=CosmeticCatalog.ALL_ITEMS.filter{if(shop)it.acquisitionType==CosmeticAcquisitionType.EARNABLE else game.cosmeticService.inventory.isOwned(it.id)}
    fun cardBounds(index:Int):Rectangle {
        val column=(worldWidth-72f)/2f;val step=(worldHeight-300f)/rowCount
        return Rectangle(28f+(index%2)*(column+16f),worldHeight-182f-(index/2+1)*step+16f,column,step-16f)
    }

    override fun tapped(x:Float,y:Float):Boolean{
        when{backBounds.contains(x,y)->go(ScreenType.MAIN_MENU);switchBounds.contains(x,y)->go(if(shop)ScreenType.COSMETIC_COLLECTION else ScreenType.COSMETIC_SHOP);previous.contains(x,y)&&page>0->page--;next.contains(x,y)&&(page+1)*pageSize<catalog().size->page++;else->{catalog().drop(page*pageSize).take(pageSize).forEachIndexed{i,item->if(cardBounds(i).contains(x,y)){previewItemId=item.id;game.soundService.feedback(SoundCue.COSMETIC_PREVIEW);game.screenManager.pushScreen(ScreenType.COSMETIC_PREVIEW,FadeTransition(.2f,.91f,.94f,.88f));return true}};return false}};return true
    }
    protected fun ScreenUi.preview(item:CosmeticItem,rect:Rectangle){
        when(item.type){
            CosmeticType.TILE_SET->{val theme=item.assetPath.substringAfterLast('/');tile(TileAssetResolver.tileIdsFor(theme).first(),rect.x+rect.width/2f-42f,rect.y+rect.height/2f-14f,84f,86f)}
            CosmeticType.TILE_BODY->bodyPreview(item.assetPath,rect.x+rect.width/2f-42f,rect.y+rect.height/2f-14f,84f,86f)
            CosmeticType.UI_PALETTE->rounded(rect.x+rect.width/2f-42f,rect.y+rect.height/2f-14f,84f,75f,18f,paletteColor(item.assetPath))
            CosmeticType.EFFECT->effect(item.assetPath.substringAfterLast('/').removeSuffix(".webp"),elapsed,rect.x+rect.width/2f-50f,rect.y+rect.height/2f-14f,100f,100f)
            else->art("succulent",rect.x+rect.width/2f-47f,rect.y+rect.height/2f-14f,94f,94f)
        }
    }
    override fun draw(delta:Float){ui{
        top(if(shop)"Candy shop"else"Collection",if(shop)"Unlock your next favorite"else"Your tiles, your style")
        wallet(game.economyService.gemBalance,Rectangle(28f,height-153f,170f,48f));control(if(shop)"Collection"else"Candy shop",switchBounds)
        catalog().drop(page*pageSize).take(pageSize).forEachIndexed{i,item->val rect=cardBounds(i)
            panel(rect);preview(item,rect)
            text(item.rarity.name.lowercase().replaceFirstChar{it.uppercase()},rect.x+12f,rect.y+rect.height-19f,.65f,muted,rect.width-24f)
            text(item.name,rect.x+12f,rect.y+76f,.96f,ink,rect.width-24f)
            if(shop)wallet(item.gemPrice,Rectangle(rect.x+rect.width/2f-55f,rect.y+18f,110f,36f))
            else text(if(game.cosmeticService.inventory.getEquipped(item.type)==item.id)"Equipped"else"Preview",rect.x+12f,rect.y+33f,.80f,sage,rect.width-24f)
        }
        control("Previous",previous,page>0);control("Next",next,(page+1)*pageSize<catalog().size);notifications()
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
        top("Preview",item.rarity.name.lowercase().replaceFirstChar{it.uppercase()})
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
        control(if(owned)"Equip"else if(item.acquisitionType==CosmeticAcquisitionType.EARNABLE)"Collect for ${item.gemPrice} Permen"else"Coming soon",actionBounds,owned||item.acquisitionType==CosmeticAcquisitionType.EARNABLE,primary=true)
        control("Back to collection",closeBounds);notifications()
    }}
}
class SettingsScreen(game:OnetMatch3Game):CeramicScreen(game){
    override fun actionBounds(id:String):Rectangle? = if(id.startsWith("toggle_")) id.substringAfter('_').toIntOrNull()?.takeIf { it in 0..4 }?.let(::toggleBounds) else super.actionBounds(id)
    fun toggleBounds(index:Int)=Rectangle(worldWidth-138f,worldHeight/2f+154f-index*88f,96f,56f)
    override fun tapped(x:Float,y:Float):Boolean{
        if(backBounds.contains(x,y)){game.saveNow();if(!game.screenManager.popScreen(FadeTransition(.18f,.91f,.94f,.88f)))go(ScreenType.MAIN_MENU);return true}
        for(i in 0..4)if(toggleBounds(i).contains(x,y)){
            val s=game.settings
            when(i){0->s.soundEnabled=!s.soundEnabled;1->s.musicEnabled=!s.musicEnabled;2->s.hapticsEnabled=!s.hapticsEnabled;3->s.reducedMotion=!s.reducedMotion;4->s.reducedEffects=!s.reducedEffects}
            game.saveNow();return true
        };return false
    }
    override fun draw(delta:Float){ui{
        top("Settings","Sound, music & accessibility")
        panel(Rectangle(24f,height/2f-244f,width-48f,500f))
        val entries=listOf(Triple("Sound effects","Match and button sounds",game.settings.soundEnabled),Triple("Music","Background music",game.settings.musicEnabled),Triple("Haptics","Touch feedback",game.settings.hapticsEnabled),Triple("Reduced motion","Simpler animations",game.settings.reducedMotion),Triple("Reduced effects","Fewer visual effects",game.settings.reducedEffects))
        entries.forEachIndexed{i,(title,subtitle,on)->val rect=toggleBounds(i)
            text(title,44f,rect.y+44f,1.05f)
            text(subtitle,44f,rect.y+16f,.76f,muted)
            toggle(rect,on)
            if(i<4)rounded(44f,rect.y-14f,width-88f,1f,.5f,Color.valueOf("E4E9DE"))
        }
        text("Changes are saved automatically",32f,height/2f-218f,.72f,muted,width-64f)
    }}
}

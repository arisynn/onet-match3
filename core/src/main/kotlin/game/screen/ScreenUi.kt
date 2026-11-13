package game.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.PixmapTextureData
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.JsonReader
import game.assets.TileAssetResolver
import kotlin.math.min

/** Ceramic artwork, crisp scalable typography, and consistent controls for native screens. */
class ScreenUi(typeface: UiTypeface) : Disposable {
    val ink = Color.valueOf("28483D")
    val muted = Color.valueOf("718276")
    val sage = Color.valueOf("37785D")
    val cream = Color.valueOf("FFFDF5")
    private val batch = SpriteBatch()
    private val glyphs = GlyphLayout()
    private val bodyFont = typeface.body
    private val displayFont = typeface.display
    private val sourcePixmaps = mutableListOf<Pixmap>()
    private val pixel = managedPixmap(1) { it.setColor(Color.WHITE); it.fill() }
    private val circle = managedPixmap(128) {
        it.setColor(Color.WHITE); it.fillCircle(64, 64, 63)
    }
    private val textures = mutableMapOf<String, Texture>()
    private val regions = mutableMapOf<String, TextureRegion>()
    private val patches = mutableMapOf<String, NinePatch>()
    private val manifest = JsonReader().parse(Gdx.files.internal("ui-native/manifest.json"))
    private var originX = 0f
    private var originY = 0f
    private var pointer: Vector2? = null
    var bodyTone = "ivory"
    var palettePath = "ivory"
    var effectsEnabled = true

    fun paletteColor(path: String): Color = Color.valueOf(when(path.substringAfterLast('/').removeSuffix(".webp")) {
        "mint" -> "E3EDDF"; "peach" -> "F6E4D9"; "lavender" -> "EBE5F2"
        "sky" -> "E2EDF3"; "rose" -> "F3E1E4"; "sand" -> "F2E7D7"
        "slate" -> "DCE2E5"; else -> "F2F4EA"
    })

    fun bodyPreview(path: String, x: Float, y: Float, w: Float, h: Float) {
        val tone=path.substringAfterLast('/').removeSuffix(".webp").substringAfterLast('-')
        val name=if (manifest.has("tile-$tone")) "tile-$tone" else "tile-ivory"
        patch(name).draw(batch,x+originX,y+originY,w,h)
    }

    fun effect(name:String,elapsed:Float,x:Float,y:Float,w:Float,h:Float) {
        val frame=if(effectsEnabled) ((elapsed/.08f).toInt()%8).coerceIn(0,7) else 3
        val path="ui-kit/effects/$name/${frame.toString().padStart(2,'0')}.png"
        if (Gdx.files.internal(path).exists()) batch.draw(texture(path),x+originX,y+originY,w,h)
        else art("reward",x,y,w,h)
    }
    var width = 540f
        private set
    var height = 960f
        private set

    private fun managedPixmap(size: Int, draw: (Pixmap) -> Unit): Texture {
        val pixmap = Pixmap(size, size, Pixmap.Format.RGBA8888)
        draw(pixmap)
        sourcePixmaps.add(pixmap)
        return Texture(PixmapTextureData(pixmap, Pixmap.Format.RGBA8888, true, false, true)).apply {
            setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear)
        }
    }

    fun render(projection: Matrix4, worldWidth: Float, worldHeight: Float,
               offsetX: Float, offsetY: Float, touch: Vector2?, draw: ScreenUi.() -> Unit) {
        width = worldWidth; height = worldHeight
        pointer = touch
        originX = 0f; originY = 0f
        batch.projectionMatrix = projection
        batch.begin()
        try {
            background()
            originX = offsetX; originY = offsetY
            draw()
        } finally { batch.end() }
    }

    private fun texture(path: String) = textures.getOrPut(path) {
        Texture(Gdx.files.internal(path), true).apply {
            setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear)
        }
    }

    private fun region(name: String) = regions.getOrPut(name) {
        val entry = manifest.get(name)
        val bounds = entry.get("bounds").asIntArray()
        TextureRegion(texture(entry.getString("path")), bounds[0], bounds[1], bounds[2], bounds[3])
    }

    private fun patch(name: String) = patches.getOrPut(name) {
        val source = region(name)
        val inset = min(source.regionWidth, source.regionHeight) / 4
        NinePatch(source, inset, inset, inset, inset).apply { scale(13f / inset, 13f / inset) }
    }

    private fun background() {
        fillRect(0f, 0f, width, height, paletteColor(palettePath).cpy().lerp(Color.valueOf("FFF9EF"), .43f))
        // Decorative art stays quiet so the puzzle, controls and text carry the contrast.
        val source = region("spa")
        val scale = maxOf(width / source.regionWidth, height / source.regionHeight)
        batch.setColor(1f, 1f, 1f, .045f)
        batch.draw(source,(width-source.regionWidth*scale)/2f,(height-source.regionHeight*scale)/2f,
            source.regionWidth*scale,source.regionHeight*scale)
        batch.color = Color.WHITE
    }

    private fun fillRect(x: Float, y: Float, w: Float, h: Float, color: Color) {
        batch.color = color
        batch.draw(pixel, x + originX, y + originY, w, h)
        batch.color = Color.WHITE
    }

    private val outerX=FloatArray(28)
    private val outerY=FloatArray(28)
    private val innerX=FloatArray(28)
    private val innerY=FloatArray(28)
    private val shapeVertices=FloatArray(20)
    private val arcCos=FloatArray(28){i->kotlin.math.cos((i/7*90.0+i%7*15.0)*Math.PI/180.0).toFloat()}
    private val arcSin=FloatArray(28){i->kotlin.math.sin((i/7*90.0+i%7*15.0)*Math.PI/180.0).toFloat()}
    private fun quad(x1:Float,y1:Float,c1:Float,x2:Float,y2:Float,c2:Float,x3:Float,y3:Float,c3:Float,x4:Float,y4:Float,c4:Float){
        shapeVertices[0]=x1+originX;shapeVertices[1]=y1+originY;shapeVertices[2]=c1
        shapeVertices[5]=x2+originX;shapeVertices[6]=y2+originY;shapeVertices[7]=c2
        shapeVertices[10]=x3+originX;shapeVertices[11]=y3+originY;shapeVertices[12]=c3
        shapeVertices[15]=x4+originX;shapeVertices[16]=y4+originY;shapeVertices[17]=c4
        batch.draw(pixel,shapeVertices,0,20)
    }
    fun rounded(x: Float, y: Float, w: Float, h: Float, radius: Float = 18f, color: Color = cream) {
        if(w<=0f||h<=0f||color.a<=0f)return
        val r=min(radius,min(w,h)/2f);val inset=min(.7f,r*.3f)
        for(i in 0 until 28){
            val corner=i/7
            val cx=if(corner==0||corner==3)x+w-r else x+r
            val cy=if(corner<=1)y+h-r else y+r
            outerX[i]=cx+arcCos[i]*r;outerY[i]=cy+arcSin[i]*r
            innerX[i]=cx+arcCos[i]*(r-inset);innerY[i]=cy+arcSin[i]*(r-inset)
        }
        val packed=color.toFloatBits();val clear=Color.toFloatBits(color.r,color.g,color.b,0f)
        val cx=x+w/2f;val cy=y+h/2f
        for(i in 0 until 28){val j=(i+1)%28
            quad(cx,cy,packed,innerX[i],innerY[i],packed,innerX[j],innerY[j],packed,innerX[j],innerY[j],packed)
            quad(innerX[i],innerY[i],packed,outerX[i],outerY[i],clear,outerX[j],outerY[j],clear,innerX[j],innerY[j],packed)
        }
    }

    fun box(x: Float, y: Float, width: Float, height: Float, color: Color = cream) =
        rounded(x, y, width, height, min(16f, height / 2f), color)

    fun panel(rect: Rectangle, color: Color = cream) {
        rounded(rect.x, rect.y - 3f, rect.width, rect.height, 24f, Color.valueOf("DCE1D3"))
        rounded(rect.x, rect.y, rect.width, rect.height, 24f, color)
    }

    fun art(name: String, x: Float, y: Float, w: Float, h: Float, alpha: Float = 1f) {
        val source = region(name)
        val scale = min(w / source.regionWidth, h / source.regionHeight)
        val rw = source.regionWidth * scale; val rh = source.regionHeight * scale
        batch.setColor(1f, 1f, 1f, alpha)
        batch.draw(source, x + originX + (w - rw) / 2f, y + originY + (h - rh) / 2f, rw, rh)
        batch.color = Color.WHITE
    }

    private fun label(value: String, x: Float, y: Float, points: Float, color: Color,
                      width: Float, display: Boolean = false, centeredVertically: Boolean = false) {
        val font = if (display) displayFont else bodyFont
        font.data.setScale(points / 64f)
        font.color = color
        glyphs.setText(font, value)
        if (width > 0f && glyphs.width > width) {
            font.data.setScale(font.data.scaleX * width / glyphs.width)
            glyphs.setText(font,value)
        }
        val left = if (width > 0f) x + (width - glyphs.width) / 2f else x
        font.draw(batch, glyphs, left + originX, y + originY + if (centeredVertically) glyphs.height / 2f else 0f)
    }

    fun text(value: String, x: Float, y: Float, size: Float = 1.5f, color: Color = ink, width: Float = 0f) =
        label(value, x, y, size * 18f, color, width)

    fun heading(value: String, x: Float, y: Float, points: Float = 36f, width: Float = 0f) =
        label(value, x, y, points, ink, width)

    fun title(value: String, subtitle: String = "") {
        heading(value, 20f, 812f, 40f, 500f)
        if (subtitle.isNotBlank()) text(subtitle, 20f, 759f, 1.05f, muted, 500f)
    }

    fun button(value: String, x: Float, y: Float, width: Float, height: Float,
               enabled: Boolean = true, primary: Boolean = value in setOf("Play", "Resume", "Next puzzle", "Play again")) {
        val pressed = enabled && pointer?.let { Rectangle(x, y, width, height).contains(it) } == true
        val dy = if (pressed) -2f else 0f
        val face = if (primary) sage else cream
        val shadow = if (primary) Color.valueOf("285B47") else Color.valueOf("D2DCCF")
        rounded(x,y-4f,width,height,18f,shadow)
        rounded(x,y+dy,width,height,18f,if (enabled) face else Color.valueOf("E5E8DE"))
        if (!primary && enabled) {
            rounded(x+1f,y+dy+1f,width-2f,height-2f,17f,Color.valueOf("FFFDF8"))
        }
        label(value, x, y + height / 2f + dy, min(24f, width / (value.length.coerceAtLeast(1) * 0.54f)),
            if (primary) cream else if (enabled) ink else muted, width, centeredVertically = true)
    }

    fun back() { button("Back", 40f, 880f, 100f, 50f) }

    fun tile(id: String, x: Float, y: Float, width: Float, height: Float, selected: Boolean = false) {
        if (selected) rounded(x - 2f, y - 2f, width + 4f, height + 4f, 15f, sage)
        patch(if (selected) "tile-mint" else if(manifest.has("tile-$bodyTone")) "tile-$bodyTone" else "tile-ivory").draw(batch, x + originX, y + originY, width, height)
        val original = TileAssetResolver.tileAssetFor(id)
        val path = original?.let { if (it.endsWith(".webp")) it.replace("tiles/", "tiles-native/").removeSuffix(".webp") + ".png" else it }
        val icon = if (path != null && Gdx.files.internal(path).exists()) texture(path) else null
        if (icon != null) {
            val scale = min((width - 12f) / icon.width, (height - 12f) / icon.height) * 0.90f
            val w = icon.width * scale; val h = icon.height * scale
            batch.draw(icon, x + originX + (width - w) / 2f, y + originY + (height - h) / 2f + 1f, w, h)
        } else text(id.substringAfterLast('_'), x, y + height / 2f + 6f, 1f, ink, width)
    }

    fun tile(id: String, rect: Rectangle, selected: Boolean = false) =
        tile(id, rect.x, rect.y, rect.width, rect.height, selected)

    fun iconButton(rect: Rectangle, name: String) {
        val cx = rect.x + rect.width / 2f; val cy = rect.y + rect.height / 2f
        rounded(cx - 25f, cy - 27f, 50f, 50f, 25f, Color.valueOf("DCE3D3"))
        rounded(cx - 25f, cy - 24f, 50f, 50f, 25f, cream)
        if (name == "pause") {
            rounded(cx - 7f, cy - 8f, 5f, 17f, 2f, sage)
            rounded(cx + 3f, cy - 8f, 5f, 17f, 2f, sage)
        } else {
            line(cx + 4f, cy + 8f, cx - 4f, cy)
            line(cx - 4f, cy, cx + 4f, cy - 8f)
        }
    }

    fun imageTile(id: String,x:Float,y:Float,w:Float,h:Float) {
        val original=TileAssetResolver.tileAssetFor(id) ?: return
        val path=if(original.endsWith(".webp")) original.replace("tiles/","tiles-native/").removeSuffix(".webp")+".png" else original
        if(!Gdx.files.internal(path).exists())return
        val icon=texture(path);val scale=min(w/icon.width,h/icon.height)
        batch.draw(icon,x+originX+(w-icon.width*scale)/2f,y+originY+(h-icon.height*scale)/2f,icon.width*scale,icon.height*scale)
    }

    fun wallet(amount:Int,rect:Rectangle) {
        rounded(rect.x,rect.y-2f,rect.width,rect.height,24f,Color.valueOf("D5DFD0"))
        rounded(rect.x,rect.y,rect.width,rect.height,24f,cream)
        val size=min(42f,rect.height-6f)
        imageTile("candy_3",rect.x+6f,rect.y+(rect.height-size)/2f,size,size)
        label(amount.toString(),rect.x+size+10f,rect.y+rect.height/2f,min(22f,rect.height*.5f),ink,rect.width-size-18f,centeredVertically=true)
    }

    fun star(cx:Float,cy:Float,r:Float,filled:Boolean) {
        val color=if(filled)Color.valueOf("E8AD45") else Color.valueOf("DADFD3")
        val packed=color.toFloatBits()
        for(i in 0 until 10) {
            val a=(90.0+i*36.0)*Math.PI/180.0
            val b=a+Math.PI/5.0
            val ra=if(i%2==0)r else r*.47f
            val rb=if(i%2==0)r*.47f else r
            val px=cx+kotlin.math.cos(a).toFloat()*ra
            val py=cy+kotlin.math.sin(a).toFloat()*ra
            val qx=cx+kotlin.math.cos(b).toFloat()*rb
            val qy=cy+kotlin.math.sin(b).toFloat()*rb
            quad(cx,cy,packed,px,py,packed,qx,qy,packed,qx,qy,packed)
        }
    }

    fun navButton(rect:Rectangle,icon:String,title:String) {
        if (icon == "settings") {
            val cx=rect.x+rect.width/2f;val cy=rect.y+52f
            rounded(cx-22f,cy-22f,44f,44f,22f,Color.valueOf("DFEBDB"))
            val packed=sage.toFloatBits()
            for(i in 0 until 32) {
                val a=i*Math.PI/16.0;val b=(i+1)*Math.PI/16.0
                val ra=if(i%4 in 1..2)18f else 14f
                val rb=if((i+1)%4 in 1..2)18f else 14f
                val px=cx+kotlin.math.cos(a).toFloat()*ra;val py=cy+kotlin.math.sin(a).toFloat()*ra
                val qx=cx+kotlin.math.cos(b).toFloat()*rb;val qy=cy+kotlin.math.sin(b).toFloat()*rb
                quad(cx,cy,packed,px,py,packed,qx,qy,packed,qx,qy,packed)
            }
            rounded(cx-6f,cy-6f,12f,12f,6f,cream)
        } else art(icon,rect.x+rect.width/2f-22f,rect.y+30f,44f,44f)
        text(title,rect.x,rect.y+19f,.82f,ink,rect.width)
    }

    fun toggle(rect:Rectangle,on:Boolean) {
        val trackW=70f;val trackH=36f
        val x=rect.x+(rect.width-trackW)/2f;val y=rect.y+(rect.height-trackH)/2f
        rounded(x,y,trackW,trackH,18f,if(on)sage else Color.valueOf("D4DCD0"))
        rounded(x+(if(on)37f else 4f),y+4f,29f,28f,14f,cream)
    }

    fun gameHeader(title: String, subtitle: String, score: Int, seconds: Int, duration: Int, layout: PuzzleLayout) {
        iconButton(layout.back, "back"); iconButton(layout.pause, "pause")
        heading(title, 80f, height - 40f, 32f, width - 160f)
        text(subtitle, 80f, height - 75f, 0.78f, muted, width - 160f)
        panel(Rectangle(28f, height - 152f, width - 56f, 60f))
        text("SCORE", 48f, height - 104f, 0.61f, muted)
        text(score.toString().padStart(3, '0'), 48f, height - 120f, 1.18f, ink)
        val minutes = seconds / 60; val remainder = seconds % 60
        val time = "$minutes:${remainder.toString().padStart(2, '0')}"
        text("TIME LEFT", width - 150f, height - 104f, 0.61f, muted)
        text(time, width - 150f, height - 120f, 1.18f, if (seconds < 20) Color.valueOf("BE745C") else ink)
        val barX = 46f; val barWidth = width - 92f
        rounded(barX, height - 146f, barWidth, 5f, 3f, Color.valueOf("E4E8DC"))
        rounded(barX, height - 146f, barWidth * (seconds.toFloat() / duration).coerceIn(0f, 1f), 5f, 3f, sage)
    }

    fun tool(rect: Rectangle, icon: String, title: String, enabled: Boolean = true) {
        panel(rect, if (enabled) cream else Color.valueOf("E4E9DC"))
        art(icon, rect.x + 16f, rect.y + 11f, 46f, 46f, if (enabled) 1f else 0.4f)
        label(title, rect.x + 68f, rect.y + rect.height / 2f, 22f,
            if (enabled) ink else muted, rect.width - 82f, centeredVertically = true)
    }

    fun slot(rect: Rectangle) {
        patch("slot-mint").draw(batch, rect.x + originX, rect.y + originY, rect.width, rect.height)
    }

    fun line(x1: Float, y1: Float, x2: Float, y2: Float) {
        val dx = x2 - x1; val dy = y2 - y1
        val length = kotlin.math.sqrt(dx * dx + dy * dy)
        batch.color = sage
        batch.draw(pixel, x1 + originX, y1 + originY - 1.5f, 0f, 1.5f, length, 3f, 1f, 1f,
            Math.toDegrees(kotlin.math.atan2(dy, dx).toDouble()).toFloat(), 0, 0, 1, 1, false, false)
        batch.color = Color.WHITE
    }

    override fun dispose() {
        textures.values.forEach { it.dispose() }
        textures.clear()
        pixel.dispose(); circle.dispose()
        sourcePixmaps.forEach { it.dispose() }
        batch.dispose()
    }
}

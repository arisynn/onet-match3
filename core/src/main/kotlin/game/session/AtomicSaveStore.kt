package game.session

import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlinx.serialization.json.*

/** Offline integrity detects corruption, not a hostile user who can modify the APK. */
class AtomicSaveStore(private val directory: File) {
    private val primary get() = File(directory, "player-v1.json")
    private val backup get() = File(directory, "player-v1.backup.json")
    var recoveredFromBackup: Boolean = false
        private set
    private var writable = true
    var loadFailure: String? = null
        private set

    @Synchronized fun load(): SaveState? {
        recoveredFromBackup = false
        loadFailure = null
        for (file in listOf(primary, backup)) {
            if (!file.exists()) continue
            try {
                val state = readValid(file)
                recoveredFromBackup = file == backup
                return state
            } catch (e: Exception) {
                loadFailure = e.message ?: "Invalid save"
                if (e.message == "Unsupported save version") { writable = false; return null }
            }
        }
        return null
    }

    @Synchronized fun save(state: SaveState) {
        check(writable) { "Save from a newer version must not be overwritten" }
        SaveCodec.validate(state)
        check(directory.exists() || directory.mkdirs()) { "Cannot create save directory" }
        val payload = SaveCodec.encode(state)
        val bytes = buildJsonObject {
            put("payload", payload); put("sha256", hash(payload))
        }.toString().toByteArray(Charsets.UTF_8)
        val temp = File(directory, "player-v1.pending")
        FileOutputStream(temp).use { stream -> stream.write(bytes); stream.fd.sync() }
        // On Android/Linux rename within a filesystem atomically replaces the destination.
        // Keep the last committed valid snapshot before switching the primary.
        if (primary.exists() && runCatching { readValid(primary) }.isSuccess) {
            val pendingBackup = File(directory, "player-v1.backup.pending")
            FileOutputStream(pendingBackup).use { out -> primary.inputStream().use { it.copyTo(out) }; out.fd.sync() }
            check(pendingBackup.renameTo(backup)) { "Cannot replace backup save" }
        }
        check(temp.renameTo(primary)) { "Cannot commit save" }
    }

    private fun readValid(file: File): SaveState {
        require(file.length() in 1..8_000_000) { "Invalid save length" }
        val envelope = Json.parseToJsonElement(file.readText()).jsonObject
        val payload = envelope.getValue("payload").jsonPrimitive.content
        require(envelope.getValue("sha256").jsonPrimitive.content == hash(payload)) { "Checksum mismatch" }
        require(Json.parseToJsonElement(payload).jsonObject.getValue("schemaVersion").jsonPrimitive.int == 1) { "Unsupported save version" }
        return SaveCodec.decode(payload).also { SaveCodec.validate(it) }
    }

    private fun hash(value: String) = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}

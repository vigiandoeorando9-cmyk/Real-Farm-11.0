package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.domain.AudioChannelSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

data class EngineAudioProfile(
    val label: String,
    val baseFreqHz: Double,
    val cylinders: Int,
    val hasTurboWhine: Boolean,
    val hasAirBrake: Boolean,
)

data class VehicleAudioSnapshot(
    val inVehicle: Boolean,
    val speedMs: Double,
    val rpm: Double,
    val gear: Int,
    val engineRunning: Boolean,
    val surfaceType: String,
    val soundProfile: String,
    val horsepower: Double,
    val implementActive: Boolean,
    val isSprayerActive: Boolean,
    val isBraking: Boolean,
    val engineLoadPct: Double = 0.0,
    val hasAirBrake: Boolean = false,
)

fun resolveEngineAudioProfile(soundProfile: String, horsepower: Double): EngineAudioProfile {
    val low = soundProfile.lowercase()
    return when {
        low.contains("moto") -> EngineAudioProfile("Monocilíndrico Moto", 42.0, 1, false, false)
        low.contains("mercedes") || low.contains("caminhão") || low.contains("scania") || low.contains("volvo") ->
            EngineAudioProfile("Diesel Rodoviário 6L", 28.0, 6, true, true)
        low.contains("colheitadeira") || low.contains("axial") ->
            EngineAudioProfile("Colheitadeira Axial 9L", 31.0, 6, true, false)
        horsepower < 55.0 -> EngineAudioProfile("Diesel 2/3-Cilindros Clássico", 29.0, 3, false, false)
        horsepower > 250.0 -> EngineAudioProfile("Diesel Heavy Duty 6-Cil Turbo", 27.0, 6, true, true)
        else -> EngineAudioProfile("Diesel Agrícola 4-Cilindros", 32.0, 4, low.contains("turbo"), false)
    }
}

private data class ActiveToneEffect(
    val kind: String,
    var ageSec: Double,
    val durationSec: Double,
)

class AmbientAudioEngine(private val scope: CoroutineScope) {
    private val sampleRate = 22050
    private val trackLock = Any()
    private var renderJob: Job? = null
    private var audioTrack: AudioTrack? = null

    @Volatile
    private var running = false

    @Volatile
    private var enabled = false

    @Volatile
    private var channels = AudioChannelSettings()

    @Volatile
    private var rainLevel = 0.0

    @Volatile
    private var hourOfDay = 10.0

    @Volatile
    private var vehicleSnap = VehicleAudioSnapshot(
        inVehicle = false,
        speedMs = 0.0,
        rpm = 0.0,
        gear = 0,
        engineRunning = false,
        surfaceType = "terra",
        soundProfile = "",
        horsepower = 90.0,
        implementActive = false,
        isSprayerActive = false,
        isBraking = false,
        engineLoadPct = 0.0,
        hasAirBrake = false,
    )

    private val pendingEffects = ConcurrentLinkedQueue<ActiveToneEffect>()
    private val activeEffects = mutableListOf<ActiveToneEffect>()

    fun start() {
        if (running) return
        running = true
        renderJob = scope.launch(Dispatchers.Default) {
            try {
                val minBuf = runCatching {
                    AudioTrack.getMinBufferSize(
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                    )
                }.getOrDefault(4096).coerceAtLeast(2048)

                val track = runCatching {
                    AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_GAME)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(sampleRate)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build()
                        )
                        .setBufferSizeInBytes(minBuf * 2)
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .build()
                }.getOrNull()

                synchronized(trackLock) {
                    audioTrack = track
                }
                var isPlaying = false
                val chunkSamples = (minBuf / 2).coerceAtLeast(512)
                val pcm = ShortArray(chunkSamples)
                var engPhase = 0.0
                var turboPhase = 0.0
                var ptoPhase = 0.0
                var chordPhase = 0.0
                var lpRain = 0.0
                val dt = 1.0 / sampleRate

                while (isActive && running) {
                    while (true) {
                        val nextFx = pendingEffects.poll() ?: break
                        activeEffects.add(nextFx)
                    }

                    if (!enabled || channels.master <= 0.01) {
                        if (isPlaying) {
                            synchronized(trackLock) {
                                runCatching { audioTrack?.pause() }
                                runCatching { audioTrack?.flush() }
                            }
                            isPlaying = false
                        }
                        delay(60L)
                        continue
                    }

                    if (!isPlaying) {
                        synchronized(trackLock) {
                            runCatching { audioTrack?.play() }
                        }
                        isPlaying = true
                    }

                    val snap = vehicleSnap
                    val profile = resolveEngineAudioProfile(snap.soundProfile, snap.horsepower)
                    val masterGain = channels.master.coerceIn(0.0, 1.0)
                    val engineGain = channels.engine * 0.28
                    val weatherGain = channels.weather * rainLevel * 0.24
                    val ambientGain = channels.ambient * 0.04
                    val musicGain = channels.music * 0.035

                    val rpmNorm = ((snap.rpm - 650.0) / 1950.0).coerceIn(0.0, 1.25)
                    val loadNorm = (snap.engineLoadPct / 100.0).coerceIn(0.0, 1.0)
                    val cylFactor = when {
                        profile.cylinders <= 1 -> 1.18
                        profile.cylinders <= 3 -> 0.88
                        profile.cylinders == 4 -> 1.0
                        else -> 1.12
                    }
                    val baseHz = if (snap.inVehicle && snap.engineRunning) {
                        profile.baseFreqHz * cylFactor * (0.82 + rpmNorm * 1.92)
                    } else 0.0

                    for (i in 0 until chunkSamples) {
                        var mix = 0.0

                        if (baseHz > 1.0) {
                            engPhase = (engPhase + baseHz * dt * 2.0 * PI) % (2.0 * PI)
                            val fundamental = sin(engPhase)
                            val loadBite = 0.22 + loadNorm * 0.45
                            val pulse = sin(engPhase * 2.0) * (0.48 + loadBite * 0.25) +
                                sin(engPhase * 3.0) * (0.24 + loadBite * 0.20) +
                                sin(engPhase * 0.5) * (0.14 + loadNorm * 0.24)
                            val exhaustGain = engineGain * (0.85 + loadNorm * 0.48)
                            mix += (fundamental * 0.62 + pulse) * exhaustGain
                            if (profile.hasTurboWhine && (rpmNorm > 0.30 || loadNorm > 0.40)) {
                                val spool = ((rpmNorm * 0.65 + loadNorm * 0.35) - 0.28).coerceIn(0.0, 1.0)
                                turboPhase = (turboPhase + (1250.0 + rpmNorm * 1650.0 + loadNorm * 400.0) * dt * 2.0 * PI) % (2.0 * PI)
                                mix += sin(turboPhase) * spool * 0.095 * channels.engine
                            }
                        }

                        if (snap.implementActive || snap.isSprayerActive) {
                            ptoPhase = (ptoPhase + (142.0 + rpmNorm * 34.0) * dt * 2.0 * PI) % (2.0 * PI)
                            mix += sin(ptoPhase) * 0.07 * channels.implements
                        }

                        val white = Random.nextDouble(-1.0, 1.0)
                        lpRain = lpRain * 0.86 + white * 0.14
                        mix += lpRain * weatherGain
                        mix += lpRain * ambientGain * 0.45

                        if (musicGain > 0.004) {
                            chordPhase = (chordPhase + 196.0 * dt * 2.0 * PI) % (2.0 * PI)
                            val violaPad = sin(chordPhase) * 0.5 + sin(chordPhase * 1.25) * 0.35 + sin(chordPhase * 1.5) * 0.3
                            mix += violaPad * musicGain
                        }

                        if (activeEffects.isNotEmpty()) {
                            val iter = activeEffects.iterator()
                            while (iter.hasNext()) {
                                val fx = iter.next()
                                val env = (1.0 - (fx.ageSec / fx.durationSec)).coerceIn(0.0, 1.0)
                                when (fx.kind) {
                                    "horn" -> {
                                        val t = fx.ageSec * 2.0 * PI
                                        mix += (sin(392.0 * t) + sin(493.88 * t) * 0.85) * 0.16 * env * channels.vehicles
                                    }
                                    "click" -> {
                                        mix += sin(fx.ageSec * 1200.0 * 2.0 * PI) * 0.12 * env * channels.ui
                                    }
                                    "gear_shift" -> {
                                        val t = fx.ageSec * 2.0 * PI
                                        mix += (sin(220.0 * t) * 0.65 + sin(95.0 * t) * 0.55) * 0.16 * env * channels.vehicles
                                    }
                                    "air_brake" -> {
                                        mix += white * 0.19 * env * env * channels.vehicles
                                    }
                                    "engine_start" -> {
                                        mix += sin(fx.ageSec * 110.0 * 2.0 * PI) * 0.18 * env * channels.engine
                                    }
                                    "dump_grain" -> {
                                        mix += lpRain * 0.25 * env * channels.implements
                                    }
                                    "thunder" -> {
                                        mix += (lpRain * 0.45 + sin(fx.ageSec * 52.0 * 2.0 * PI) * 0.25) * env * channels.weather
                                    }
                                    else -> {
                                        mix += sin(fx.ageSec * 580.0 * 2.0 * PI) * 0.09 * env * channels.ui
                                    }
                                }
                                fx.ageSec += dt
                                if (fx.ageSec >= fx.durationSec) iter.remove()
                            }
                        }

                        val clamped = (mix * masterGain).coerceIn(-0.95, 0.95)
                        pcm[i] = (clamped * Short.MAX_VALUE).toInt().toShort()
                    }

                    val wrote = synchronized(trackLock) {
                        val t = audioTrack
                        if (t != null && running) {
                            runCatching { t.write(pcm, 0, pcm.size) }.getOrDefault(-1)
                        } else -1
                    }
                    if (wrote <= 0) {
                        delay(25L)
                    }
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (_: Throwable) {
                // Prevent audio subsystem device faults from crashing gameplay
            } finally {
                synchronized(trackLock) {
                    runCatching { audioTrack?.stop() }
                    runCatching { audioTrack?.release() }
                    audioTrack = null
                }
            }
        }
    }

    fun setEnabled(on: Boolean) {
        enabled = on
    }

    fun updateParameters(
        rain: Double,
        wind: Double,
        hour: Double,
        channelSettings: AudioChannelSettings,
    ) {
        rainLevel = (rain + wind * 0.15).coerceIn(0.0, 1.0)
        hourOfDay = hour
        channels = channelSettings
    }

    fun updateVehicleAudio(snap: VehicleAudioSnapshot) {
        vehicleSnap = snap
    }

    fun playHorn() = enqueue("horn", 0.38)
    fun playEngineStart() = enqueue("engine_start", 0.45)
    fun playEngineStop() = enqueue("click", 0.15)
    fun playGearShift() = enqueue("gear_shift", 0.09)
    fun playAirBrakeHiss() = enqueue("air_brake", 0.34)
    fun playGrainUnload() = enqueue("dump_grain", 0.55)
    fun playPurchaseSuccess() = enqueue("click", 0.14)
    fun playThunder() = enqueue("thunder", 1.2)
    fun playUiClick() = enqueue("click", 0.06)

    private fun enqueue(kind: String, durationSec: Double) {
        if (!enabled) return
        if (pendingEffects.size < 12) {
            pendingEffects.add(ActiveToneEffect(kind, 0.0, durationSec))
        }
    }

    fun release() {
        running = false
        renderJob?.cancel()
        renderJob = null
        synchronized(trackLock) {
            runCatching { audioTrack?.stop() }
            runCatching { audioTrack?.release() }
            audioTrack = null
        }
    }
}

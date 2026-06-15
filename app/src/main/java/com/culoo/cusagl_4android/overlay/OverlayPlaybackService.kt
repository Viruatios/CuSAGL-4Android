package com.culoo.cusagl_4android.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.culoo.cusagl_4android.MainActivity
import com.culoo.cusagl_4android.R
import com.culoo.cusagl_4android.accessibility.AccessibilityServiceBridge
import com.culoo.cusagl_4android.accessibility.AccessibilityServiceListener
import com.culoo.cusagl_4android.accessibility.AccessibilityTouchInjector
import com.culoo.cusagl_4android.accessibility.LyreAccessibilityService
import com.culoo.cusagl_4android.accessibility.TouchCoordinateMapper
import com.culoo.cusagl_4android.core.DefaultLogger
import com.culoo.cusagl_4android.core.LogTags
import com.culoo.cusagl_4android.core.PlaybackSnapshot
import com.culoo.cusagl_4android.core.PlaybackSnapshotListener
import com.culoo.cusagl_4android.core.RuntimePlaybackEngine
import com.culoo.cusagl_4android.core.ScoreCacheProvider
import com.culoo.cusagl_4android.ui.theme.CuSAGL4AndroidTheme
import kotlin.math.roundToInt

class OverlayPlaybackService : Service(), LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner,
    AccessibilityServiceListener {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    private val serviceViewModelStore = ViewModelStore()
    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var overlayWidth = 0
    private var overlayHeight = 0
    private var positionInitialized = false
    private var positionX = 0f
    private var positionY = 0f

    private var touchInjector: AccessibilityTouchInjector? = null
    private var playbackEngine: RuntimePlaybackEngine? = null
    private var snapshot by mutableStateOf(PlaybackSnapshot())
    private val snapshotListener = PlaybackSnapshotListener { updated ->
        mainHandler.post {
            snapshot = updated
        }
    }

    private val permissionCheck = object : Runnable {
        override fun run() {
            if (!OverlayPermission.canDraw(this@OverlayPlaybackService)) {
                DefaultLogger.w(LogTags.OVERLAY, "Overlay permission was revoked")
                shutdown()
                return
            }
            mainHandler.postDelayed(this, OverlayConstants.PERMISSION_CHECK_INTERVAL_MS)
        }
    }

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry

    override val viewModelStore: ViewModelStore
        get() = serviceViewModelStore

    override fun onCreate() {
        super.onCreate()
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        windowManager = getSystemService(WindowManager::class.java)
        AccessibilityServiceBridge.registerListener(this)
        createNotificationChannel()
        startForegroundCompat(buildNotification())
        mainHandler.post(permissionCheck)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val request = intent?.let(PlaybackSessionRequest::from)
        if (request == null || !OverlayPermission.canDraw(this) || !AccessibilityServiceBridge.isConnected()) {
            DefaultLogger.w(LogTags.OVERLAY, "Cannot start overlay playback session")
            shutdown()
            return START_NOT_STICKY
        }

        createPlaybackSession(request)
        if (overlayView == null) {
            createOverlay()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mainHandler.post(::applyConstrainedPosition)
    }

    override fun onServiceAvailable(service: LyreAccessibilityService) {
        // The active session can continue.
    }

    override fun onServiceUnavailable() {
        mainHandler.post {
            DefaultLogger.w(LogTags.ACCESSIBILITY, "Accessibility service disconnected during playback session")
            shutdown()
        }
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(permissionCheck)
        AccessibilityServiceBridge.unregisterListener(this)
        playbackEngine?.removeSnapshotListener(snapshotListener)
        playbackEngine?.stop()
        playbackEngine = null
        touchInjector?.close()
        touchInjector = null
        removeOverlay()
        serviceViewModelStore.clear()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        super.onDestroy()
    }

    private fun createPlaybackSession(request: PlaybackSessionRequest) {
        playbackEngine?.removeSnapshotListener(snapshotListener)
        playbackEngine?.stop()
        touchInjector?.close()

        val injector = AccessibilityTouchInjector(
            serviceProvider = AccessibilityServiceBridge::getService,
            coordinateMapper = TouchCoordinateMapper(windowManager)
        )
        touchInjector = injector
        playbackEngine = RuntimePlaybackEngine(
            cacheProvider = ScoreCacheProvider(filesDir),
            touchInjector = injector
        ).also { engine ->
            engine.updateConfig(request.config)
            engine.updateQueue(request.queue)
            engine.addSnapshotListener(snapshotListener)
        }
    }

    private fun createOverlay() {
        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayPlaybackService)
            setViewTreeSavedStateRegistryOwner(this@OverlayPlaybackService)
            setViewTreeViewModelStoreOwner(this@OverlayPlaybackService)
            setContent {
                CuSAGL4AndroidTheme {
                    OverlayPlaybackPanel(
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            onOverlayMeasured(coordinates.size.width, coordinates.size.height)
                        },
                        snapshot = snapshot,
                        onDrag = ::moveOverlayBy,
                        onStart = { playbackEngine?.start() },
                        onPause = { playbackEngine?.pause() },
                        onStop = ::shutdown,
                        onPrevious = { playbackEngine?.previous() },
                        onNext = { playbackEngine?.next() }
                    )
                }
            }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = topInset()
        }
        try {
            windowManager.addView(view, params)
            overlayView = view
            layoutParams = params
            lifecycleRegistry.currentState = Lifecycle.State.STARTED
        } catch (ex: Exception) {
            DefaultLogger.e(LogTags.OVERLAY, "Failed to create overlay", ex)
            shutdown()
        }
    }

    private fun onOverlayMeasured(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        overlayWidth = width
        overlayHeight = height
        val bounds = windowManager.currentWindowMetrics.bounds
        if (!positionInitialized) {
            val initial = OverlayPositionMapper.initialPosition(
                widthPx = bounds.width(),
                heightPx = bounds.height(),
                overlayWidthPx = width,
                overlayHeightPx = height,
                topInsetPx = topInset()
            )
            positionX = initial.x.toFloat()
            positionY = initial.y.toFloat()
            positionInitialized = true
        }
        applyConstrainedPosition()
    }

    private fun moveOverlayBy(deltaX: Float, deltaY: Float) {
        if (!snapshot.canMoveOverlay || overlayWidth <= 0 || overlayHeight <= 0) return
        positionX += deltaX
        positionY += deltaY
        applyConstrainedPosition()
    }

    private fun applyConstrainedPosition() {
        val params = layoutParams ?: return
        val view = overlayView ?: return
        if (overlayWidth <= 0 || overlayHeight <= 0) return
        val bounds = windowManager.currentWindowMetrics.bounds
        val topInset = topInset()
        val constrained = OverlayPositionMapper.constrain(
            requestedX = positionX.roundToInt(),
            requestedY = positionY.roundToInt(),
            widthPx = bounds.width(),
            heightPx = bounds.height(),
            overlayWidthPx = overlayWidth,
            overlayHeightPx = overlayHeight,
            topInsetPx = topInset
        )
        positionX = constrained.x.toFloat()
        positionY = constrained.y.toFloat()
        params.x = constrained.x
        params.y = constrained.y
        try {
            windowManager.updateViewLayout(view, params)
        } catch (ex: Exception) {
            DefaultLogger.w(LogTags.OVERLAY, "Failed to update overlay position", ex)
        }
    }

    private fun topInset(): Int {
        val metrics = windowManager.currentWindowMetrics
        return metrics.windowInsets
            .getInsetsIgnoringVisibility(android.view.WindowInsets.Type.systemBars())
            .top
    }

    private fun removeOverlay() {
        val view = overlayView ?: return
        overlayView = null
        layoutParams = null
        try {
            windowManager.removeView(view)
        } catch (ex: Exception) {
            DefaultLogger.w(LogTags.OVERLAY, "Failed to remove overlay", ex)
        }
    }

    private fun shutdown() {
        playbackEngine?.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                OverlayConstants.NOTIFICATION_CHANNEL_ID,
                getString(R.string.overlay_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            )
        )
    }

    private fun buildNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, OverlayConstants.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(OverlayConstants.NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(OverlayConstants.NOTIFICATION_ID, notification)
        }
    }

    companion object {
        fun start(context: Context, request: PlaybackSessionRequest) {
            val intent = request.writeTo(Intent(context, OverlayPlaybackService::class.java))
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, OverlayPlaybackService::class.java))
        }
    }
}

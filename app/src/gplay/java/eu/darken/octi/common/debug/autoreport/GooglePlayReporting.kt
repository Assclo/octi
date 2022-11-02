package eu.darken.octi.common.debug.autoreport

import android.app.Application
import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.getkeepsafe.relinker.ReLinker
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.octi.App
import eu.darken.octi.common.BuildConfigWrap
import eu.darken.octi.common.InstallId
import eu.darken.octi.common.datastore.valueBlocking
import eu.darken.octi.common.debug.AutomaticBugReporter
import eu.darken.octi.common.debug.Bugs
import eu.darken.octi.common.debug.autoreport.bugsnag.BugsnagErrorHandler
import eu.darken.octi.common.debug.autoreport.bugsnag.BugsnagLogger
import eu.darken.octi.common.debug.autoreport.bugsnag.NOPBugsnagErrorHandler
import eu.darken.octi.common.debug.logging.Logging
import eu.darken.octi.common.debug.logging.log
import eu.darken.octi.common.debug.logging.logTag
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class GooglePlayReporting @Inject constructor(
    @ApplicationContext private val context: Context,
    private val debugSettings: DebugSettings,
    private val installId: InstallId,
    private val bugsnagLogger: Provider<BugsnagLogger>,
    private val bugsnagErrorHandler: Provider<BugsnagErrorHandler>,
    private val nopBugsnagErrorHandler: Provider<NOPBugsnagErrorHandler>,
) : AutomaticBugReporter {

    override fun setup(application: Application) {
        val isEnabled = debugSettings.isAutoReportingEnabled.valueBlocking
        log(TAG) { "setup(): isEnabled=$isEnabled" }

        if (isEnabled) {
            ReLinker
                .log { message -> log(App.TAG) { "ReLinker: $message" } }
                .loadLibrary(application, "bugsnag-plugin-android-anr")
        }
        try {
            val bugsnagConfig = Configuration.load(context).apply {
                if (debugSettings.isAutoReportingEnabled.valueBlocking) {
                    Logging.install(bugsnagLogger.get())
                    setUser(installId.id, null, null)
                    autoTrackSessions = true
                    addOnError(bugsnagErrorHandler.get())
                    addMetadata("App", "buildFlavor", BuildConfigWrap.FLAVOR)
                    log(TAG) { "Bugsnag setup done!" }
                } else {
                    autoTrackSessions = false
                    addOnError(nopBugsnagErrorHandler.get())
                    log(TAG) { "Installing Bugsnag NOP error handler due to user opt-out!" }
                }
            }

            Bugsnag.start(context, bugsnagConfig)
            Bugs.reporter = this
        } catch (e: IllegalStateException) {
            log(TAG) { "Bugsnag API Key not configured." }
        }
    }

    override fun notify(throwable: Throwable) {
        // NOOP
    }

    companion object {
        private val TAG = logTag("Debug", "GooglePlayReporting")
    }
}
package de.juvento.plain

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.os.UserHandle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import de.juvento.plain.data.AppModel
import de.juvento.plain.helper.getAppsList
import de.juvento.plain.helper.getHiddenAppsList
import de.juvento.plain.helper.showToastShort
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext: Context = application.applicationContext

    val appList = MutableLiveData<List<AppModel>?>()
    val hiddenApps = MutableLiveData<List<AppModel>?>()

    fun launchApp(appModel: AppModel) =
        launchApp(appModel.appPackage, appModel.appActivityName, appModel.user, appModel.shortcutId.orEmpty())

    fun launchApp(packageName: String, activityName: String, userHandle: UserHandle, shortcutId: String = "") {
        if (shortcutId.isNotEmpty()) {
            launchShortcut(packageName, shortcutId, userHandle)
            return
        }

        val launcher = appContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val activities = launcher.getActivityList(packageName, userHandle)

        // Prefer the stored activity; fall back to the last launcher activity of the package.
        val activity = activities.firstOrNull { it.name == activityName } ?: activities.lastOrNull()
        if (activity == null) {
            showToastShort(appContext, appContext.getString(R.string.app_not_found))
            return
        }
        val component = ComponentName(packageName, activity.name)

        try {
            launcher.startMainActivity(component, userHandle, null, null)
        } catch (e: SecurityException) {
            try {
                launcher.startMainActivity(component, android.os.Process.myUserHandle(), null, null)
            } catch (e: Exception) {
                showToastShort(appContext, appContext.getString(R.string.unable_to_launch_app))
            }
        } catch (e: Exception) {
            showToastShort(appContext, appContext.getString(R.string.unable_to_launch_app))
        }
    }

    private fun launchShortcut(packageName: String, shortcutId: String, userHandle: UserHandle) {
        val launcher = appContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        try {
            launcher.startShortcut(packageName, shortcutId, null, null, userHandle)
        } catch (e: Exception) {
            showToastShort(appContext, appContext.getString(R.string.unable_to_launch_app))
        }
    }

    fun getAppList(showHiddenApps: Boolean = false) {
        viewModelScope.launch {
            appList.value = getAppsList(appContext, showHiddenApps)
        }
    }

    fun getHiddenApps() {
        viewModelScope.launch {
            hiddenApps.value = getHiddenAppsList(appContext)
        }
    }

}

package com.taskdata.vpplugin.nicksmanager

import com.vp.plugin.VPPlugin
import com.vp.plugin.VPPluginInfo

class NicksManagerPlugin : VPPlugin, VPHelper {
    override fun unloaded() {

    }

    override fun loaded(info: VPPluginInfo) {
        showMessage("Loaded ${info.pluginId}, plugin dir: ${info.pluginDir}")
    }
}
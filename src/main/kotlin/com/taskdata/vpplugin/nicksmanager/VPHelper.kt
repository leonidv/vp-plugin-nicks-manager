package com.taskdata.vpplugin.nicksmanager

import com.vp.plugin.ApplicationManager
import com.vp.plugin.model.IProject

interface VPHelper {

    val project: IProject
        get() = ApplicationManager.instance().projectManager.project


    fun showMessage(msg : String) {
        val viewManager = ApplicationManager.instance().viewManager
        viewManager.showMessage(msg)
    }
}
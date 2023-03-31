package com.vygovskiy.vpplugin.nicksmanager

import com.vp.plugin.ApplicationManager
import com.vp.plugin.action.VPAction
import com.vp.plugin.action.VPActionController
import com.vp.plugin.diagram.IDiagramElement
import com.vp.plugin.diagram.IDiagramUIModel
import com.vp.plugin.model.IModelElement

class SwapNicknamesActionController : VPActionController, VPHelper {
    override fun update(action: VPAction) {

    }

    override fun performAction(action: VPAction) {
        val diagram: IDiagramUIModel = ApplicationManager.instance().diagramManager.activeDiagram;
        val rootFrame = ApplicationManager.instance().viewManager.rootFrame;
        val nicknames = arrayOf(NICKNAME_ORIGINAL, *project.nicknames)
        val initialProjectNickname = project.nickname

        val diagramElementIterator = diagram.diagramElementIterator() as Iterator<IDiagramElement>;
        val nicksManagerDialog = NicksManagerDialog.build(rootFrame, nicknames, diagramElementIterator);
        nicksManagerDialog.isVisible = true;

        project.nickname = initialProjectNickname
    }
}
package com.taskdata.vpplugin.nicksmanager

import com.vp.plugin.model.IModelElement
import com.vp.plugin.model.IProject

fun IModelElement.nickname(language: String): String {
    project.nickname = language.toVPnicknameId()
    return "${this.nickname}@${project.nickname}"
}

fun String.toVPnicknameId(): String? {
    return if (this == NICKNAME_ORIGINAL) {
        null
    } else {
        this
    }
}

fun IModelElement.setNickname(language: String, value: String) {
    if (language == NICKNAME_ORIGINAL) {
        project.nickname = null
        this.name = value
    } else {
        project.nickname = language
        this.nickname = value
    }
}

data class Info(
    val nickname: String,
    val name: String?,
    val description: String?,
    val documentation: String?,
    val htmlDescription: String?,
    val htmlDocumentation: String?
) {
    companion object : VPHelper {
        fun formNick(elem: IModelElement, nickname: String): Info {
            return Info(
                nickname = nickname,
                name = elem.nickname,
                description = elem.nickDescription,
                documentation = elem.nickDocumentation,
                htmlDescription = elem.nickHTMLDescription,
                htmlDocumentation = elem.nickHTMLDocumentation
            )
        }

        fun fromOriginal(elem: IModelElement, nickname: String): Info {
            return Info(
                nickname = nickname,
                name = elem.name,
                description = elem.description,
                documentation = elem.documentation,
                htmlDescription = elem.htmlDescription,
                htmlDocumentation = elem.htmlDocumentation
            )
        }

    }

    @Suppress("DuplicatedCode")
    fun copyTo(elem: IModelElement, nickname: String, project: IProject, dryRun: Boolean, messageSpacePrefix: String) {
        project.nickname = nickname.toVPnicknameId()
        showMessage("$messageSpacePrefix ${elem.name}: ${elem.nickname} <== ${this.name}")

        if (dryRun) return

        elem.nickname = this.name
        elem.nickDescription = this.description
        elem.nickDocumentation = this.documentation
        elem.nickHTMLDescription = this.htmlDescription
        elem.nickHTMLDocumentation = this.htmlDocumentation
    }
}

val nicknameSwapper = NicknameSwapper()

const val NICKNAME_ORIGINAL = "Original"

enum class SwapMode {
    SWAP,
    COPY
}

typealias OnCompleteCallback = (IModelElement) -> Unit

class NicknameSwapper : VPHelper {
    private fun getAllInfo(elem: IModelElement): Map<String, Info> {

        val infoByNickname = mutableMapOf<String, Info>()
        project.nickname = null;
        infoByNickname[NICKNAME_ORIGINAL] = Info.fromOriginal(elem, NICKNAME_ORIGINAL)

        project.nicknames.forEach {nickname ->
            Info.project.nickname = nickname
            infoByNickname[nickname] = Info.formNick(elem, nickname)
        }
        return infoByNickname
    }

    fun process(
        elem: IModelElement,
        sourceNickname: String,
        targetNickname: String,
        mode: SwapMode,
        processChildren: Boolean,
        dryRun: Boolean,
        alreadyProcessed: Set<IModelElement>,
        messageSpacePrefix : String,
        onComplete: OnCompleteCallback?
    ) : Set<IModelElement> {
        showMessage("$messageSpacePrefix Process ${elem.name} (source: $sourceNickname, target: $targetNickname)")
        if (alreadyProcessed.contains(elem)) {
            showMessage("$messageSpacePrefix already processed")
            return alreadyProcessed
        }

        val project = elem.project
        val nicknames = getAllInfo(elem)
        val sourceInfo = nicknames[sourceNickname]
        val targetInfo = nicknames[targetNickname]

        val processed = alreadyProcessed.toMutableSet()

        requireNotNull(sourceInfo) {"nickname $sourceInfo is not exists in project"}
        requireNotNull(targetInfo) {"nickname $targetInfo is not exists in project"}

        when (mode) {
            SwapMode.SWAP -> {
                sourceInfo.copyTo(elem, targetNickname, project, dryRun, messageSpacePrefix)
                targetInfo.copyTo(elem, sourceNickname, project, dryRun, messageSpacePrefix)
            }

            SwapMode.COPY -> {
                sourceInfo.copyTo(elem, targetNickname, project, dryRun, messageSpacePrefix)
            }
        }

        processed.add(elem)

        if (processChildren) {
            elem.childIterator().forEach {
                val child = it as IModelElement
                val processedChildren = process(
                    elem = child,
                    sourceNickname = sourceNickname,
                    targetNickname = targetNickname,
                    mode = mode,
                    processChildren = true,
                    dryRun = dryRun,
                    alreadyProcessed = processed,
                    messageSpacePrefix = " $messageSpacePrefix",
                    onComplete
                )

                processed.addAll(processedChildren)
            }



            elem.toFromRelationshipArray().forEach {relationship ->
                process(
                    elem = relationship,
                    sourceNickname = sourceNickname,
                    targetNickname = targetNickname,
                    mode = mode,
                    processChildren = false,
                    dryRun = dryRun,
                    alreadyProcessed = processed,
                    messageSpacePrefix = " $messageSpacePrefix",
                    null
                )
            }
        }

        if (onComplete != null) {
            onComplete(elem)
        }

        return processed;
    }
}
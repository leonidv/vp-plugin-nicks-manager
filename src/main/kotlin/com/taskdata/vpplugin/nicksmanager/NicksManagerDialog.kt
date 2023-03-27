package com.taskdata.vpplugin.nicksmanager

import com.vp.plugin.diagram.IDiagramElement
import com.vp.plugin.model.IModelElement
import net.miginfocom.swing.MigLayout
import java.awt.*
import java.awt.event.ActionListener
import javax.swing.*

class NicksManagerDialog(nicknames: Array<String>, diagramElements: Iterator<IDiagramElement>) : JDialog(), VPHelper {
    companion object {
        fun build(
            rootFrame: Component?,
            nicknames: Array<String>,
            elements: Iterator<IDiagramElement>
        ): NicksManagerDialog {
            val frame = NicksManagerDialog(nicknames, elements);
            frame.title = "Swap nicknames"
            frame.pack();
            frame.setLocationRelativeTo(rootFrame)
            frame.isModal = true;
            frame.isResizable = false;

            return frame;
        }
    }

    /**
     * Contains main logic of processing elements.
     */
    class BackgroundTask(
        private val thisDialog: NicksManagerDialog,
        private val elements: List<IModelElement>
    ) : SwingWorker<Unit, IModelElement>(), VPHelper {

        override fun process(chunks: MutableList<IModelElement>) {
            chunks.forEach {
                if (elements.contains(it)) {
                    thisDialog.progressBar.value++
                }

                if (thisDialog.progressBar.maximum == thisDialog.progressBar.value) {
                      thisDialog.cancelProcessButton.apply {
                         text = "Exit"
                         val listeners = listOf<ActionListener>(*actionListeners)
                         listeners.forEach{l -> removeActionListener(l)}
                         addActionListener{thisDialog.dispose()}
                      }
                }
            }
        }

        override fun doInBackground(): Unit {
            thisDialog.progressBar.maximum = elements.size
            showMessage("Count of processed elements: ${thisDialog.progressBar.maximum}")

            // This is a strange bug, but swap from origin to any other don't work.
            // Next code is ugly hack, but it works!
            val source = if (thisDialog.sourceValue() == NICKNAME_ORIGINAL) {
                thisDialog.targetValue()
            } else {
                thisDialog.sourceValue()
            }

            val target = if (thisDialog.sourceValue() == NICKNAME_ORIGINAL) {
                thisDialog.sourceValue()
            } else {
                thisDialog.targetValue()
            }

            val dryRun = thisDialog.dryRun();

            showMessage("Target: $target, Source: $source, DryRun: $dryRun")

            var processedElements = mutableSetOf<IModelElement>()

            elements.forEach {element ->
                if (this.isCancelled) {
                    return@forEach
                }
                val processed = nicknameSwapper.process(
                    elem = element,
                    sourceNickname = source,
                    targetNickname = target,
                    mode = SwapMode.SWAP,
                    processChildren = true,
                    dryRun = dryRun,
                    alreadyProcessed = processedElements,
                    messageSpacePrefix = "",
                    this::publish
                )
                processedElements.addAll(processed)
            }
        }
    }

    private val sourceCombobox = JComboBox(nicknames)

    private val targetCombobox = JComboBox(nicknames)

    private val dryRunCheckbox = JCheckBox("Dry run (test execution)").apply {
        isSelected = true
    }

    private val doItButton = JButton("Do it!").apply {
        this.addActionListener {
            val dialog = this@NicksManagerDialog
            dialog.executePanel.isVisible = false;
            dialog.progressPanel.isVisible = true;
            dialog.pack();
            dialog.runProcess(diagramElements);
        }

        this.isEnabled = false;
    }

    private val warningLabel = JLabel("Select different nicknames").apply {
        this.maximumSize = Dimension(Int.MAX_VALUE, this.height)
        this.horizontalAlignment = SwingConstants.CENTER
    }

    private val cancelProcessButton = JButton("Stop!")

    private val executePanel = JPanel();

    private val progressBar = JProgressBar().apply {
        this.isStringPainted = true
    }



    private val progressPanel = JPanel()

    private fun sourceValue() = sourceCombobox.selectedItem as String

    private fun targetValue() = targetCombobox.selectedItem as String

    private fun dryRun() = dryRunCheckbox.isSelected

    private val comboboxValueCheck: ActionListener = ActionListener {
        val sameValues = sourceValue() == targetValue()
        doItButton.isEnabled = !sameValues
        warningLabel.isVisible = sameValues
    }

    init {

        this.layout = BorderLayout();

        sourceCombobox.addActionListener(comboboxValueCheck)
        targetCombobox.addActionListener(comboboxValueCheck)

        val propertiesPanel = JPanel(MigLayout("","[150px][150px]"))
        propertiesPanel.add(JLabel("From:"))
        propertiesPanel.add(JLabel("To:"), "wrap")
        propertiesPanel.add(sourceCombobox, "growx")
        propertiesPanel.add(targetCombobox, "growx, wrap");
        propertiesPanel.add(dryRunCheckbox, "span, wrap")

        propertiesPanel.border = BorderFactory.createLineBorder(Color.BLACK)
        propertiesPanel.doLayout();

        this.add(propertiesPanel, BorderLayout.CENTER)

        executePanel.layout = BorderLayout()
        val btCancel = JButton("Cancel").apply {
            this.addActionListener {
                this@NicksManagerDialog.dispose()
            }
        };

        executePanel.apply {
            add(btCancel, BorderLayout.WEST)
            add(warningLabel, BorderLayout.CENTER)
            add(doItButton, BorderLayout.EAST)
        }

        progressBar.maximumSize = Dimension(Int.MAX_VALUE, Int.MAX_VALUE)
        progressPanel.apply {
            layout = MigLayout("fill","[fill][25px]")
            add(progressBar, "growx" )
            add(cancelProcessButton, "growx, growy")
            isVisible = false
        }

        val footerPanel = JPanel(MigLayout("fill, hidemode 2"))
        footerPanel.add(executePanel,"growx, wrap")
        footerPanel.add(progressPanel,"growx, growy")

        this.add(footerPanel, BorderLayout.SOUTH)
    }



    private fun runProcess(diagramElements: Iterator<IDiagramElement>) {
        var elements = mutableListOf<IModelElement>()

        diagramElements.forEach {
            val modelElement = it.modelElement
            elements.add(modelElement)
        }

        val backgroundTask = BackgroundTask(this, elements)

        this.cancelProcessButton.addActionListener {
            backgroundTask.cancel(true)

        }

        backgroundTask.execute();
    }
}


fun main() {
    val frame = NicksManagerDialog.build(null, arrayOf("Origin", "Technical"), emptyList<IDiagramElement>().iterator());
    frame.isVisible = true;

}
package com.joetr.modulemaker.ui.file

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellij.icons.AllIcons
import org.jetbrains.jewel.bridge.icon.fromPlatformIcon
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icon.IntelliJIconKey

@Composable
fun FileTreeView(model: FileTree, height: Dp, onClick: (ExpandableFile) -> Unit, modifier: Modifier) = Box(
    modifier = modifier.height(height)
) {
    with(LocalDensity.current) {
        Box {
            val lazyListState = rememberLazyListState()
            val scrollState = rememberScrollState()

            LazyColumn(
                modifier = Modifier.fillMaxSize().horizontalScroll(scrollState),
                state = lazyListState
            ) {
                items(model.items.size) {
                    FileTreeItemView(
                        14.sp,
                        14.sp.toDp() * 1.5f,
                        model.items[it],
                        onClick = onClick,
                        // if it's the last one and the scrollbar is showing
                        showBottomPadding = it == model.items.size - 1 && (lazyListState.canScrollForward || lazyListState.canScrollBackward),
                        // if the scrollbar is showing
                        showEndPadding = scrollState.canScrollForward || scrollState.canScrollBackward
                    )
                }
            }

            VerticalScrollbar(
                rememberScrollbarAdapter(lazyListState),
                Modifier.align(Alignment.CenterEnd)
            )

            HorizontalScrollbar(
                rememberScrollbarAdapter(scrollState),
                Modifier.align(Alignment.BottomStart)
            )
        }
    }
}

@Composable
private fun FileTreeItemView(
    fontSize: TextUnit,
    height: Dp,
    model: FileTree.Item,
    onClick: (ExpandableFile) -> Unit,
    showBottomPadding: Boolean,
    showEndPadding: Boolean
) =
    Row(
        modifier = Modifier
            .wrapContentHeight()
            .clickable {
                model.open()

                // let UI know
                onClick(model.file)
            }
            // give padding for scroll bar
            .padding(
                start = 24.dp * model.level,
                end = if (showEndPadding) 8.dp else 0.dp,
                bottom = if (showBottomPadding) 8.dp else 0.dp
            )
            .height(height)
            .fillMaxWidth()
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val active by interactionSource.collectIsHoveredAsState()

        FileItemIcon(Modifier.align(Alignment.CenterVertically), model)
        Text(
            text = model.name,
            color = if (active) JewelTheme.contentColor.copy(alpha = 0.60f) else JewelTheme.contentColor,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .clipToBounds()
                .hoverable(interactionSource),
            softWrap = true,
            fontSize = fontSize,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1
        )
    }

@Composable
private fun FileItemIcon(modifier: Modifier, model: FileTree.Item) = Box(modifier.size(24.dp).padding(4.dp)) {
    when (val type = model.type) {
        is FileTree.ItemType.Folder -> when {
            !type.canExpand -> Unit
            type.isExpanded -> Icon(
                key = IntelliJIconKey.fromPlatformIcon(AllIcons.General.ArrowDown),
                contentDescription = null,
                iconClass = AllIcons::class.java
            )

            else -> Icon(
                key = IntelliJIconKey.fromPlatformIcon(AllIcons.General.ArrowRight),
                contentDescription = null,
                iconClass = AllIcons::class.java
            )
        }

        is FileTree.ItemType.File -> when (type.ext) {
            in sourceCodeFileExtensions -> Icon(
                key = IntelliJIconKey.fromPlatformIcon(AllIcons.FileTypes.Java),
                contentDescription = null,
                iconClass = AllIcons::class.java
            )
            "txt" -> Icon(
                key = IntelliJIconKey.fromPlatformIcon(AllIcons.FileTypes.Text),
                contentDescription = null,
                iconClass = AllIcons::class.java
            )
            "md" -> Icon(
                key = IntelliJIconKey.fromPlatformIcon(AllIcons.FileTypes.Text),
                contentDescription = null,
                iconClass = AllIcons::class.java
            )
            "gitignore" -> Icon(
                key = IntelliJIconKey.fromPlatformIcon(AllIcons.Vcs.Ignore_file),
                contentDescription = null,
                iconClass = AllIcons::class.java
            )
            "gradle" -> Icon(
                key = IntelliJIconKey.fromPlatformIcon(AllIcons.FileTypes.Config),
                contentDescription = null,
                iconClass = AllIcons::class.java
            )
            "kts" -> Icon(
                key = IntelliJIconKey.fromPlatformIcon(AllIcons.FileTypes.Java),
                contentDescription = null,
                iconClass = AllIcons::class.java
            )
            "properties" -> Icon(
                key = IntelliJIconKey.fromPlatformIcon(AllIcons.FileTypes.Properties),
                contentDescription = null,
                iconClass = AllIcons::class.java
            )
            "bat" -> Icon(
                key = IntelliJIconKey.fromPlatformIcon(AllIcons.FileTypes.Custom),
                contentDescription = null,
                iconClass = AllIcons::class.java
            )
            else -> Icon(
                key = IntelliJIconKey.fromPlatformIcon(AllIcons.FileTypes.Unknown),
                contentDescription = null,
                iconClass = AllIcons::class.java
            )
        }
    }
}

private val sourceCodeFileExtensions = listOf(
    "java",
    "kt",
    "cpp",
    "c",
    "h",
    "py",
    "js",
    "html",
    "css",
    "php",
    "rb",
    "swift",
    "go",
    "scala",
    "rust",
    "dart",
    "lua",
    "xml",
    "pl",
    "sh",
    "sql"
)

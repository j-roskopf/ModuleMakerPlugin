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
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FileTreeView(model: FileTree, height: Dp, onClick: (ExpandableFile) -> Unit, modifier: Modifier) = Surface(
    modifier = modifier.height(height)
) {
    with(LocalDensity.current) {
        Box() {
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
            color = if (active) LocalContentColor.current.copy(alpha = 0.60f) else LocalContentColor.current,
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
                Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = LocalContentColor.current
            )

            else -> Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = LocalContentColor.current
            )
        }

        is FileTree.ItemType.File -> when (type.ext) {
            in sourceCodeFileExtensions -> Icon(
                Icons.Default.Code,
                contentDescription = null,
                tint = Color(0xFF3E86A0)
            )
            "txt" -> Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF87939A))
            "md" -> Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF87939A))
            "gitignore" -> Icon(
                Icons.Default.BrokenImage,
                contentDescription = null,
                tint = Color(0xFF87939A)
            )
            "gradle" -> Icon(Icons.Default.Build, contentDescription = null, tint = Color(0xFF87939A))
            "kts" -> Icon(Icons.Default.Build, contentDescription = null, tint = Color(0xFF3E86A0))
            "properties" -> Icon(
                Icons.Default.Settings,
                contentDescription = null,
                tint = Color(0xFF62B543)
            )
            "bat" -> Icon(Icons.AutoMirrored.Filled.Launch, contentDescription = null, tint = Color(0xFF87939A))
            else -> Icon(Icons.AutoMirrored.Filled.TextSnippet, contentDescription = null, tint = Color(0xFF87939A))
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

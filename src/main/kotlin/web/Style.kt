package schemati.web

import kotlinx.css.*
import kotlinx.css.Float
import kotlinx.css.properties.*

fun getStyle(): String {
    return CSSBuilder().apply {
        body {
            marginTop = 50.px
            backgroundColor = Color("#222222")
            color = Color("#444444")
            fontFamily = "'Montserrat', sans-serif"
        }
        i {
            color = Color("#212121")
        }
        span {
            color = Color("#e57373")
        }
        hr {
            width = LinearDimension("80%")
            border = "0px"
            borderTop = "1px solid #e0e0e0"
        }
        h2 {
            textAlign = TextAlign.center
        }
        p {
            width = LinearDimension("100%")
            textAlign = TextAlign.center
        }
        a {
            textDecoration = TextDecoration.none
            color = Color("#e57373")
        }
        ".center" {
            margin = "auto"
            width = LinearDimension.fitContent
        }
        "#loginWithDiscord" {
            color = Color.white
            backgroundColor = Color("#7289DA")
            borderRadius = 3.px
            padding = "14px 30px"
            verticalAlign = VerticalAlign.middle
            fontWeight = FontWeight.bold
            boxShadow += BoxShadow(
                inset = false,
                offsetX = 0.px,
                offsetY = 0.px,
                color = Color("#888888"),
                spreadRadius = 0.px,
                blurRadius = 10.px
            )
        }
        "#discordLoginIcon" {
            width = 100.px
            verticalAlign = VerticalAlign.bottom
        }
        "#fileListing" {
            listStyleType = ListStyleType.none
            margin = "0px"
            padding = "0px"
        }
        ".fileEntry" {
            paddingTop = 10.px
            paddingRight = 10.px
            paddingBottom = 10.px
            paddingLeft = 10.px
        }
        ".generalButton" {
            color = Color.initial
            backgroundColor = Color("#e57373")
            border = "none"
            borderRadius = 3.px
            padding = "10px"
            verticalAlign = VerticalAlign.middle
            fontWeight = FontWeight.bold
            fontFamily = "'Montserrat', sans-serif"
            boxShadow += BoxShadow(
                inset = false,
                offsetX = 0.px,
                offsetY = 0.px,
                color = Color("#888888"),
                spreadRadius = 0.px,
                blurRadius = 10.px
            )
        }
        ".textInput" {
            border = "none"
            backgroundColor = Color("#eeeeee")
            color = Color("#444444")
            borderBottom = "2px solid #999999"
            padding = "14px"
            borderTopRightRadius = 6.px
            borderTopLeftRadius = 6.px
        }
        ".leftFloat" {
            display = Display.inlineBlock
            float = Float.left
            marginLeft = 60.px
        }
        ".rightFloat" {
            display = Display.inlineBlock
            float = Float.right
            marginRight = 60.px
        }
        ".fileEntryName" {
            display = Display.inlineBlock
            width = 300.px
        }
        ".fileActions" {
            float = Float.right
        }
        ".actionButton" {
            float = Float.right
            position = Position.relative
            display = Display.inlineBlock
            backgroundColor = Color.inherit
            border = "none"
            outline = Outline.none
            cursor = Cursor.pointer
        }
        ".actionMenu" {
            display = Display.inlineBlock
            float = Float.right
        }
        ".actionMenuOpen" {
            backgroundColor = Color("#cccccc")
            borderRadius = 12.px
        }
        "#content" {
            minHeight = 100.px
        }
        "#main" {
            backgroundColor = Color.white
            width = 400.px
            padding = "20px"
            borderRadius = 3.px
            boxShadow += BoxShadow(
                inset = false,
                offsetX = 0.px,
                offsetY = 0.px,
                color = Color("#888888"),
                spreadRadius = 0.px,
                blurRadius = 10.px
            )
        }
    }.toString()
}

fun getScript(): String {
    return """
        function swapDisplay(clickId) {
            var actionOption = document.getElementById('actionOptions'+clickId);
            var actionIcon = document.getElementById('actionIcon'+clickId);
            var fileName = document.getElementById('fileName'+clickId);
            var parent = fileName.parentNode;
            if (actionOption.style.display === 'none') {
                resetActions()
                actionOption.style.display = 'inline-block';
                parent.classList.add('actionMenuOpen')
                fileName.style.display = 'none';
                actionIcon.classList.remove('fa-caret-left')
                actionIcon.classList.add('fa-caret-right')
            } else {
                actionOption.style.display = 'none';
                parent.classList.remove('actionMenuOpen')
                fileName.style.display = 'inline-block';
                actionIcon.classList.add('fa-caret-left')
                actionIcon.classList.remove('fa-caret-right')
            }
        }
        function resetActions() {
            var actions = document.getElementsByClassName('fileEntryActions');
            var names = document.getElementsByClassName('fileEntryName');
            for(var i = 0; i < actions.length; i++){
                actions[i].style.display = 'none';
                names[i].style.display = 'inline-block';
            }
            var entries = document.getElementsByClassName('actionMenuOpen');
            for(var i = 0; i < entries.length; i++){
                entries[i].classList.remove('actionMenuOpen')
            }
            var buttons = document.getElementsByClassName('fa-caret-right')
            for(var i = 0; i < buttons.length; i++){
                buttons[i].classList.add('fa-caret-left')
                buttons[i].classList.remove('fa-caret-right')
            }
        }
    """
}
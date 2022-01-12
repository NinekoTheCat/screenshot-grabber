// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.tfowl.ktor.client.features.JsoupFeature
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jsoup.nodes.Document
import java.io.InputStream

@OptIn(ExperimentalMaterialApi::class)
fun main() = application {

    Window(onCloseRequest = ::exitApplication, title = "Niko's Screenshot Grabber") {
        val defaultImage = painterResource("placeholder.jpg")
        MaterialTheme {
            val image = remember { mutableStateOf(defaultImage) }
            val url = remember { mutableStateOf("") }
            val buttonVisible = remember { mutableStateOf(true) }
            BackdropScaffold(appBar = {
                randomScreenshotButton(url = url.value, randomImage = {
                    it.launch(Dispatchers.IO) {
                        buttonVisible.value = false
                        val rand = getRandomImage()
                        image.value = rand.image
                        url.value = rand.url
                        buttonVisible.value = true
                    }
                }, buttonVisible = buttonVisible.value)
            }, backLayerContent = {
            }, frontLayerContent = {
                randomImage(image.value)
            })
        }

    }
}

data class RandImage(
    val url: String,
    val image: Painter
)

val alphabet = "abcdefghijklmnopqrstuvwxyz1234567890".toCharArray()
suspend fun getRandomImage(): RandImage {
    val client = HttpClient(CIO) {
        install(JsoupFeature)
    }
    val randomNumberString = buildString {
        repeat(6) {
            append(alphabet.random())
        }
    }
    val urlString = "https://prnt.sc/$randomNumberString"
    val response = client.get<Document>(urlString)
    val imageSrc = response.select("#screenshot-image").attr("src")
    val imageInput = client.get<InputStream>(imageSrc)
    client.close()
    return RandImage(image = BitmapPainter(loadImageBitmap(imageInput)), url = urlString)
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun randomImage(painter: Painter) {
    AnimatedContent(targetState = painter) {
        Image(
            it,
            contentDescription = "an Image of a random Screenshot",
            alignment = Alignment.BottomCenter,
            contentScale = ContentScale.Inside
        )
    }

}

@OptIn(ExperimentalUnitApi::class)
@Composable
fun randomScreenshotButton(randomImage: (CoroutineScope) -> Unit, url: String, buttonVisible: Boolean) {
    val rememberCoroutineScope = rememberCoroutineScope()
    val current = LocalClipboardManager.current
    Row {
        Box {
            Button(
                onClick = {
                    randomImage(rememberCoroutineScope)
                }, enabled = buttonVisible, modifier = Modifier.alpha(
                    if (buttonVisible) 100.0F else 0.0F
                )
            ) {
                Text("Get a new Image")
            }
            CircularProgressIndicator(
                color = MaterialTheme.colors.onPrimary, strokeWidth = Dp(1F), modifier = Modifier.alpha(
                    if (!buttonVisible) 100.0F else 0.0F
                ).align(Alignment.Center)
            )
        }
        ClickableText(
            AnnotatedString(url),
            onClick = {
                current.setText(AnnotatedString(url))
            },
            style = TextStyle(
                fontSize = TextUnit(1.0F, TextUnitType.Em),
                color = MaterialTheme.colors.onPrimary,
                textAlign = TextAlign.Justify
            )
        )
    }

}

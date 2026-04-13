package com.koboshelves

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.koboshelves.ui.MainScreen
import com.koboshelves.ui.theme.KoboShelvesTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pickFile = registerForActivityResult(
            ActivityResultContracts.OpenDocument(),
        ) { uri ->
            uri?.let { viewModel.processDatabase(it, applicationContext) }
        }

        setContent {
            KoboShelvesTheme {
                MainScreen(
                    viewModel = viewModel,
                    onPickFile = { pickFile.launch(arrayOf("*/*")) },
                )
            }
        }
    }
}

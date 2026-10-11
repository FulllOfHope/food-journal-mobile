package com.messledger.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import com.messledger.app.data.ConfigDataStore
import com.messledger.app.data.MessLedgerDatabase
import com.messledger.app.ui.MainScreen
import com.messledger.app.ui.theme.FoodJournalTheme
import com.messledger.app.viewmodel.MessViewModel
import com.messledger.app.viewmodel.MessViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val db = MessLedgerDatabase.getDatabase(applicationContext)
        val configDataStore = ConfigDataStore(applicationContext)
        
        val factory = MessViewModelFactory(db.messLedgerDao(), configDataStore)
        val viewModel = ViewModelProvider(this, factory)[MessViewModel::class.java]

        setContent {
            FoodJournalTheme {
                MainScreen(viewModel)
            }
        }
    }
}

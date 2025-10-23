package com.example.algovisualizer.features.welcome

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.algovisualizer.R

@Composable
fun WelcomeScreen(
    onStartClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Your Logo Image
            Image(
                painter = painterResource(id = R.drawable.logo), // ← Your logo file
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(300.dp)
                    .padding(32.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Start Button
            Button(
                onClick = onStartClick,
                modifier = Modifier
                    .padding(horizontal = 48.dp)
                    .height(56.dp)
                    .width(200.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF69DAAD)
                )
            ) {
                Text(
                    text = "Start",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Quote
            Text(
                text = "\"Unity can only be manifested by the Binary.\nUnity itself and the idea of Unity are already two.\"",
                fontSize = 14.sp,
                fontStyle = FontStyle.Italic,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
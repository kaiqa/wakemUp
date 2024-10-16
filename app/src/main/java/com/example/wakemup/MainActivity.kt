// MainActivity.kt
package com.example.wakemup

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.wakemup.ui.theme.WakemUpTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.withContext
import android.content.Context
import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WakemUpTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    WakeOnLanScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun WakeOnLanScreen(modifier: Modifier = Modifier) {
    var macAddress by remember { mutableStateOf("e0:73:e7:bc:9c:82") }
    var ipAddress by remember { mutableStateOf("192.168.1.35") }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = "Enter MAC Address")
        BasicTextField(
            value = macAddress,
            onValueChange = { macAddress = it },
            modifier = Modifier.fillMaxWidth()
        )

        Text(text = "Enter Broadcast IP Address")
        BasicTextField(
            value = ipAddress,
            onValueChange = { ipAddress = it },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                if (macAddress.isNotEmpty() && ipAddress.isNotEmpty()) {
                    if (isValidMac(macAddress) && isValidIp(ipAddress)) {
                        Toast.makeText(context, "Sending Wake-on-LAN packet...", Toast.LENGTH_SHORT).show()
                        coroutineScope.launch(Dispatchers.IO) {
                            sendWakeOnLanPacket(macAddress, ipAddress, context)
                        }
                    } else {
                        Toast.makeText(context, "Invalid MAC or IP address.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Please enter MAC and IP addresses", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Wake Up PC")
        }
    }
}

suspend fun sendWakeOnLanPacket(macAddress: String, ipAddress: String, context: Context) {
    try {
        val macBytes = getMacBytes(macAddress)
        val bytes = ByteArray(6 + 16 * macBytes.size)
        for (i in 0..5) {
            bytes[i] = 0xFF.toByte()
        }
        for (i in 6 until bytes.size) {
            bytes[i] = macBytes[i % macBytes.size]
        }

        val address = InetAddress.getByName(ipAddress)
        val packet = DatagramPacket(bytes, bytes.size, address, 9)
        val socket = DatagramSocket()
        socket.send(packet)
        socket.close()

        // Inform the user of success on the main thread
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Wake-on-LAN packet sent successfully!", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Log.e("WakeOnLan", "Error sending packet", e)
        // Inform the user of the error on the main thread
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Failed to send Wake-on-LAN packet.", Toast.LENGTH_SHORT).show()
        }
    }
}

fun getMacBytes(macStr: String): ByteArray {
    val bytes = ByteArray(6)
    val hex = macStr.split(":")
    for (i in hex.indices) {
        bytes[i] = Integer.parseInt(hex[i], 16).toByte()
    }
    return bytes
}

fun isValidMac(mac: String): Boolean {
    return mac.matches(Regex("^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$"))
}

fun isValidIp(ip: String): Boolean {
    return ip.matches(Regex("^(\\d{1,3}\\.){3}\\d{1,3}$"))
}

@Preview(showBackground = true)
@Composable
fun WakeOnLanScreenPreview() {
    WakemUpTheme {
        WakeOnLanScreen()
    }
}

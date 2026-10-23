package com.mediagenda.app.sync

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors

class NearbySyncActivity:AppCompatActivity() {
    private val service="com.mediagenda.app.stable.nearby.family.v1"
    private val client by lazy{Nearby.getConnectionsClient(this)}
    private val vault by lazy{NearbySyncStore(this)}
    private val worker=Executors.newSingleThreadExecutor()
    private val pending=mutableMapOf<Long,Payload>()
    private lateinit var status:TextView
    private var peer:String?=null
    private var connecting=false
    private var onGranted:(()->Unit)?=null

    private val permissions=registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()){result->
        if(result.values.all{it} && required().all{
                ContextCompat.checkSelfPermission(this,it)==PackageManager.PERMISSION_GRANTED
            })onGranted?.invoke()
        else update("Permissões recusadas. Abre Definições e autoriza Dispositivos próximos.")
        onGranted=null
    }
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        val panel=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL
            setPadding(24,32,24,32)
        }
        fun label(s:String,size:Float)=TextView(this).apply{
            text=s;textSize=size;setPadding(8,12,8,12)
        }
        panel.addView(label("🔄 Sincronização familiar",25f))
        panel.addView(label("No primeiro telemóvel escolhe PARTILHAR e no segundo PROCURAR. Verifica o mesmo código em ambos e confirma.",15f))
        panel.addView(label("Dados: medicação, tomas, consultas, análises, documentos, embalagens e identificação. A fusão não elimina registos.",14f))
        fun addButton(name:String,fn:()->Unit){
            panel.addView(Button(this).apply{text=name;setOnClickListener{fn()}})
        }
        addButton("📡 PARTILHAR"){request{advertise()}}
        addButton("🔎 PROCURAR"){request{discover()}}
        addButton("✕ DESLIGAR"){disconnect();update("Ligação terminada.")}
        status=label("Pronto para emparelhar.",17f)
        panel.addView(status)
        setContentView(ScrollView(this).apply{addView(panel)})
    }
    private fun update(s:String){runOnUiThread{if(::status.isInitialized)status.text=s}}
    private fun required():Array<String> = when {
        Build.VERSION.SDK_INT>=33 -> arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.NEARBY_WIFI_DEVICES)
        Build.VERSION.SDK_INT>=31 -> arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.ACCESS_FINE_LOCATION)
        else -> arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    private fun request(f:()->Unit){
        val missing=required().filter{
            ContextCompat.checkSelfPermission(this,it)!=PackageManager.PERMISSION_GRANTED
        }
        if(missing.isEmpty())f()else{
            onGranted=f
            permissions.launch(missing.toTypedArray())
        }
    }
    private fun disconnect(){
        client.stopDiscovery()
        client.stopAdvertising()
        client.stopAllEndpoints()
        peer=null;connecting=false
    }
    private val connected=object:ConnectionLifecycleCallback(){
        override fun onConnectionInitiated(id:String,info:ConnectionInfo){
            runOnUiThread{
                MaterialAlertDialogBuilder(this@NearbySyncActivity)
                    .setTitle("Emparelhar com "+info.endpointName)
                    .setMessage("Confirma que o código nos dois telemóveis é igual:\n\n"+
                        info.authenticationDigits)
                    .setCancelable(false)
                    .setPositiveButton("✓ Códigos iguais"){_,_->
                        client.acceptConnection(id,received)
                    }
                    .setNegativeButton("Recusar"){_,_->
                        client.rejectConnection(id);connecting=false
                    }.show()
            }
        }
        override fun onConnectionResult(id:String,res:ConnectionResolution){
            if(res.status.isSuccess){
                peer=id
                client.stopAdvertising();client.stopDiscovery()
                update("Ligação segura. A transferir os dados...")
                worker.execute{
                    try{
                        val data=vault.export()
                        val payload=Payload.fromFile(data)
                        client.sendPayload(id,payload).addOnFailureListener{
                            update("Erro no envio: "+it.localizedMessage)
                        }
                    }catch(e:Exception){
                        update("Erro ao preparar os dados: "+e.localizedMessage)
                    }
                }
            }else{
                connecting=false
                update("Ligação não estabelecida: "+res.status.statusCode)
            }
        }
        override fun onDisconnected(id:String){
            peer=null;connecting=false;update("Ligação encerrada.")
        }
    }
    private val found=object:EndpointDiscoveryCallback(){
        override fun onEndpointFound(id:String,info:DiscoveredEndpointInfo){
            if(connecting||peer!=null)return
            connecting=true
            update("Encontrado: "+info.endpointName)
            client.requestConnection(android.os.Build.MODEL,id,connected)
                .addOnFailureListener{
                    connecting=false;update("Ligação falhou: "+it.localizedMessage)
                }
        }
        override fun onEndpointLost(id:String){}
    }
    private fun advertise(){
        disconnect();update("À espera do outro telemóvel...")
        client.startAdvertising(Build.MODEL,service,connected,
            AdvertisingOptions.Builder().setStrategy(Strategy.P2P_POINT_TO_POINT).build())
            .addOnFailureListener{update("Falha na partilha: "+it.localizedMessage)}
    }
    private fun discover(){
        disconnect();update("A procurar telemóveis próximos...")
        client.startDiscovery(service,found,
            DiscoveryOptions.Builder().setStrategy(Strategy.P2P_POINT_TO_POINT).build())
            .addOnFailureListener{update("Falha na pesquisa: "+it.localizedMessage)}
    }
    private val received=object:PayloadCallback(){
        override fun onPayloadReceived(id:String,p:Payload){
            if(id==peer && p.type==Payload.Type.FILE) synchronized(pending){
                pending[p.id]=p
            }
        }
        override fun onPayloadTransferUpdate(id:String,u:PayloadTransferUpdate){
            if(id!=peer)return
            if(u.status==PayloadTransferUpdate.Status.SUCCESS){
                val p=synchronized(pending){pending.remove(u.payloadId)}?:return
                worker.execute{
                    val local=File(cacheDir,UUID.randomUUID().toString()+".zip")
                    try{
                        val uri=p.asFile()?.asUri()?:error("Ficheiro inválido")
                        contentResolver.openInputStream(uri)?.use{input->
                            local.outputStream().use{out->input.copyTo(out)}
                        }?:error("Ficheiro indisponível")
                        val result=vault.merge(local)
                        update("✓ Novos registos: "+result.newRecords+
                            "; ficheiros: "+result.newFiles+
                            "; conflitos protegidos: "+result.conflicts)
                    }catch(e:Exception){
                        update("Erro na sincronização: "+e.localizedMessage)
                    }finally{local.delete()}
                }
            }else if(u.status==PayloadTransferUpdate.Status.FAILURE){
                update("Transferência interrompida. Dados locais preservados.")
            }else if(u.status==PayloadTransferUpdate.Status.IN_PROGRESS &&
                u.totalBytes>0){
                update("Transferência: "+(u.bytesTransferred*100/u.totalBytes)+"%")
            }
        }
    }
    override fun onDestroy(){
        disconnect()
        worker.shutdown()
        super.onDestroy()
    }
}

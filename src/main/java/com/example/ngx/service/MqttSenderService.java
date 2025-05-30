package com.example.ngx.service;

import com.example.ngx.model.DeviceInfo;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
    // 修改第9行导入语句为：
import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;

@Service
public class MqttSenderService {
    
    private final Random random = new Random();
    private final Map<String, MqttClient> clients = new ConcurrentHashMap<>();
    private final Map<String, ScheduledExecutorService> schedulers = new ConcurrentHashMap<>();

    @Async
    public void startDevice(DeviceInfo device) {
        String clientId = device.group() + "&" + device.deviceId();
        String topic = "/" + device.group() + "/" + device.deviceId() + "/post";
        
        try {
            MqttClient client = new MqttClient("tcp://mqtt-server:1883", clientId);
            client.connect();
            clients.put(clientId, client);

            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            schedulers.put(clientId, scheduler);

            scheduler.scheduleAtFixedRate(() -> {
                try {
                    String payload = String.format("{\"%s\": %d}", device.deviceId(), random.nextInt(100));
                    MqttMessage message = new MqttMessage(payload.getBytes());
                    message.setQos(0);
                    client.publish(topic, message);
                } catch (MqttException e) {
                    // 添加重试逻辑或错误通知
                    System.err.println("消息发送失败: " + e.getMessage());
                }
            }, 0, 2, TimeUnit.HOURS);
            
        } catch (MqttException e) {
            System.err.println("MQTT连接失败: " + e.getMessage());
            cleanupResource(clientId);
        }
    }

    @PreDestroy
    public void cleanup() {
        schedulers.forEach((id, scheduler) -> {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(3, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        clients.forEach((id, client) -> {
            try {
                if (client.isConnected()) {
                    client.disconnect();
                }
                client.close();
            } catch (MqttException e) {
                System.err.println("资源释放异常: " + e.getMessage());
            }
        });
    }

    private void cleanupResource(String clientId) {
        MqttClient client = clients.remove(clientId);
        if (client != null) {
            try {
                client.close();
            } catch (MqttException ignored) {}
        }
        
        ScheduledExecutorService scheduler = schedulers.remove(clientId);
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }
}
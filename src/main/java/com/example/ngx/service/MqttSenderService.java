package com.example.ngx.service;

import com.example.ngx.model.DeviceInfo;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
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
        System.out.println("开始发送设备[" + device.deviceId() + "]的消息");
        System.out.println("真实值[" + device.longitude() + "]");

        try {
            MqttClient client = new MqttClient("tcp://10.8.2.69:1883", clientId);
            // 打印clientId
            System.out.println("MQTT客户端ID: " + client.getClientId());
            // 新增设备三元组认证
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(device.deviceId()); // 从设备信息获取用户名
            options.setPassword(device.password().toCharArray()); // 从设备信息获取密码
            client.connect(options); // 使用带认证参数的连接方法

            clients.put(clientId, client);
            System.out.println("设备[" + device.deviceId() + "]已连接到MQTT服务器");

            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            schedulers.put(clientId, scheduler);

            // scheduler.scheduleAtFixedRate(() -> {
            // try {
            // // 修改随机数生成：将第一个nextInt(70)强制转换为float类型，并调整格式化字符串为%f
            // String payload = String.format("{\"Distance\":%.2f,\"amp\":%d}", (float)
            // random.nextInt(70), random.nextInt(50));
            // System.out.println("发送设备[" + device.deviceId() + "]的消息: " + payload);
            // MqttMessage message = new MqttMessage(payload.getBytes());
            // message.setQos(0);
            // client.publish(topic, message);

            scheduler.scheduleAtFixedRate(() -> {
                try {
                    // 产生一个范围在大于10小于79之间的随机2位小数
                    // double dis = (random.nextInt(69) + 10);
                    for (int i = 0; i < 3; i++) { // 循环优化重复逻辑
                        String payload = String.format(
                                i == 0 ? "{\"Distance\":%.2f,\"amp\":%d}"
                                        : i == 1 ? "{\"Distance1\":%.2f,\"amp\":%d}"
                                                : "{\"Distance2\":%.2f,\"amp\":%d}",
                                device.longitude(),
                                random.nextInt(50));
                        System.out.println("发送设备[" + device.deviceId() + "]的消息: " + payload);

                        MqttMessage message = new MqttMessage(payload.getBytes());
                        message.setQos(0);
                        client.publish(topic, message);

                        try {
                            Thread.sleep(19000); // 统一处理延时
                        } catch (InterruptedException e) {
                            System.err.println("设备[" + device.deviceId() + "]任务被中断");
                            Thread.currentThread().interrupt(); // 恢复中断状态
                            return; // 直接退出任务
                        }
                    }
                } catch (MqttException e) {
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
            } catch (MqttException ignored) {
            }
        }

        ScheduledExecutorService scheduler = schedulers.remove(clientId);
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }
}
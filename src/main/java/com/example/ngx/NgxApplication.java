package com.example.ngx;

import com.example.ngx.model.DeviceInfo;
import com.example.ngx.service.MqttSenderService;
import com.example.ngx.util.ExcelReader;
import java.util.List;  // 新增导入语句
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class NgxApplication implements CommandLineRunner {

	private final MqttSenderService senderService;
	
	public NgxApplication(MqttSenderService senderService) {
		this.senderService = senderService;
	}

	public static void main(String[] args) {
		SpringApplication.run(NgxApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		List<DeviceInfo> devices = ExcelReader.readDevices("device_info.xlsx");
		devices.forEach(senderService::startDevice);
	}
}
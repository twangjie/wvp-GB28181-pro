package com.genersoft.iot.vmp.vmanager.gb28181.ptz;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import javax.sip.message.Response;

import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;

@Api(tags = "云台控制")
@CrossOrigin
@RestController
@RequestMapping("/api/ptz")
public class PtzController {

	private final static Logger logger = LoggerFactory.getLogger(PtzController.class);

	@Autowired
	private SIPCommander cmder;

	@Autowired
	private IVideoManagerStorager storager;

	@Autowired
	private DeferredResultHolder resultHolder;

	/***
	 * 云台控制
	 * @param deviceId 设备id
	 * @param channelId 通道id
	 * @param cmdCode		指令码
	 * @param horizonSpeed	水平移动速度
	 * @param verticalSpeed	垂直移动速度
	 * @param zoomSpeed	    缩放速度
	 * @return String 控制结果
	 */
	@ApiOperation("云台控制")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "deviceId", value = "设备ID", dataTypeClass = String.class),
			@ApiImplicitParam(name = "channelId", value = "通道ID", dataTypeClass = String.class),
			@ApiImplicitParam(name = "cmdCode", value = "指令码", dataTypeClass = Integer.class),
			@ApiImplicitParam(name = "horizonSpeed", value = "水平速度", dataTypeClass = Integer.class),
			@ApiImplicitParam(name = "verticalSpeed", value = "垂直速度", dataTypeClass = Integer.class),
			@ApiImplicitParam(name = "zoomSpeed", value = "缩放速度", dataTypeClass = Integer.class),
	})
	@PostMapping("/control/{deviceId}/{channelId}")
	public ResponseEntity<String> ptz(@PathVariable String deviceId,@PathVariable String channelId,int cmdCode, int horizonSpeed, int verticalSpeed, int zoomSpeed){

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("设备云台控制 API调用，deviceId：%s ，channelId：%s ，cmdCode：%d ，horizonSpeed：%d ，verticalSpeed：%d ，zoomSpeed：%d",deviceId, channelId, cmdCode, horizonSpeed, verticalSpeed, zoomSpeed));
		}
		Device device = storager.queryVideoDevice(deviceId);

		cmder.frontEndCmd(device, channelId, cmdCode, horizonSpeed, verticalSpeed, zoomSpeed);
		return new ResponseEntity<String>("success",HttpStatus.OK);
	}

	@ApiOperation("通用前端控制命令")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "deviceId", value = "设备ID", dataTypeClass = String.class),
			@ApiImplicitParam(name = "channelId", value = "通道ID", dataTypeClass = String.class),
			@ApiImplicitParam(name = "cmdCode", value = "指令码", dataTypeClass = Integer.class),
			@ApiImplicitParam(name = "parameter1", value = "数据一", dataTypeClass = Integer.class),
			@ApiImplicitParam(name = "parameter2", value = "数据二", dataTypeClass = Integer.class),
			@ApiImplicitParam(name = "combindCode2", value = "组合码二", dataTypeClass = Integer.class),
	})
	@PostMapping("/front_end_command/{deviceId}/{channelId}")
	public ResponseEntity<String> frontEndCommand(@PathVariable String deviceId,@PathVariable String channelId,int cmdCode, int parameter1, int parameter2, int combindCode2){

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("设备云台控制 API调用，deviceId：%s ，channelId：%s ，cmdCode：%d parameter1：%d parameter2：%d",deviceId, channelId, cmdCode, parameter1, parameter2));
		}
		Device device = storager.queryVideoDevice(deviceId);

		cmder.frontEndCmd(device, channelId, cmdCode, parameter1, parameter2, combindCode2);
		return new ResponseEntity<String>("success",HttpStatus.OK);
	}

	@ApiOperation("预置位查询")
	@ApiImplicitParams({
            @ApiImplicitParam(name = "deviceId", value = "设备ID", dataTypeClass = String.class),
            @ApiImplicitParam(name = "channelId", value = "通道ID", dataTypeClass = String.class),
	})
	@GetMapping("/preset/query/{deviceId}/{channelId}")
	public DeferredResult<ResponseEntity<String>> presetQueryApi(@PathVariable String deviceId, @PathVariable String channelId) {
		if (logger.isDebugEnabled()) {
			logger.debug("设备预置位查询API调用");
		}
		Device device = storager.queryVideoDevice(deviceId);
		cmder.presetQuery(device, channelId, event -> {
			Response response = event.getResponse();
			RequestMessage msg = new RequestMessage();
			msg.setId(DeferredResultHolder.CALLBACK_CMD_PRESETQUERY + (StringUtils.isEmpty(channelId) ? deviceId : channelId));
			msg.setData(String.format("获取设备预置位失败，错误码： %s, %s", response.getStatusCode(), response.getReasonPhrase()));
			resultHolder.invokeResult(msg);
		});
        DeferredResult<ResponseEntity<String>> result = new DeferredResult<ResponseEntity<String >> (3 * 1000L);
		result.onTimeout(()->{
			logger.warn(String.format("获取设备预置位超时"));
			// 释放rtpserver
			RequestMessage msg = new RequestMessage();
			msg.setId(DeferredResultHolder.CALLBACK_CMD_PRESETQUERY + (StringUtils.isEmpty(channelId) ? deviceId : channelId));
			msg.setData("获取设备预置位超时");
			resultHolder.invokeResult(msg);
		});
		resultHolder.put(DeferredResultHolder.CALLBACK_CMD_PRESETQUERY + (StringUtils.isEmpty(channelId) ? deviceId : channelId), result);
		return result;
	}
}

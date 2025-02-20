package com.genersoft.iot.vmp.vmanager.user;

import com.genersoft.iot.vmp.conf.security.SecurityUtils;
import com.genersoft.iot.vmp.conf.security.dto.LoginUser;
import com.genersoft.iot.vmp.service.IUserService;
import com.genersoft.iot.vmp.storager.dao.dto.User;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import javax.security.sasl.AuthenticationException;
import javax.xml.crypto.Data;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Api(tags = "用户管理")
@CrossOrigin
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private IUserService userService;

    private final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @ApiOperation("登录")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "username", required = true, value = "用户名", dataTypeClass = String.class),
            @ApiImplicitParam(name = "password", required = true, value = "密码（32位md5加密）", dataTypeClass = String.class),
    })
    @GetMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password){
        LoginUser user;
        try {
            user = SecurityUtils.login(username, password, authenticationManager);
        } catch (AuthenticationException e) {
            e.printStackTrace();
            return "fail";
        }
        if (user != null) {
            return "success";
        }else {
            return "fail";
        }
    }

    @ApiOperation("修改密码")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "username", required = true, value = "用户名", dataTypeClass = String.class),
            @ApiImplicitParam(name = "oldpassword", required = true, value = "旧密码（已md5加密的密码）", dataTypeClass = String.class),
            @ApiImplicitParam(name = "password", required = true, value = "新密码（未md5加密的密码）", dataTypeClass = String.class),
    })
    @PostMapping("/changePassword")
    public String changePassword(@RequestParam String oldPassword, @RequestParam String password){
        // 获取当前登录用户id
        String username = SecurityUtils.getUserInfo().getUsername();
        LoginUser user = null;
        try {
            user = SecurityUtils.login(username, oldPassword, authenticationManager);
            if (user != null) {
                int userId = SecurityUtils.getUserId();
                boolean result = userService.changePassword(userId, DigestUtils.md5DigestAsHex(password.getBytes()));
                if (result) {
                    return "success";
                }
            }
        } catch (AuthenticationException e) {
            e.printStackTrace();
        }
        return "fail";
    }


    @ApiOperation("添加用户")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "username", required = true, value = "用户名", dataTypeClass = String.class),
            @ApiImplicitParam(name = "password", required = true, value = "密码（未md5加密的密码）", dataTypeClass = String.class),
            @ApiImplicitParam(name = "roleId", required = true, value = "角色ID", dataTypeClass = String.class),
    })
    @PostMapping("/add")
    public ResponseEntity<WVPResult<Integer>> add(@RequestParam String username,
                                                 @RequestParam String password,
                                                 @RequestParam int roleId){
        // 获取当前登录用户id
        int currenRoleId = SecurityUtils.getUserInfo().getRoleId();
        if (currenRoleId != 0) {
            // 只用角色id为0才可以删除和添加用户
            return new ResponseEntity<>(null, HttpStatus.FORBIDDEN);
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(DigestUtils.md5DigestAsHex(password.getBytes()));
        user.setRoleId(roleId);
        user.setCreateTime(format.format(System.currentTimeMillis()));
        user.setUpdateTime(format.format(System.currentTimeMillis()));
        int addResult = userService.addUser(user);
        WVPResult<Integer> result = new WVPResult<>();
        result.setCode(addResult > 0 ? 0 : -1);
        result.setMsg(addResult > 0 ? "success" : "fail");
        result.setData(addResult);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @ApiOperation("删除用户")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", required = true, value = "用户Id", dataTypeClass = Integer.class),
    })
    @DeleteMapping("/delete")
    public ResponseEntity<WVPResult<String>> delete(@RequestParam Integer id){
        // 获取当前登录用户id
        int currenRoleId = SecurityUtils.getUserInfo().getRoleId();
        if (currenRoleId != 0) {
            // 只用角色id为0才可以删除和添加用户
            return new ResponseEntity<>(null, HttpStatus.FORBIDDEN);
        }
        int deleteResult = userService.deleteUser(id);
        WVPResult<String> result = new WVPResult<>();
        result.setCode(deleteResult>0? 0 : -1);
        result.setMsg(deleteResult>0? "success" : "fail");
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @ApiOperation("查询用户")
    @ApiImplicitParams({})
    @GetMapping("/all")
    public ResponseEntity<WVPResult<List<User>>> all(){
        // 获取当前登录用户id
        List<User> allUsers = userService.getAllUsers();
        WVPResult<List<User>> result = new WVPResult<>();
        result.setCode(0);
        result.setMsg("success");
        result.setData(allUsers);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}

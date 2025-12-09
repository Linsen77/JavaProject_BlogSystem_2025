package com.blog.controller;

import com.blog.entity.User;
import com.blog.service.UserService;
import com.blog.util.EmailUtils;
import com.blog.util.PasswordUtils;
import com.blog.util.Result;
import com.blog.util.VerifyCodeUtils;
import jakarta.servlet.http.HttpSession;
import lombok.Data;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import org.springframework.web.bind.support.SessionStatus;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private EmailUtils emailUtils;

    //======================= 发送验证码 ========================
    @GetMapping("/sendCode/{email}")
    public Result<String> sendRegisterCode(@PathVariable String email, HttpSession session){
        try{
            if(!email.matches("^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$")){
                return Result.fail("邮箱格式错误Σ(⊙▽⊙)");
            }

            // 判断邮箱是否已注册
            if(userService.existsByEmail(email)){
                return Result.fail("该邮箱已注册 ovo");
            }

            // 生成验证码并发送
            String code = VerifyCodeUtils.generate6DigitalCode();
            emailUtils.sendRegisterCode(email, code);

            session.setAttribute("code_" + email, code);
            session.setMaxInactiveInterval(300);

            return Result.success("验证码已发送，五分钟内有效！");
        }
        catch (Exception e){
            e.printStackTrace();
            return Result.fail("验证码发送失败：" + e.getMessage());
        }
    }

    //======================= 用户注册 ========================
    @PostMapping("/register")
    public Result<String> register(@RequestBody RegisterDTO dto, HttpSession session){
        try{
            String email = dto.getEmail();
            String code = dto.getCode();

            // 从 session 获取验证码
            String sessionCode = (String) session.getAttribute("code_" + email);
            if(sessionCode == null){
                return Result.fail("验证码失效，请重新获取！");
            }
            if(!sessionCode.equals(code)){
                return Result.fail("验证码错误！");
            }

            User user = new User();
            user.setEmail(email);
            user.setPassword(PasswordUtils.encryptPassword(dto.getPassword()));

            if(!userService.save(user)){
                return Result.fail("注册失败，请重试");
            }

            session.removeAttribute("code_" + email);

            return Result.success("注册成功！");
        }
        catch (Exception e){
            e.printStackTrace();
            return Result.fail("注册失败：" + e.getMessage());
        }
    }


    //======================= 用户登录 ========================
    @PostMapping("/login")
    public Result<User> login(@RequestBody LoginDTO dto, HttpSession session, SessionStatus sessionStatus){

        User user = userService.findByEmail(dto.getEmail());

        if(user == null){
            return Result.fail("(❀｣╹□╹)｣*･请先注册");
        }

        if(!PasswordUtils.checkPassword(dto.getPassword(), user.getPassword())){
            return Result.fail("密码错误(／_＼)");
        }

        user.setPassword(null);
        session.setAttribute("loginUser", user);

        return Result.success(user);
    }
}


//======= DTO 类 =======
@Data
class RegisterDTO {
    private String email;
    private String password;
    private String code;
}

@Data
class LoginDTO {
    private String email;
    private String password;
}

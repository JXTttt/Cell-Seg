package com.urine.cell_seg_sys.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 系统用户实体类
 * 对应表: sys_user
 */
@Data // Lombok 注解：自动生成 Getter/Setter/ToString
public class User {
    private Long userId;        // 主键
    private String username;    // 账号
    private String password;    // 密码
    private String nickname;    // 昵称
    private String avatar;      // 头像URL
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间
}
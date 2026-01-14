package com.urine.cell_seg_sys;

import com.urine.cell_seg_sys.entity.User;
import com.urine.cell_seg_sys.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

@SpringBootTest
class CellSegSysApplicationTests {

    @Autowired
    private UserMapper userMapper; // 注入我们刚才写的接口

    @Test
    void testInsertUser() {
        System.out.println("=========== 开始测试数据库写入 ===========");

        // 1. 创建一个假用户
        User u = new User();
        u.setUsername("test_student_" + System.currentTimeMillis()); // 随机用户名防止重复
        u.setPassword("123456");
        u.setNickname("测试同学");

        // 2. 写入数据库
        userMapper.insert(u);

        // 3. 验证是否拿到了自增 ID
        System.out.println("插入成功！新用户的 ID 是: " + u.getUserId());
        System.out.println("用户名是: " + u.getUsername());
        System.out.println("=========== 测试结束 ===========");
    }

    @Test
    void testFindUser() {
        // 这里的用户名改为你数据库里确实存在的（比如刚才插入的，或者 admin）
        String targetName = "admin";
        User user = userMapper.findByUsername(targetName);
        if(user != null) {
            System.out.println("查询成功：找到用户 " + user.getNickname());
        } else {
            System.out.println("查询结果为空，请确认数据库里有没有这个用户。");
        }
    }
}
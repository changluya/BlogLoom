package com.changlu.blogloom.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;
import com.changlu.blogloom.entity.User;

/**
 * @Description: 用户持久层接口
 * @Author: changlu
 * @Date: 2026-09-13
 */
@Mapper
@Repository
public interface UserMapper {
	User findByUsername(String username);

	User findById(Long id);

	int updateUserByUsername(String username, User user);
}

package com.changlu.blogloom.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import com.changlu.blogloom.entity.User;
import com.changlu.blogloom.exception.NotFoundException;
import com.changlu.blogloom.mapper.UserMapper;
import com.changlu.blogloom.service.UserService;
import com.changlu.blogloom.util.HashUtils;
import com.changlu.blogloom.util.JwtUtils;

/**
 * @Description: 用户业务层接口实现类
 * @Author: changlu
 * @Date: 2026-09-13
 */
@Service
public class UserServiceImpl implements UserService, UserDetailsService {
	@Autowired
	private UserMapper userMapper;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user = userMapper.findByUsername(username);
		if (user == null) {
			throw new UsernameNotFoundException("用户不存在");
		}
		return user;
	}

	@Override
	public User findUserByUsernameAndPassword(String username, String password) {
		User user = userMapper.findByUsername(username);
		if (user == null) {
			throw new UsernameNotFoundException("用户不存在");
		}
		if (!HashUtils.matchBC(password, user.getPassword())) {
			throw new UsernameNotFoundException("密码错误");
		}
		return user;
	}

	@Override
	public User findUserById(Long id) {
		User user = userMapper.findById(id);
		if (user == null) {
			throw new NotFoundException("用户不存在");
		}
		return user;
	}

	@Override
	public boolean changeAccount(User user, String jwt) {
		String username = JwtUtils.getTokenBody(jwt).getSubject();
		user.setPassword(HashUtils.getBC(user.getPassword()));
		if (userMapper.updateUserByUsername(username, user) != 1) {
			TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
			return false;
		}
		return true;
	}
}

package com.changlu.blogloom.controller;

import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.changlu.blogloom.annotation.AccessLimit;
import com.changlu.blogloom.annotation.VisitLogger;
import com.changlu.blogloom.constant.JwtConstants;
import com.changlu.blogloom.entity.Moment;
import com.changlu.blogloom.entity.User;
import com.changlu.blogloom.enums.VisitBehavior;
import com.changlu.blogloom.model.vo.PageResult;
import com.changlu.blogloom.model.vo.Result;
import com.changlu.blogloom.service.MomentService;
import com.changlu.blogloom.service.impl.UserServiceImpl;
import com.changlu.blogloom.util.JwtUtils;

/**
 * @Description: 动态
 * @Author: changlu
 * @Date: 2026-09-13
 */
@RestController
public class MomentController {
	@Autowired
	MomentService momentService;
	@Autowired
	UserServiceImpl userService;

	/**
	 * 分页查询动态List
	 *
	 * @param pageNum 页码
	 * @param jwt     博主访问Token
	 * @return
	 */
	@VisitLogger(VisitBehavior.MOMENT)
	@GetMapping("/moments")
	public Result moments(@RequestParam(defaultValue = "1") Integer pageNum,
	                      @RequestHeader(value = "Authorization", defaultValue = "") String jwt) {
		boolean adminIdentity = false;
		if (JwtUtils.judgeTokenIsExist(jwt)) {
			try {
				String subject = JwtUtils.getTokenBody(jwt).getSubject();
				if (subject.startsWith(JwtConstants.ADMIN_PREFIX)) {
					//博主身份Token
					String username = subject.replace(JwtConstants.ADMIN_PREFIX, "");
					User admin = (User) userService.loadUserByUsername(username);
					if (admin != null) {
						adminIdentity = true;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		PageInfo<Moment> pageInfo = new PageInfo<>(momentService.getMomentVOList(pageNum, adminIdentity));
		PageResult<Moment> pageResult = new PageResult<>(pageInfo.getPages(), pageInfo.getList());
		return Result.ok("获取成功", pageResult);
	}

	/**
	 * 给动态点赞
	 * 简单限制一下点赞
	 *
	 * @param id 动态id
	 * @return
	 */
	@AccessLimit(seconds = 86400, maxCount = 1, msg = "不可以重复点赞哦")
	@VisitLogger(VisitBehavior.LIKE_MOMENT)
	@PostMapping("/moment/like/{id}")
	public Result like(@PathVariable Long id) {
		momentService.addLikeByMomentId(id);
		return Result.ok("点赞成功");
	}
}

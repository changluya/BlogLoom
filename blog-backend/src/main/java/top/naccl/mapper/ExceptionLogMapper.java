package top.naccl.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;
import top.naccl.entity.ExceptionLog;

import java.util.List;

/**
 * @Description: 异常日志持久层接口
 * @Author: changlu
 * @Date: 2026-09-13
 */
@Mapper
@Repository
public interface ExceptionLogMapper {
	List<ExceptionLog> getExceptionLogListByDate(String startDate, String endDate);

	int saveExceptionLog(ExceptionLog log);

	int deleteExceptionLogById(Long id);
}

package top.naccl.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;
import top.naccl.entity.ScheduleJobLog;

import java.util.List;

/**
 * @Description: 定时任务日志持久层接口
 * @Author: changlu
 * @Date: 2026-09-13
 */
@Mapper
@Repository
public interface ScheduleJobLogMapper {
	List<ScheduleJobLog> getJobLogListByDate(String startDate, String endDate);

	int saveJobLog(ScheduleJobLog jobLog);

	int deleteJobLogByLogId(Long logId);
}

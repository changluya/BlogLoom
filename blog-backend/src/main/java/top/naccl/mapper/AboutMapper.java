package top.naccl.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;
import top.naccl.entity.About;

import java.util.List;

/**
 * @Description: 关于我持久层接口
 * @Author: changlu
 * @Date: 2026-09-13
 */
@Mapper
@Repository
public interface AboutMapper {
	List<About> getList();

	int updateAbout(String nameEn, String value);

	String getAboutCommentEnabled();
}

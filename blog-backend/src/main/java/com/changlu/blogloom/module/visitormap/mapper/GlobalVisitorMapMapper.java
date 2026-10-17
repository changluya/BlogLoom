package com.changlu.blogloom.module.visitormap.mapper;

import com.changlu.blogloom.module.visitormap.domain.GlobalVisitorMapVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Mapper
@Repository
public interface GlobalVisitorMapMapper {
	GlobalVisitorMapVO.Summary getSummary(@Param("start") Date start, @Param("end") Date end);

	List<GlobalVisitorMapVO.Country> getCountries(@Param("start") Date start, @Param("end") Date end);

	List<GlobalVisitorMapVO.City> getCities(@Param("start") Date start, @Param("end") Date end);

	long countResolvedCityPv(@Param("start") Date start, @Param("end") Date end);

	long countCountryOnlyPv(@Param("start") Date start, @Param("end") Date end);

	long countUnknownPv(@Param("start") Date start, @Param("end") Date end);
}

package com.changlu.blogloom.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface VisitorGeoMapper {
	Long findCityGeoId(@Param("cityName") String cityName, @Param("countryCode") String countryCode);

	Long findCountryGeoId(@Param("countryCode") String countryCode);
}

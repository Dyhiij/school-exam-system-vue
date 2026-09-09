package com.school.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.exam.entity.UserInfo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserInfoMapper extends BaseMapper<UserInfo> {
}

package com.example.ridersafety.service;

import com.example.ridersafety.config.ApiException;
import com.example.ridersafety.model.Station;
import com.example.ridersafety.model.User;
import com.example.ridersafety.repository.StationRepository;
import com.example.ridersafety.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUser {
    private final UserRepository userRepository;
    private final StationRepository stationRepository;

    public User require(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw ApiException.forbidden("未登录");
        }
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> ApiException.forbidden("用户不存在"));
    }

    /** 解析站点：优先用户所属站点；否则用传入的 stationId（管理员场景） */
    public Station resolveStation(User user, Long stationId) {
        if (user.getStation() != null) {
            return user.getStation();
        }
        if (stationId != null) {
            return stationRepository.findById(stationId)
                    .orElseThrow(() -> ApiException.notFound("站点不存在"));
        }
        throw ApiException.badRequest("请指定站点 (stationId)");
    }
}

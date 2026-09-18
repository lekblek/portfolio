package com.scalke.portfolio.backend.profile.web.dto;

import java.util.List;

public record SkillGroupResponse(String category, List<SkillResponse> skills) {
}

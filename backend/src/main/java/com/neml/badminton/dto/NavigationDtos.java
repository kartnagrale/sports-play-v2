package com.neml.badminton.dto;

import com.neml.badminton.entity.*;
import java.util.*;

public class NavigationDtos {
    public record ScreenDto(String code, String label, String path, String icon,
                            String section, Integer displayOrder) {
        public static ScreenDto from(AppScreen screen) {
            return new ScreenDto(screen.getCode(), screen.getLabel(), screen.getPath(), screen.getIcon(),
                    screen.getSection(), screen.getDisplayOrder());
        }
    }

    public record NavigationResponse(NavigationRole role, List<ScreenDto> screens) {}
}

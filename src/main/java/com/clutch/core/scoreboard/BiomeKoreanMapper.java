package com.clutch.core.scoreboard;

import org.bukkit.block.Biome;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public final class BiomeKoreanMapper {
    private static final Map<Biome, String> BIOME_MAP = new EnumMap<>(Biome.class);

    static {
        BIOME_MAP.put(Biome.PLAINS, "평원");
        BIOME_MAP.put(Biome.SUNFLOWER_PLAINS, "해바라기 평원");
        BIOME_MAP.put(Biome.DESERT, "사막");
        BIOME_MAP.put(Biome.FOREST, "숲");
        BIOME_MAP.put(Biome.FLOWER_FOREST, "꽃 숲");
        BIOME_MAP.put(Biome.BIRCH_FOREST, "자작나무 숲");
        BIOME_MAP.put(Biome.DARK_FOREST, "어두운 숲");
        BIOME_MAP.put(Biome.OLD_GROWTH_PINE_TAIGA, "거대 소나무 타이가");
        BIOME_MAP.put(Biome.TAIGA, "타이가");
        BIOME_MAP.put(Biome.SNOWY_TAIGA, "눈 덮인 타이가");
        BIOME_MAP.put(Biome.JUNGLE, "정글");
        BIOME_MAP.put(Biome.BAMBOO_JUNGLE, "대나무 정글");
        BIOME_MAP.put(Biome.SAVANNA, "사바나");
        BIOME_MAP.put(Biome.SNOWY_PLAINS, "눈 평원");
        BIOME_MAP.put(Biome.OCEAN, "바다");
        BIOME_MAP.put(Biome.DEEP_OCEAN, "깊은 바다");
        BIOME_MAP.put(Biome.RIVER, "강");
        BIOME_MAP.put(Biome.BEACH, "해변");
        BIOME_MAP.put(Biome.STONY_SHORE, "자갈 해안");
        BIOME_MAP.put(Biome.SWAMP, "늪");
        BIOME_MAP.put(Biome.MANGROVE_SWAMP, "맹그로브 늪");
        BIOME_MAP.put(Biome.MUSHROOM_FIELDS, "버섯 지대");
        BIOME_MAP.put(Biome.BADLANDS, "악지");
        BIOME_MAP.put(Biome.WOODED_BADLANDS, "숲 악지");
        BIOME_MAP.put(Biome.ERODED_BADLANDS, "침식된 악지");
        BIOME_MAP.put(Biome.MEADOW, "초원");
        BIOME_MAP.put(Biome.CHERRY_GROVE, "벚꽃 숲");
        BIOME_MAP.put(Biome.GROVE, "침엽수 숲");
        BIOME_MAP.put(Biome.WINDSWEPT_HILLS, "바람부는 언덕");
        BIOME_MAP.put(Biome.FROZEN_PEAKS, "얼어붙은 봉우리");
        BIOME_MAP.put(Biome.JAGGED_PEAKS, "뾰족한 봉우리");
        BIOME_MAP.put(Biome.STONY_PEAKS, "돌 봉우리");
        BIOME_MAP.put(Biome.LUSH_CAVES, "무성한 동굴");
        BIOME_MAP.put(Biome.DRIPSTONE_CAVES, "첨적석 동굴");
        BIOME_MAP.put(Biome.NETHER_WASTES, "네더 황무지");
        BIOME_MAP.put(Biome.SOUL_SAND_VALLEY, "영혼 모래 계곡");
        BIOME_MAP.put(Biome.CRIMSON_FOREST, "진홍빛 숲");
        BIOME_MAP.put(Biome.WARPED_FOREST, "뒤틀린 숲");
        BIOME_MAP.put(Biome.BASALT_DELTAS, "현무암 삼각주");
        BIOME_MAP.put(Biome.THE_END, "엔드");
    }

    private BiomeKoreanMapper() {
    }

    public static String toKorean(Biome biome) {
        return BIOME_MAP.getOrDefault(biome, prettify(biome.name()));
    }

    private static String prettify(String enumName) {
        String[] parts = enumName.toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }
}

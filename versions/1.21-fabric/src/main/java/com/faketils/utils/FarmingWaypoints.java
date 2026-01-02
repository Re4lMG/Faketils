package com.faketils.utils;

import com.faketils.Faketils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.util.math.BlockPos;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class FarmingWaypoints {

    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, List<Map<String, Integer>>>>() {}.getType();

    public static final Map<String, List<BlockPos>> WAYPOINTS = new HashMap<>();

    private static File getFile() {
        return new File(Faketils.configDirectory, "farming.json");
    }

    public static void load() {
        File file = getFile();
        if (file.exists()) {
            try {
                String content = Files.readString(file.toPath());
                Map<String, List<Map<String, Integer>>> data = GSON.fromJson(content, MAP_TYPE);
                WAYPOINTS.clear();
                if (data != null) {
                    data.forEach((key, list) -> {
                        List<BlockPos> positions = new ArrayList<>();
                        for (Map<String, Integer> posMap : list) {
                            int x = posMap.get("x");
                            int y = posMap.get("y");
                            int z = posMap.get("z");
                            positions.add(new BlockPos(x, y, z));
                        }
                        WAYPOINTS.put(key, positions);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void save() {
        Map<String, List<Map<String, Integer>>> data = new HashMap<>();
        WAYPOINTS.forEach((key, list) -> {
            List<Map<String, Integer>> serialized = new ArrayList<>();
            for (BlockPos pos : list) {
                Map<String, Integer> posMap = new HashMap<>();
                posMap.put("x", pos.getX());
                posMap.put("y", pos.getY());
                posMap.put("z", pos.getZ());
                serialized.add(posMap);
            }
            data.put(key, serialized);
        });

        File file = getFile();
        try {
            Files.writeString(file.toPath(), GSON.toJson(data));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
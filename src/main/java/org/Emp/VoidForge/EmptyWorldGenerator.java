package org.Emp.VoidForge;

import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

import java.util.Random;

public class EmptyWorldGenerator extends ChunkGenerator {

    @Override
    public void generateNoise(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkData chunkData) {

        if (chunkX == 0 && chunkZ == 0) {

            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    if (x >= 1 && x <= 15 && z >= 1 && z <= 15) {
                        chunkData.setBlock(x, 64, z, Material.OBSIDIAN);
                    }
                }
            }


            chunkData.setBlock(8, 65, 8, Material.GLOWSTONE);
        }
    }

    @Override
    public boolean shouldGenerateNoise() {
        return true;
    }

    @Override
    public boolean shouldGenerateSurface() {
        return false;
    }

    @Override
    public boolean shouldGenerateBedrock() {
        return false;
    }

    @Override
    public boolean shouldGenerateCaves() {
        return false;
    }

    @Override
    public boolean shouldGenerateDecorations() {
        return false;
    }

    @Override
    public boolean shouldGenerateMobs() {
        return false;
    }

    @Override
    public boolean shouldGenerateStructures() {
        return false;
    }
}
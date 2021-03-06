package com.janfic.games.library.ecs.systems.world;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.github.czyzby.noise4j.map.Grid;
import com.github.czyzby.noise4j.map.generator.noise.NoiseGenerator;
import com.github.czyzby.noise4j.map.generator.util.Generators;
import com.janfic.games.library.ecs.Mapper;
import com.janfic.games.library.ecs.components.events.EventComponent;
import com.janfic.games.library.ecs.components.input.ClickableComponent;
import com.janfic.games.library.ecs.components.physics.BoundingBoxComponent;
import com.janfic.games.library.ecs.components.physics.PositionComponent;
import com.janfic.games.library.ecs.components.physics.RotationComponent;
import com.janfic.games.library.ecs.components.rendering.ModelInstanceComponent;
import com.janfic.games.library.ecs.components.rendering.RenderableProviderComponent;
import com.janfic.games.library.ecs.components.world.GenerateWorldComponent;
import com.janfic.games.library.ecs.components.world.TileComponent;
import com.janfic.games.library.ecs.components.world.WorldComponent;
import com.janfic.games.library.utils.ECSClickListener;
import com.janfic.games.library.utils.voxel.CubeVoxel;
import com.janfic.games.library.utils.voxel.VoxelChunk;
import com.janfic.games.library.utils.voxel.VoxelWorld;
import jdk.internal.icu.text.NormalizerBase;

import java.io.File;

public class WorldGenerationSystem extends EntitySystem {

    private ImmutableArray<Entity> entities;

    private static final Family worldGeneratorFamily = Family.all(GenerateWorldComponent.class).exclude(WorldComponent.class).get();

    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
        entities = engine.getEntitiesFor(worldGeneratorFamily);
    }

    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);

        for (Entity entity : entities) {
            Json json = new Json();
            GenerateWorldComponent generateWorldComponent = Mapper.generateWorldComponentMapper.get(entity);
            JsonValue v = json.fromJson(null, generateWorldComponent.generationSettings);

            int width = generateWorldComponent.width;
            int height = generateWorldComponent.height;
            int length = generateWorldComponent.length;

            NoiseGenerator noiseGenerator = new NoiseGenerator();
            Grid grid = new Grid(width, length);
            JsonValue stages = v.get("generation").get("stages");
            JsonValue stage = stages.child();
            while(stage.next() != null) {
                int radius = stage.getInt(0);
                float modifier = stage.getFloat(1);
                noiseStage(grid, noiseGenerator, radius , modifier);
                stage = stage.next();
            }

            WorldComponent worldComponent = new WorldComponent();
            worldComponent.centerX = width / 2;
            worldComponent.centerY = height / 2;
            worldComponent.centerZ = length / 2;

            RenderableProviderComponent renderableProviderComponent = new RenderableProviderComponent();
            VoxelWorld world = new VoxelWorld(Gdx.files.local("models/tileTextures/test"), (int) Math.ceil(width / (float)VoxelChunk.CHUNK_SIZE_X),(int) Math.ceil(height / (float)VoxelChunk.CHUNK_SIZE_Y),(int) Math.ceil(length / (float)VoxelChunk.CHUNK_SIZE_Z));
            worldComponent.world = world;
            renderableProviderComponent.renderableProvider = world;

            System.out.println("Making World");

            for (int x = 0; x < width; x++) {
                for (int z = 0; z < length; z++) {
                    int h = (int) (grid.get(x,z) * height);
                    for (int y = 0; y <= h; y++) {
                        world.set(x,y,z, new CubeVoxel(world.atlas, y == h && h >= height / 2 ? "grass" : y == h && h < height / 2 ? "sand" : "dirt"));
                        if(y == height / 2 - 1 && y == h) world.set(x,y,z, new CubeVoxel(world.atlas, "sand"));
                    }
                    for (h = h + 1; h < height / 2; h++) {
                        world.set(x,h,z, new CubeVoxel(world.atlas, "water"));
                    }
                }
            }
            System.out.println("Finished World");

            PositionComponent positionComponent = new PositionComponent();
            positionComponent.position = new Vector3(worldComponent.centerX, worldComponent.centerY, worldComponent.centerZ);

            entity.add(positionComponent);
            entity.add(worldComponent);
            entity.add(renderableProviderComponent);


        }
    }

    private static void noiseStage(Grid grid, NoiseGenerator noiseGenerator, int radius, float modifier) {
        noiseGenerator.setRadius(radius);
        noiseGenerator.setModifier(modifier);
        noiseGenerator.setSeed(Generators.rollSeed());
        noiseGenerator.generate(grid);
    }
}

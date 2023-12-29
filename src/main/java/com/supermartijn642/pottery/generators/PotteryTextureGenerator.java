package com.supermartijn642.pottery.generators;

import com.supermartijn642.core.generator.ResourceCache;
import com.supermartijn642.core.generator.ResourceGenerator;
import com.supermartijn642.core.generator.ResourceType;
import com.supermartijn642.core.util.Triple;
import com.supermartijn642.pottery.Pottery;
import com.supermartijn642.pottery.content.PotColor;
import com.supermartijn642.pottery.content.PotType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.DecoratedPotPatterns;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModFileInfo;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Created 27/11/2023 by SuperMartijn642
 */
public class PotteryTextureGenerator extends ResourceGenerator {

    private final List<Triple<ResourceLocation,String,PotColor>> hueShifts = new ArrayList<>();

    public PotteryTextureGenerator(ResourceCache cache){
        super(Pottery.MODID, cache);
    }

    @Override
    public void generate(){
        // Pot textures
        for(PotType type : PotType.values()){
            for(PotColor color : PotColor.values()){
                if(color == PotColor.BLANK)
                    continue;
                String from = type.getIdentifier() + "/" + type.getIdentifier(PotColor.BLANK);
                String to = type.getIdentifier() + "/" + type.getIdentifier(color);
                this.hueShifts.add(Triple.of(new ResourceLocation(Pottery.MODID, from), to, color));
                this.cache.trackToBeGeneratedResource(ResourceType.ASSET, this.modid, "textures", to, ".png");
                if(type != PotType.DEFAULT){
                    this.hueShifts.add(Triple.of(new ResourceLocation(Pottery.MODID, from + "_side"), to + "_side", color));
                    this.cache.trackToBeGeneratedResource(ResourceType.ASSET, this.modid, "textures", to + "_side", ".png");
                    this.hueShifts.add(Triple.of(new ResourceLocation(Pottery.MODID, from + "_side_decorated"), to + "_side_decorated", color));
                    this.cache.trackToBeGeneratedResource(ResourceType.ASSET, this.modid, "textures", to + "_side_decorated", ".png");
                }
            }
        }

        // Pattern textures
        for(PotColor color : PotColor.values()){
            if(color == PotColor.BLANK)
                continue;
            BuiltInRegistries.DECORATED_POT_PATTERNS.registryKeySet().stream()
                .filter(key -> key.location().getNamespace().equals("minecraft"))
                .map(DecoratedPotPatterns::location)
                .forEach(texture -> {
                    String to = "patterns/" + color.getIdentifier() + "/" + texture.getPath().substring(texture.getPath().lastIndexOf('/') + 1);
                    this.hueShifts.add(Triple.of(texture, to, color));
                    this.cache.trackToBeGeneratedResource(ResourceType.ASSET, this.modid, "textures", to, ".png");
                });
        }
    }

    @Override
    public void save(){
        for(Triple<ResourceLocation,String,PotColor> entry : this.hueShifts){
            BufferedImage image = this.readImage(entry.left());
            hueShiftImage(image, entry.right());
            this.cache.saveResource(ResourceType.ASSET, writeImage(image), this.modid, "textures", entry.middle(), ".png");
        }
    }

    private BufferedImage readImage(ResourceLocation file){
        IModFileInfo modFile = ModList.get().getModFileById(file.getNamespace());
        if(modFile == null)
            throw new RuntimeException("Could not find mod for namespace '" + file.getNamespace() + "' for texture '" + file + "'!");
        Path filePath = modFile.getFile().findResource("assets", file.getNamespace(), "textures", file.getPath() + ".png");
        if(filePath == null)
            throw new RuntimeException("Could not find file 'assets/" + file.getNamespace() + "/textures/" + file.getPath() + ".png' from mod '" + file.getNamespace() + "'!");
        try(InputStream stream = Files.newInputStream(filePath)){
            BufferedImage image = ImageIO.read(stream);
            // Make sure we have an image with the full RGB color model
            BufferedImage redrawn = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
            redrawn.getGraphics().drawImage(image, 0, 0, null);
            return redrawn;
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    private static void hueShiftImage(BufferedImage image, PotColor color){
        float[] hsb = new float[3];
        ColorModel colorModel = ColorModel.getRGBdefault();
        for(int x = 0; x < image.getWidth(); x++){
            for(int y = 0; y < image.getHeight(); y++){
                int rgb = image.getRGB(x, y);
                if(colorModel.getAlpha(rgb) == 0)
                    continue;
                Color.RGBtoHSB(colorModel.getRed(rgb), colorModel.getGreen(rgb), colorModel.getBlue(rgb), hsb);
                hsb[0] += color.getHueShift() / 360f;
                hsb[0] %= 1;
                hsb[1] *= color.getSaturationShift() / 100f;
                hsb[1] = Math.max(0, Math.min(1, hsb[1]));
                hsb[2] *= 1 + color.getBrightnessShift() / 100f;
                hsb[2] = Math.max(0, Math.min(1, hsb[2]));
                rgb = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
                image.setRGB(x, y, rgb);
            }
        }
    }

    private static byte[] writeImage(BufferedImage image){
        try(ByteArrayOutputStream stream = new ByteArrayOutputStream()){
            ImageIO.write(image, "png", stream);
            return stream.toByteArray();
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getName(){
        return this.modName + " Texture Generator";
    }
}

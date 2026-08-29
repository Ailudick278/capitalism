package com.ailudick.capitalismmod.company;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * A player's conglomerate (group). Auto-created on first join; all their companies belong to it.
 *
 * @param name      conglomerate name (defaults to the player's name)
 * @param companies company name -> company
 */
public record Conglomerate(String name, Map<String, Company> companies) {

    public static final Codec<Conglomerate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(Conglomerate::name),
            Codec.unboundedMap(Codec.STRING, Company.CODEC).fieldOf("companies").forGetter(Conglomerate::companies)
    ).apply(instance, Conglomerate::new));

    public static Conglomerate create(String name) {
        return new Conglomerate(name, new HashMap<>());
    }
}

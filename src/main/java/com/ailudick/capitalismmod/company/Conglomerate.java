package com.ailudick.capitalismmod.company;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * A player's conglomerate (group). Created when the player first registers a company.
 *
 * @param name      conglomerate name (defaults to the player's name)
 * @param companies company name -> independent company ID
 */
public record Conglomerate(String name, Map<String, String> companies) {

    public static final Codec<Conglomerate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(Conglomerate::name),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("companies").forGetter(Conglomerate::companies)
    ).apply(instance, Conglomerate::new));

    public static Conglomerate create(String name) {
        return new Conglomerate(name, new HashMap<>());
    }
}
